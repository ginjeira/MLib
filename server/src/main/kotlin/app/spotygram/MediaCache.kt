package app.spotygram

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shared temporary cache on top of TDLib's file store. One download per file,
 * many readers; idle entries are removed and TDLib deletes the local copy.
 */
class MediaCache(private val app: SpotygramApp, private val config: ServerConfig) {
    class Entry(
        val trackId: String,
        val fileId: Int,
        val path: File,
        val size: Long,
    ) {
        @Volatile var readers: Int = 0
        @Volatile var lastUsed: Long = System.currentTimeMillis()
    }

    private val entries = ConcurrentHashMap<String, Entry>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        scope.launch {
            while (true) {
                delay(60_000)
                sweep()
            }
        }
    }

    suspend fun acquire(trackId: String): Entry? {
        val track = app.library.state.value.byId[trackId] ?: return null
        val lock = locks.computeIfAbsent(trackId) { Mutex() }
        return lock.withLock {
            val existing = entries[trackId]
            if (existing != null && existing.path.isFile) {
                existing.readers++
                existing.lastUsed = System.currentTimeMillis()
                return@withLock existing
            }
            val path = ensureDownloaded(track) ?: return@withLock null
            val entry = Entry(trackId, track.fileId, path, path.length())
            entry.readers = 1
            entries[trackId] = entry
            enforceLimit()
            entry
        }
    }

    fun release(entry: Entry) {
        entry.readers = (entry.readers - 1).coerceAtLeast(0)
        entry.lastUsed = System.currentTimeMillis()
    }

    fun stats(): String {
        val count = entries.size
        val bytes = entries.values.sumOf { it.size }
        val readers = entries.values.sumOf { it.readers }
        return "{\"entries\":$count,\"bytes\":$bytes,\"readers\":$readers}"
    }

    private suspend fun ensureDownloaded(track: Track): File? {
        if (track.path.isNotBlank()) {
            val local = File(track.path)
            if (local.isFile && local.length() > 0) return local
        }
        val existing = app.telegram.files[track.fileId]
        if (existing?.complete == true && existing.path.isNotBlank()) {
            val file = File(existing.path)
            if (file.isFile && file.length() > 0) return file
        }
        if (track.fileId == 0) return null
        runCatching { app.telegram.download(track.fileId, priority = 32) }
        val deadline = System.currentTimeMillis() + 120_000
        while (System.currentTimeMillis() < deadline) {
            val state = app.telegram.files[track.fileId]
            if (state?.complete == true && state.path.isNotBlank()) {
                val file = File(state.path)
                if (file.isFile && file.length() > 0) return file
            }

            delay(200)
        }
        return null
    }

    private fun sweep() {
        val cutoff = System.currentTimeMillis() - config.cacheTtlMillis
        for ((id, entry) in entries) {
            if (entry.readers == 0 && entry.lastUsed < cutoff) {
                entries.remove(id)
                runCatching {
                    scope.launch { evict(entry.fileId) }
                }
            }
        }
        enforceLimit()
    }

    private suspend fun evict(fileId: Int) {
        runCatching {
            app.telegram.request(
                json("deleteFile", "file_id" to fileId),
                timeout = 10_000,
            )
        }
    }

    private fun enforceLimit() {
        var total = entries.values.sumOf { it.size }
        if (total <= config.cacheMaxBytes) return
        val idle =
            entries.values
                .filter { it.readers == 0 }
                .sortedBy { it.lastUsed }
        for (entry in idle) {
            if (total <= config.cacheMaxBytes) break
            entries.remove(entry.trackId)
            total -= entry.size
            scope.launch { evict(entry.fileId) }
        }
    }
}
