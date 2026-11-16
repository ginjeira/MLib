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
            listOf(AcquisitionOption(1, "One More Time"), AcquisitionOption(2, "Around the World"), AcquisitionOption(2, "Duplicate")),
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
    fun duplicateMatchingUsesMCatMetadataRulesNotTelegramFileIds() {
        assertFalse(
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
    fun reversedArtistAndTitleMetadataMatchesWithCompatibleDuration() {
        assertTrue(
            MusicAcquisitionRules.isSameRecording(
                track("a", title = "One More Time", artist = "Daft Punk"),
                track("b", title = "Daft Punk", artist = "One More Time", duration = 322),
            )
        )
    }

    @Test
    fun sameTitleWithDifferentKnownArtistsIsNotDuplicate() {
        assertFalse(MusicAcquisitionRules.isSameRecording(track("a"), track("b", artist = "Justice")))
    }

    @Test
    fun sameTitleWithMissingArtistMatchesAsInMCat() {
        assertTrue(
            MusicAcquisitionRules.isSameRecording(
                track("a", artist = "", duration = 0, size = 0),
                track("b", artist = "", duration = 0, size = 0),
            )
        )
    }

    @Test
    fun missingArtistMatchesWithoutDurationCheckAsInMCat() {
        assertTrue(
            MusicAcquisitionRules.isSameRecording(
                track("a", artist = "", duration = 320),
                track("b", artist = "Daft Punk", duration = 321),
            )
        )
    }

    @Test
    fun parsesArtistAndAlbumTitleFromBotCollectionResult() {
        assertEquals(
            ParsedMusicName("Daft Punk", "Discovery (Deluxe Edition)"),
            MusicAcquisitionRules.parseArtistAndTitle("Daft Punk - Discovery (Deluxe Edition) (14 tracks)"),
        )
        assertEquals(
            ParsedMusicName("Massive Attack", "Mezzanine"),
            MusicAcquisitionRules.parseArtistAndTitle("Mezzanine by Massive Attack"),
        )
    }

    @Test
    fun collectionTitleRemovesTrackCountButPreservesEdition() {
        assertEquals("Greatest Hits (Deluxe Edition)", MusicAcquisitionRules.cleanCollectionTitle("Greatest Hits (Deluxe Edition) (18 🎵)"))
        assertEquals("Album (2024)", MusicAcquisitionRules.cleanCollectionTitle("Album (2024)"))
        assertEquals("My Playlist", MusicAcquisitionRules.cleanCollectionTitle("01. My Playlist (18 🎵) 🎧"))
    }

    @Test
    fun pendingCandidatesFindEquivalentTrackWithDifferentTelegramFileKey() {
        val candidate = track("candidate", title = "One More Tiem", fileKey = "file-a")
        val pending = listOf(track("pending", title = "One More Time", duration = 321, fileKey = "file-b"))
        assertEquals(-1, MusicAcquisitionRules.findEquivalentCandidate(candidate, pending))
    }

    @Test
    fun pendingCandidatesUseMCatSameTitleMatching() {
        val candidate = track("candidate", title = "One More Tiem", artist = "", duration = 0, size = 0)
        val pending = listOf(track("pending", title = "One More Time", artist = "", duration = 0, size = 0))
        assertEquals(0, MusicAcquisitionRules.findEquivalentCandidate(candidate, pending))
    }

    @Test
    fun smallTitleTypoDoesNotMatchWithoutExactMCatTitleRules() {
        assertFalse(
            MusicAcquisitionRules.isSameRecording(
                track("a", title = "One More Tiem"),
                track("b", title = "One More Time", duration = 321),
            )
        )
    }

    @Test
    fun adjacentTitleTranspositionDoesNotMatchMCatExactTitleRules() {
        assertFalse(
            MusicAcquisitionRules.isSameRecording(
                track("a", title = "One More Tiem"),
                track("b", title = "One More Time", duration = 321),
            )
        )
    }

    @Test
    fun fuzzyTitleDoesNotMatchWhenArtistDiffers() {
        assertFalse(
            MusicAcquisitionRules.isSameRecording(
                track("a", title = "One More Tiem"),
                track("b", title = "One More Time", artist = "Justice", duration = 321),
            )
        )
    }

    @Test
    fun fuzzyTitleDoesNotMatchWhenDurationDiffers() {
        assertFalse(
            MusicAcquisitionRules.isSameRecording(
                track("a", title = "One More Tiem", duration = 320),
                track("b", title = "One More Time", duration = 330),
            )
        )
    }

    @Test
    fun matchingArtistUsesMCatMatchingWithoutDurationConstraint() {
        assertTrue(MusicAcquisitionRules.isSameRecording(track("a"), track("b", duration = 400)))
    }
}
