package app.spotygram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

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
        assertEquals("Album", MusicAcquisitionRules.cleanCollectionTitle("Album (2024)"))
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
        assertEquals(-1, MusicAcquisitionRules.findEquivalentCandidate(candidate, pending))
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

    @Test
    fun albumMatchingUsesMCatTitleCleanupAndCompatibleArtistRules() {
        assertTrue(
            MusicAcquisitionRules.areAlbumsDuplicate(
                "Daft Punk - Discovery (Deluxe Edition) (2001)",
                "Daft Punk",
                "Discovery",
                "Daft Punk",
            )
        )
        assertFalse(
            MusicAcquisitionRules.areAlbumsDuplicate(
                "Discovery",
                "Daft Punk",
                "Discovery",
                "Justice",
            )
        )
    }

    @Test
    fun wave0GoldenSearchQueryAndPlaylistNameCasesMatchMCat() {
        val queryCases = listOf(
            "[12 tracks] Daft Punk - One More Time.mp3" to "Daft Punk - One More Time",
            "01. Björk - Jóga [Official Audio].flac" to "Björk - Jóga",
            "Greatest Hits - Seleção (18 🎵)" to "Greatest Hits",
            "" to "",
        )
        queryCases.forEach { (input, expected) ->
            assertEquals(expected, MusicAcquisitionRules.sanitizeSearchQuery(input))
        }
        val playlistCases = listOf(
            "01. My Playlist (18 🎵) 🎧" to "My Playlist",
            "Greatest Hits (Deluxe Edition) (14 tracks)" to "Greatest Hits (Deluxe Edition)",
            "Road Trip 🎶" to "Road Trip",
        )
        playlistCases.forEach { (input, expected) ->
            assertEquals(expected, MusicAcquisitionRules.cleanCollectionTitle(input))
        }
    }

    @Test
    fun wave0GoldenTrackDuplicateCasesMatchMCatSource() {
        data class Case(val candidateTitle: String, val candidateArtist: String, val existingTitle: String, val existingArtist: String, val expected: Boolean)
        val cases = listOf(
            Case("Halo (Official Audio)", "Beyoncé", "Beyonce", "Halo", true),
            Case("One More Time", "Daft Punk", "One More Time", "Justice", false),
            Case("Around the World", "", "Around the World", "Daft Punk", true),
            Case("", "Daft Punk", "", "Daft Punk", false),
            Case("One More Time", "Daft Punk", "One More Time - Official Video", "Daft Punk", true),
        )
        cases.forEach { case ->
            assertEquals(
                case.expected,
                MusicAcquisitionRules.areTracksDuplicate(
                    case.candidateTitle, case.candidateArtist,
                    case.existingTitle, case.existingArtist,
                ),
            )
        }
    }

    @Test
    fun wave0GoldenAlbumDuplicateCasesMatchMCatSource() {
        data class Case(val firstTitle: String, val firstArtist: String, val secondTitle: String, val secondArtist: String, val expected: Boolean)
        val cases = listOf(
            Case("Discovery (Deluxe Edition)", "Daft Punk", "Daft Punk - Discovery", "Daft Punk", true),
            Case("Discovery", "Daft Punk", "Discovery", "Justice", false),
            Case("Mezzanine (1998)", "Massive Attack", "Mezzanine", "Massive Attack", true),
            Case("", "Unknown", "", "Unknown", false),
        )
        cases.forEach { case ->
            assertEquals(
                case.expected,
                MusicAcquisitionRules.areAlbumsDuplicate(
                    case.firstTitle, case.firstArtist,
                    case.secondTitle, case.secondArtist,
                ),
            )
        }
    }


    private fun readPortabilityFixture(name: String): JSONObject {
        val stream = javaClass.getResourceAsStream("/portability/$name")
            ?: error("Missing portability fixture: $name")
        return stream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
    }

    @Test
    fun wave0GoldenJsonFixtureMatchesKotlinPort() {
        val fixture = readPortabilityFixture("mcat-golden-v1.json")
        val queries = fixture.getJSONArray("searchQuery")
        for (index in 0 until queries.length()) {
            val item = queries.getJSONObject(index)
            assertEquals(
                "search query golden ${item.getString("id")}",
                item.getString("expected"),
                MusicAcquisitionRules.sanitizeSearchQuery(item.getString("input")),
            )
        }
        val playlists = fixture.getJSONArray("collectionName")
        for (index in 0 until playlists.length()) {
            val item = playlists.getJSONObject(index)
            assertEquals(
                "playlist golden ${item.getString("id")}",
                item.getString("expected"),
                MusicAcquisitionRules.cleanCollectionTitle(item.getString("input")),
            )
        }
        val tracks = fixture.getJSONArray("trackDuplicate")
        for (index in 0 until tracks.length()) {
            val item = tracks.getJSONObject(index)
            val candidate = item.getJSONObject("candidate")
            val existing = item.getJSONObject("existing")
            assertEquals(
                "track duplicate golden ${item.getString("id")}",
                item.getBoolean("expected"),
                MusicAcquisitionRules.areTracksDuplicate(
                    candidate.getString("title"), candidate.getString("artist"),
                    existing.getString("title"), existing.getString("artist"),
                ),
            )
        }
        val albums = fixture.getJSONArray("albumDuplicate")
        for (index in 0 until albums.length()) {
            val item = albums.getJSONObject(index)
            val first = item.getJSONObject("first")
            val second = item.getJSONObject("second")
            assertEquals(
                "album duplicate golden ${item.getString("id")}",
                item.getBoolean("expected"),
                MusicAcquisitionRules.areAlbumsDuplicate(
                    first.getString("title"), first.getString("artist"),
                    second.getString("title"), second.getString("artist"),
                ),
            )
        }
    }

    @Test
    fun wave0ProtocolFixturesCoverAllTypeProviderPairsAndParseResults() {
        val fixture = readPortabilityFixture("mcat-protocol-v1.json")
        val cases = fixture.getJSONArray("searchCases")
        assertEquals(8, cases.length())
        val seenPairs = mutableSetOf<String>()
        for (index in 0 until cases.length()) {
            val item = cases.getJSONObject(index)
            val type = item.getString("contentType")
            val provider = item.getString("provider")
            seenPairs += "$type/$provider"
            val message = item.getJSONObject("searchMessage")
            val options = MusicAcquisitionRules.parseResults(message.getString("text"))
            assertEquals(listOf(1, 2), options.map { it.number })
            val buttons = item.getJSONArray("resultButtons")
            assertEquals(options.size, buttons.length())
            for (buttonIndex in 0 until buttons.length()) {
                val button = buttons.getJSONObject(buttonIndex)
                assertTrue(button.getString("callbackData").startsWith("fixture:"))
            }
            assertTrue(item.getJSONObject("filterButton").getString("callbackData").startsWith("fixture:"))
        }
        assertEquals(8, seenPairs.size)
        val audio = fixture.getJSONArray("audioMessages")
        val expectedAudio = (0 until audio.length()).count { audio.getJSONObject(it).getBoolean("expectedCollected") }
        assertEquals(2, expectedAudio)
        assertTrue(fixture.getJSONArray("completionCases").length() >= 8)
        assertEquals(5, fixture.getJSONArray("cardinalityCases").length())
    }



    @Test
    fun wave1GoldenCompletionFixturesMatchMcatProtocol() {
        val fixture = readPortabilityFixture("mcat-protocol-v1.json")
        val cases = fixture.getJSONArray("completionCases")
        for (index in 0 until cases.length()) {
            val item = cases.getJSONObject(index)
            val contentType = AcquisitionContentType.valueOf(
                item.optString("contentType", "playlist").uppercase(),
            )
            val expectedCount = item.optInt("expectedCount", 0)
            val audioCount = item.getInt("receivedAudioCount")
            val signal = item.optString("signal").takeIf { it.isNotBlank() && it != "null" }
            val silenceMillis = item.optInt("silenceSeconds", 0) * 1_000L
            val elapsedMillis = item.optInt("deadlineSeconds", 0).let { seconds ->
                if (seconds > 0) seconds * 1_000L
                else if (item.getString("expected") == "fail_no_audio") 45_000L else 0L
            }
            val actual = AcquisitionCompletionRules.completionReason(
                contentType, expectedCount, audioCount,
                signal?.let { AcquisitionCompletionRules.isCompletionSignal(it) } == true,
                silenceMillis, elapsedMillis,
            )
            val expected = when (item.getString("expected")) {
                "complete_count" -> AcquisitionCompletionReason.EXPECTED_COUNT
                "complete_signal" -> AcquisitionCompletionReason.COMPLETION_SIGNAL
                "complete_single" -> AcquisitionCompletionReason.SINGLE_TRACK
                "complete_quiet" -> AcquisitionCompletionReason.QUIET_PERIOD
                "wait_for_signal" -> null
                "fail_no_audio" -> AcquisitionCompletionReason.DEADLINE_NO_AUDIO
                "partial_timeout" -> AcquisitionCompletionReason.DEADLINE_PARTIAL
                else -> error("Unknown completion fixture: ${item.getString("expected")}")
            }
            assertEquals("completion fixture ${item.getString("id")}", expected, actual)
        }
    }

    @Test
    fun artistCardinalityUsesExpectedCountInsteadOfContentTypeAlone() {
        assertEquals(1, AcquisitionCompletionRules.expectedTrackCount("Daft Punk", AcquisitionContentType.ARTIST))
        assertEquals(1, AcquisitionCompletionRules.expectedTrackCount("Daft Punk (1 track)", AcquisitionContentType.ARTIST))
        assertEquals(7, AcquisitionCompletionRules.expectedTrackCount("Daft Punk (7 tracks)", AcquisitionContentType.ARTIST))
        assertFalse(AcquisitionCompletionRules.isMultiTrack(AcquisitionContentType.ARTIST, 1))
        assertTrue(AcquisitionCompletionRules.isMultiTrack(AcquisitionContentType.ARTIST, 7))
        assertTrue(AcquisitionCompletionRules.isMultiTrack(AcquisitionContentType.PLAYLIST, 0))
        assertTrue(AcquisitionCompletionRules.isMultiTrack(AcquisitionContentType.ALBUM, 0))
        assertFalse(AcquisitionCompletionRules.isMultiTrack(AcquisitionContentType.TRACK, 1))
    }

    @Test
    fun completionSignalsMatchMCatTextAndRequireReceivedAudio() {
        listOf("Finished.", "Download complete", "COMPLETED",
            "Todas as faixas concluído", "Process complete", "Done!", "Pronto!").forEach {
            assertTrue("signal: $it", AcquisitionCompletionRules.isCompletionSignal(it))
        }
        assertFalse(AcquisitionCompletionRules.isCompletionSignal("finish"))
        assertEquals(null, AcquisitionCompletionRules.completionReason(
            AcquisitionContentType.PLAYLIST, 0, 0, true, 0, 10_000,
        ))
        assertEquals(AcquisitionCompletionReason.COMPLETION_SIGNAL,
            AcquisitionCompletionRules.completionReason(
                AcquisitionContentType.PLAYLIST, 0, 1, true, 0, 10_000,
            ))
    }

    @Test
    fun knownCountIgnoresQuietPeriodUntilTheCountArrives() {
        assertEquals(
            null,
            AcquisitionCompletionRules.completionReason(
                AcquisitionContentType.PLAYLIST, 27, 8, false, 12_000, 40_000,
            ),
        )
        assertEquals(
            AcquisitionCompletionReason.DEADLINE_PARTIAL,
            AcquisitionCompletionRules.completionReason(
                AcquisitionContentType.PLAYLIST, 27, 8, false, 12_000, 240_000,
            ),
        )
        assertEquals(
            AcquisitionCompletionReason.EXPECTED_COUNT,
            AcquisitionCompletionRules.completionReason(
                AcquisitionContentType.PLAYLIST, 27, 27, false, 1_000, 90_000,
            ),
        )
    }

    @Test
    fun unknownCountStillFinishesAfterSilence() {
        assertEquals(
            AcquisitionCompletionReason.QUIET_PERIOD,
            AcquisitionCompletionRules.completionReason(
                AcquisitionContentType.PLAYLIST, 0, 4, false, 9_000, 30_000,
            ),
        )
    }

    @Test
    fun partialDeadlineIsNotReportedAsFullCompletion() {
        assertEquals(AcquisitionCompletionReason.DEADLINE_PARTIAL,
            AcquisitionCompletionRules.completionReason(
                AcquisitionContentType.ALBUM, 12, 7, false, 1_000, 240_000,
            ))
    }


    @Test
    fun botReplacementMessageMustBeNewerThanCallbackCursor() {
        assertFalse(AcquisitionCompletionRules.isAfterMessageCursor(100L, 100L))
        assertFalse(AcquisitionCompletionRules.isAfterMessageCursor(99L, 100L))
        assertTrue(AcquisitionCompletionRules.isAfterMessageCursor(101L, 100L))
    }

    @Test
    fun expectedCountSingleTrackAndQuietPeriodMatchMCatCompletion() {
        assertEquals(AcquisitionCompletionReason.EXPECTED_COUNT,
            AcquisitionCompletionRules.completionReason(
                AcquisitionContentType.PLAYLIST, 20, 20, false, 9_000, 60_000,
            ))
        // MCat checks expected count before its single-track fallback.
        assertEquals(AcquisitionCompletionReason.EXPECTED_COUNT,
            AcquisitionCompletionRules.completionReason(
                AcquisitionContentType.TRACK, 1, 1, false, 0, 2_000,
            ))
        assertEquals(AcquisitionCompletionReason.QUIET_PERIOD,
            AcquisitionCompletionRules.completionReason(
                AcquisitionContentType.PLAYLIST, 0, 4, false, 9_000, 60_000,
            ))
        assertEquals(null, AcquisitionCompletionRules.completionReason(
            AcquisitionContentType.PLAYLIST, 0, 4, false, 8_999, 60_000,
        ))
    }

    @Test
    fun finishSignalCompletesGeographicallyShortCollection() {
        assertEquals(AcquisitionCompletionReason.COMPLETION_SIGNAL,
            AcquisitionCompletionRules.completionReason(
                AcquisitionContentType.PLAYLIST, 20, 18, true, 2_000, 60_000,
            ))
    }

    @Test
    fun knownCountWaitsForTheRemainingTracksInsteadOfTheQuietPeriod() {
        // The bot can pause while fetching the next tracks; a known count must
        // not end the collection early (this produced 1-of-27 reviews).
        assertEquals(null,
            AcquisitionCompletionRules.completionReason(
                AcquisitionContentType.PLAYLIST, 20, 18, false, 9_000, 30_000,
            ))
        assertEquals(AcquisitionCompletionReason.DEADLINE_PARTIAL,
            AcquisitionCompletionRules.completionReason(
                AcquisitionContentType.PLAYLIST, 20, 18, false, 8_000, 240_000,
            ))
    }
}
