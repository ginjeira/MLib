package app.spotygram

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.File
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/** Minimal Wave 1 HTTP surface: health, track index and ranged streaming. */
class HttpApi(
    private val app: SpotygramApp,
    private val cache: MediaCache,
    private val config: ServerConfig,
) {
    private val server =
        HttpServer.create(InetSocketAddress(config.host, config.port), 64).apply {
            executor = Executors.newFixedThreadPool(8) { task ->
                Thread(task, "MLib http").apply { isDaemon = true }
            }
            createContext("/healthz") { exchange -> handle(exchange) { health(exchange) } }
            createContext("/api/version") { exchange -> handle(exchange) { version(exchange) } }
            createContext("/api/tracks") { exchange -> handle(exchange) { tracks(exchange) } }
            createContext("/api/stream") { exchange -> handle(exchange) { stream(exchange) } }
        }

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop(0)
    }

    private fun handle(exchange: HttpExchange, block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            runCatching {
                send(exchange, 500, "application/json", JSONObject()
                    .put("error", error.message ?: error::class.java.simpleName)
                    .toString().toByteArray())
            }
            System.err.println("MLib server request failed: ${error.message}")
        } finally {
            exchange.close()
        }
    }

    private fun health(exchange: HttpExchange) {
        val state = app.library.state.value
        val auth = runCatching { app.telegram.auth.value.type }.getOrDefault("not_started")
        val connection = runCatching { app.telegram.connection.value.toString() }.getOrDefault("")
        val body =
            JSONObject()
                .put("status", if (state.tracks.isEmpty()) "empty" else "ok")
                .put("version", BuildConfig.VERSION_NAME)
                .put("telegram", auth)
                .put("connection", connection)
                .put("tracks", state.tracks.size)
                .put("sources", state.sources.size)
                .put("cache", JSONObject(cache.stats()))
                .put("time", System.currentTimeMillis())
        send(exchange, 200, "application/json", body.toString().toByteArray())
    }

    private fun version(exchange: HttpExchange) {
        send(
            exchange,
            200,
            "application/json",
            JSONObject().put("version", BuildConfig.VERSION_NAME).put("format", 1).toString().toByteArray(),
        )
    }

    private fun tracks(exchange: HttpExchange) {
        if (!authorized(exchange)) return unauthorized(exchange)
        val limit = exchange.requestURI.query
            ?.split('&')
            ?.firstOrNull { it.startsWith("limit=") }
            ?.substringAfter('=')
            ?.toIntOrNull()
            ?.coerceIn(1, 500) ?: 100
        val array = JSONArray()
        app.library.state.value.tracks
            .asSequence()
            .filter { it.fileId != 0 || it.path.isNotBlank() }
            .take(limit)
            .forEach { track ->
                array.put(
                    JSONObject()
                        .put("id", track.id)
                        .put("title", track.title)
                        .put("artist", track.artist)
                        .put("duration", track.duration)
                        .put("size", track.size)
                        .put("liked", track.liked)
                        .put("local", track.local)
                        .put("source", track.source),
                )
            }
        send(
            exchange,
            200,
            "application/json",
            JSONObject().put("tracks", array).put("total", app.library.state.value.tracks.size)
                .toString().toByteArray(),
        )
    }

    private fun stream(exchange: HttpExchange) {
        if (!authorized(exchange)) return unauthorized(exchange)
        if (exchange.requestMethod != "GET" && exchange.requestMethod != "HEAD") {
            send(exchange, 405, "text/plain", "Method not allowed".toByteArray())
            return
        }
        val trackId = exchange.requestURI.path.removePrefix("/api/stream/").trim('/')
        if (trackId.isBlank()) {
            send(exchange, 400, "text/plain", "Missing track id".toByteArray())
            return
        }
        val auth = runCatching { app.telegram.auth.value.type }.getOrDefault("")
        if (auth != "authorizationStateReady") {
            send(exchange, 503, "text/plain", "Telegram is not ready".toByteArray())
            return
        }
        val entry = runBlocking { cache.acquire(trackId) }
        if (entry == null) {
            send(exchange, 404, "text/plain", "Track not available".toByteArray())
            return
        }
        try {
            serveFile(exchange, entry.path, entry.size)
        } finally {
            cache.release(entry)
        }
    }

    private fun serveFile(exchange: HttpExchange, file: File, size: Long) {
        val type =
            when (file.extension.lowercase()) {
                "flac" -> "audio/flac"
                "m4a", "mp4", "aac" -> "audio/mp4"
                "ogg", "opus" -> "audio/ogg"
                "wav" -> "audio/wav"
                else -> "audio/mpeg"
            }
        val range = ByteRanges.parse(exchange.requestHeaders.getFirst("Range"), size)
        if (range is ByteRanges.Result.None) {
            exchange.responseHeaders.apply {
                add("Content-Type", type)
                add("Accept-Ranges", "bytes")
                add("Content-Length", size.toString())
                add("Cache-Control", "private, max-age=3600")
            }
            exchange.sendResponseHeaders(200, if (exchange.requestMethod == "HEAD") -1 else size)
            if (exchange.requestMethod != "HEAD") {
                FileInputStream(file).use { input -> exchange.responseBody.use { input.copyTo(it) } }
            }
            return
        }
        if (range is ByteRanges.Result.Invalid) {
            exchange.responseHeaders.add("Content-Range", "bytes */$size")
            send(exchange, 416, "text/plain", "Range not satisfiable".toByteArray())
            return
        }
        val valid = range as ByteRanges.Result.Valid
        val start = valid.start
        val end = valid.end
        val length = valid.length
        exchange.responseHeaders.apply {
            add("Content-Type", type)
            add("Accept-Ranges", "bytes")
            add("Content-Range", "bytes $start-$end/$size")
            add("Content-Length", length.toString())
            add("Cache-Control", "private, max-age=3600")
        }
        exchange.sendResponseHeaders(206, if (exchange.requestMethod == "HEAD") -1 else length)
        if (exchange.requestMethod == "HEAD") return
        FileInputStream(file).use { input ->
            input.channel.position(start)
            exchange.responseBody.use { output ->
                val buffer = ByteArray(64 * 1024)
                var remaining = length
                while (remaining > 0) {
                    val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    remaining -= read
                }
            }
        }
    }

    private fun authorized(exchange: HttpExchange): Boolean {
        val token = config.streamToken ?: return true
        val header = exchange.requestHeaders.getFirst("Authorization").orEmpty()
        val query =
            exchange.requestURI.query
                ?.split('&')
                ?.firstOrNull { it.startsWith("token=") }
                ?.substringAfter('=')
                .orEmpty()
        val presented = if (header.startsWith("Bearer ")) header.removePrefix("Bearer ").trim() else query
        return presented.isNotEmpty() &&
            MessageDigest.isEqual(presented.toByteArray(), token.toByteArray())
    }

    private fun unauthorized(exchange: HttpExchange) {
        send(
            exchange,
            401,
            "application/json",
            JSONObject().put("error", "unauthorized").toString().toByteArray(),
        )
    }

    private fun send(exchange: HttpExchange, code: Int, type: String, body: ByteArray) {
        exchange.responseHeaders.apply {
            add("Content-Type", "$type; charset=utf-8")
            add("X-Content-Type-Options", "nosniff")
        }
        exchange.sendResponseHeaders(code, body.size.toLong())
        exchange.responseBody.use { it.write(body) }
    }
}
