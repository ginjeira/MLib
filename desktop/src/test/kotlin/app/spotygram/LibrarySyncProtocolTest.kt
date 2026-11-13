package app.spotygram

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySyncProtocolTest {
    private val writerA = "00000000-0000-0000-0000-000000000001"
    private val writerB = "00000000-0000-0000-0000-000000000002"

    private fun row(key: String, clock: Long, writer: String, value: Any) =
        JSONArray().put(key).put(clock).put(writer).put(value)

    @Test
    fun decodeCanonicalizesObjectFieldOrder() {
        val first = LibrarySync.decode(
            JSONArray().put(
                row(
                    "source/123",
                    1,
                    writerA,
                    JSONObject().put("enabled", true).put("title", "GinjMusic"),
                )
            )
        ).single()
        val second = LibrarySync.decode(
            JSONArray().put(
                row(
                    "source/123",
                    1,
                    writerA,
                    JSONObject().put("title", "GinjMusic").put("enabled", true),
                )
            )
        ).single()

        assertEquals(first.value, second.value)
    }

    @Test
    fun languageValidationMatchesSupportedLocales() {
        val portuguese = JSONArray().put(row("setting/language", 1, writerA, "pt"))
        val english = JSONArray().put(row("setting/language", 2, writerB, "en"))
        val unsupported = JSONArray().put(row("setting/language", 3, writerA, "ru"))

        assertEquals("pt", JSONObject(LibrarySync.decode(portuguese).single().value).getString("v"))
        assertEquals("en", JSONObject(LibrarySync.decode(english).single().value).getString("v"))
        assertTrue(runCatching { LibrarySync.decode(unsupported) }.isFailure)
    }

    @Test
    fun sourceAndProfileMetadataAcceptSupportedDescriptors() {
        val packet = JSONArray()
            .put(row("source/-100123", 1, writerA, JSONObject().put("enabled", true).put("title", "Music")))
            .put(row("profile/12345", 2, writerB, JSONObject().put("enabled", false).put("title", "Archive").put("username", "archive")))

        assertEquals(2, LibrarySync.decode(packet).size)
    }

    @Test
    fun decodeRejectsDuplicateKeys() {
        val packet = JSONArray()
            .put(row("setting/theme", 1, writerA, "dark"))
            .put(row("setting/theme", 2, writerB, "light"))

        assertTrue(runCatching { LibrarySync.decode(packet) }.isFailure)
    }

    @Test
    fun decodeRejectsUnsupportedNamespaces() {
        val packet = JSONArray().put(row("telegram/session", 1, writerA, "secret"))

        assertTrue(runCatching { LibrarySync.decode(packet) }.isFailure)
    }

    @Test
    fun decodeRejectsInvalidWriterIdentity() {
        val packet = JSONArray().put(row("setting/theme", 1, "not-a-device-id", "dark"))

        assertTrue(runCatching { LibrarySync.decode(packet) }.isFailure)
    }

    @Test
    fun legacyPlaybackStateIsIgnored() {
        val packet = JSONArray()
            .put(row("setting/speed", 1, writerA, 1.25))
            .put(row("queue", 2, writerA, JSONArray()))
            .put(row("progress/track", 3, writerA, JSONObject().put("position", 1200)))

        assertTrue(LibrarySync.decode(packet, legacy = true).isEmpty())
    }

    @Test
    fun lastWriterWinsUsesStableDeviceTieBreak() {
        val olderWriter = LibrarySync.Record(
            "setting/theme", 7, writerA, """{"v":"dark"}"""
        )
        val newerWriter = LibrarySync.Record(
            "setting/theme", 7, writerB, """{"v":"light"}"""
        )

        assertTrue(newerWriter.supersedes(olderWriter))
        assertFalse(olderWriter.supersedes(newerWriter))
    }

    @Test
    fun deletionTombstoneDominatesAnOlderOrNewerLiveValue() {
        val key = "playlist/00000000-0000-0000-0000-000000000099/deleted"
        val live = LibrarySync.Record(key, 900, writerB, """{"v":false}""")
        val deleted = LibrarySync.Record(key, 2, writerA, """{"v":true}""")

        assertTrue(deleted.removesIdentity())
        assertTrue(deleted.supersedes(live))
        assertFalse(live.supersedes(deleted))
    }

    @Test
    fun trackTombstoneIsRecognized() {
        val deleted = LibrarySync.Record(
            "track/local:00000000-0000-0000-0000-000000000099",
            1,
            writerA,
            """{"v":{"deleted":true}}""",
        )

        assertTrue(deleted.removesIdentity())
    }
}
