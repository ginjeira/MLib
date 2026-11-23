package app.spotygram

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

interface ServerTdJsonNative : Library {
    fun td_create_client_id(): Int
    fun td_send(client: Int, request: String)
    fun td_receive(timeout: Double): Pointer?
    fun td_execute(request: String): Pointer?
}

/**
 * Single receiver for the whole process. TDLib's td_receive is global: every
 * update must be routed by @client_id, never consumed by a per-profile loop.
 */
object TdRouter {
    private val queues = ConcurrentHashMap<Int, LinkedBlockingQueue<String>>()
    private val native: ServerTdJsonNative by lazy {
        val directory = ServerConfig().nativeDir?.let(::File)
        val explicit =
            directory
                ?.takeIf { it.isDirectory }
                ?.listFiles()
                ?.firstOrNull { it.name.contains("tdjson", ignoreCase = true) }
        if (explicit != null) {
            Native.load(explicit.absolutePath, ServerTdJsonNative::class.java)
        } else {
            Native.load("tdjson", ServerTdJsonNative::class.java)
        }
    }
    @Volatile private var receiver: Thread? = null

    private fun ensureReceiver() {
        if (receiver != null) return
        synchronized(this) {
            if (receiver != null) return
            val thread =
                Thread(
                    {
                        while (!Thread.currentThread().isInterrupted) {
                            val pointer =
                                try {
                                    native.td_receive(0.5)
                                } catch (_: Throwable) {
                                    null
                                } ?: continue
                            val text =
                                try {
                                    pointer.getString(0, "UTF-8")
                                } catch (_: Throwable) {
                                    null
                                } ?: continue
                            TdRouting.route(text) { client, payload ->
                                queues[client]?.offer(payload) == true
                            }
                        }
                    },
                    "MLib TDLib dispatcher",
                )
                    .apply { isDaemon = true }
            receiver = thread
            thread.start()
        }
    }

    fun createClientId(): Int {
        ensureReceiver()
        val id = native.td_create_client_id()
        queues[id] = LinkedBlockingQueue()
        return id
    }

    fun send(client: Int, request: String) {
        ensureReceiver()
        native.td_send(client, request)
    }

    fun receive(client: Int, timeoutSeconds: Double): String? {
        ensureReceiver()
        val queue = queues[client] ?: return null
        val millis = (timeoutSeconds * 1000).toLong().coerceAtLeast(1)
        return queue.poll(millis, TimeUnit.MILLISECONDS)
    }

    fun execute(request: String): String? {
        ensureReceiver()
        return runCatching { native.td_execute(request)?.getString(0, "UTF-8") }.getOrNull()
    }
}

class ServerTdClient : TdClient {
    @Volatile private var client = 0

    override fun createClientId(): Int = TdRouter.createClientId().also { client = it }
    override fun send(client: Int, request: String) = TdRouter.send(client, request)
    override fun receive(timeout: Double) =
        if (client == 0) null else TdRouter.receive(client, timeout)
    override fun execute(request: String) = TdRouter.execute(request)
}
