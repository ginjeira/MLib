package app.spotygram

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KnowledgeCacheTest {
    private fun tempFile(): File =
        File.createTempFile("knowledge-cache", ".json").also { it.deleteOnExit() }

    private val info =
        KnowledgeInfo(
            summary = "Discovery is the second studio album by Daft Punk.",
            sourceNames = "MusicBrainz · Wikipédia",
            sourceUrl = "https://en.wikipedia.org/wiki/Discovery",
            releaseDate = "2001-03-12",
            kind = "Album",
            genres = "French house, disco",
            fetchedAt = 1_600_000_000_000,
        )

    @Test
    fun storesAndReadsEntries() {
        val cache = KnowledgeCache(tempFile())
        cache.put("daft punk|discovery", info)
        val restored = cache.get("daft punk|discovery")
        assertEquals(info, restored)
    }

    @Test
    fun expiredEntriesAreIgnored() {
        val cache = KnowledgeCache(tempFile(), ttlMillis = 1_000)
        cache.put("daft punk|discovery", info, now = 10_000)
        assertNull(cache.get("daft punk|discovery", now = 12_000))
        assertEquals(info, cache.get("daft punk|discovery", now = 10_500))
    }

    @Test
    fun oldestEntriesAreEvicted() {
        val cache = KnowledgeCache(tempFile(), maxEntries = 2)
        cache.put("a|x", info, now = 1)
        cache.put("b|y", info, now = 2)
        cache.put("c|z", info, now = 3)
        assertNull(cache.get("a|x", now = 3))
        assertEquals(info, cache.get("b|y", now = 3))
        assertEquals(info, cache.get("c|z", now = 3))
    }

    @Test
    fun corruptCacheFileIsTreatedAsEmpty() {
        val file = tempFile()
        file.writeText("{not json")
        val cache = KnowledgeCache(file)
        assertNull(cache.get("daft punk|discovery"))
        cache.put("daft punk|discovery", info)
        assertEquals(info, cache.get("daft punk|discovery"))
    }

    @Test
    fun missingFileIsANoOp() {
        val cache = KnowledgeCache(null)
        cache.put("daft punk|discovery", info)
        assertNull(cache.get("daft punk|discovery"))
    }

    @Test
    fun emptyEntriesAreNotStored() {
        val file = tempFile()
        val cache = KnowledgeCache(file)
        cache.put("a|x", KnowledgeInfo(summary = "", sourceNames = "", sourceUrl = ""))
        assertNull(cache.get("a|x"))
    }
}
