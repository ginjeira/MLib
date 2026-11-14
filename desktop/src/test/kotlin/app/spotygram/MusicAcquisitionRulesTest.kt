package app.spotygram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicAcquisitionRulesTest {
    private fun track(
        id: String,
        title: String = "One More Time",
        artist: String = "Daft Punk",
        duration: Int = 320,
        size: Long = 5_000_000,
        fileKey: String = "",
    ) = Track(
        id = id,
        chatId = 1,
        messageId = id.hashCode().toLong(),
        fileId = 0,
        title = title,
        artist = artist,
        duration = duration,
        size = size,
        source = "GinjMusic",
        date = 0,
        fileKey = fileKey,
    )

    @Test
    fun parsesNumberedBotResultsAndIgnoresOtherLines() {
        val results = MusicAcquisitionRules.parseResults(
            "Searching for Daft Punk\n1. One More Time\n2. Around the World\nnot a result\n2. Duplicate"
        )
        assertEquals(
            listOf(AcquisitionOption(1, "One More Time"), AcquisitionOption(2, "Around the World")),
            results,
        )
    }

    @Test
    fun sanitizesNumberedAudioFilenameAndTrackCount() {
        assertEquals(
            "Daft Punk - One More Time",
            MusicAcquisitionRules.sanitizeSearchQuery("[12 tracks] Daft Punk - One More Time.mp3"),
        )
    }

    @Test
    fun sameTelegramFileIsAlwaysTheSameRecording() {
        assertTrue(
            MusicAcquisitionRules.isSameRecording(
                track("a", title = "Different title", artist = "", duration = 0, fileKey = "telegram-unique"),
                track("b", title = "Other title", artist = "Unknown", duration = 0, fileKey = "telegram-unique"),
            )
        )
    }

    @Test
    fun matchingTitleAndArtistWithCompatibleDurationIsDuplicate() {
        assertTrue(MusicAcquisitionRules.isSameRecording(track("a"), track("b", duration = 322)))
    }

    @Test
    fun sameTitleWithDifferentKnownArtistsIsNotDuplicate() {
        assertFalse(MusicAcquisitionRules.isSameRecording(track("a"), track("b", artist = "Justice")))
    }

    @Test
    fun sameTitleAloneWithMissingArtistAndDurationIsAmbiguous() {
        assertFalse(
            MusicAcquisitionRules.isSameRecording(
                track("a", artist = "", duration = 0, size = 0),
                track("b", artist = "", duration = 0, size = 0),
            )
        )
    }

    @Test
    fun missingArtistCanBeMatchedByCloseKnownDuration() {
        assertTrue(
            MusicAcquisitionRules.isSameRecording(
                track("a", artist = "", duration = 320),
                track("b", artist = "Daft Punk", duration = 321),
            )
        )
    }

    @Test
    fun matchingArtistDoesNotHideLargeDurationMismatch() {
        assertFalse(MusicAcquisitionRules.isSameRecording(track("a"), track("b", duration = 400)))
    }
}
