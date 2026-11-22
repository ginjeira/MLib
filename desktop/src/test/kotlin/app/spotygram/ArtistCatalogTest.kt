package app.spotygram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistCatalogTest {
    private fun track(
        id: String,
        artist: String,
        title: String = "Track $id",
        source: String = "GinjMusic",
        inLibrary: Boolean = true,
        available: Boolean = true,
    ) = Track(
        id = id,
        chatId = 1,
        messageId = id.hashCode().toLong(),
        fileId = 0,
        title = title,
        artist = artist,
        duration = 200,
        size = 1_000,
        source = source,
        date = 0,
        available = available,
        inLibrary = inLibrary,
    )

    private fun album(id: String, artist: String, title: String, tracks: List<String>) =
        Album(
            id = id,
            title = title,
            artist = artist,
            tracks = tracks,
            createdAt = 1,
            updatedAt = 1,
        )

    @Test
    fun namesAreGroupedIgnoringCaseAndSpacing() {
        val state =
            LibraryState(
                tracks =
                    listOf(
                        track("1", "Daft Punk"),
                        track("2", " daft   punk "),
                        track("3", "AIR"),
                    )
            )
        val entries = ArtistCatalog.entries(state)
        assertEquals(2, entries.size)
        assertEquals("AIR", entries.first().name)
        assertEquals(listOf("1", "2"), entries.last().tracks.map { it.id })
    }

    @Test
    fun displayNameUsesTheMostCommonSpelling() {
        val state =
            LibraryState(
                tracks =
                    listOf(
                        track("1", "DAFT PUNK"),
                        track("2", "Daft Punk"),
                        track("3", "Daft Punk"),
                    )
            )
        assertEquals("Daft Punk", ArtistCatalog.entries(state).single().name)
    }

    @Test
    fun unknownArtistsGoLast() {
        val state =
            LibraryState(
                tracks =
                    listOf(
                        track("1", ""),
                        track("2", "Zeta"),
                    )
            )
        val entries = ArtistCatalog.entries(state)
        assertTrue(entries.last().unknown)
        assertEquals("Zeta", entries.first().name)
    }

    @Test
    fun albumsAreThoseThatContainTheArtistTracks() {
        val state =
            LibraryState(
                tracks = listOf(track("1", "Daft Punk"), track("2", "Daft Punk"), track("3", "AIR")),
                albums =
                    listOf(
                        album("a", "Daft Punk", "Discovery", listOf("1", "2")),
                        album("b", "AIR", "Moon Safari", listOf("3")),
                    ),
            )
        val daftPunk = ArtistCatalog.profile("daft punk", state)
        assertNotNull(daftPunk)
        assertEquals(listOf("a"), daftPunk!!.albums.map { it.id })
    }

    @Test
    fun sourcesAreDistinctAndSorted() {
        val state =
            LibraryState(
                tracks =
                    listOf(
                        track("1", "AIR", source = "Archive"),
                        track("2", "AIR", source = "GinjMusic"),
                        track("3", "AIR", source = "Archive"),
                    )
            )
        assertEquals(listOf("Archive", "GinjMusic"), ArtistCatalog.entries(state).single().sources)
    }

    @Test
    fun tracksOutsideTheLibraryAreIgnored() {
        val state =
            LibraryState(tracks = listOf(track("1", "AIR"), track("2", "AIR", inLibrary = false)))
        assertEquals(listOf("1"), ArtistCatalog.entries(state).single().tracks.map { it.id })
    }

    @Test
    fun playableExcludesUnavailableRemoteTracks() {
        val state =
            LibraryState(
                tracks =
                    listOf(
                        track("1", "AIR", available = true),
                        track("2", "AIR", available = false),
                    )
            )
        assertEquals(listOf("1"), ArtistCatalog.entries(state).single().playable.map { it.id })
    }

    @Test
    fun profileLookupNormalizesTheName() {
        val state = LibraryState(tracks = listOf(track("1", "Daft Punk")))
        assertNotNull(ArtistCatalog.profile("  DAFT   punk ", state))
        assertNull(ArtistCatalog.profile("Nothing", state))
    }

    @Test
    fun normalizeCollapsesWhitespaceAndCase() {
        assertEquals("daft punk", ArtistCatalog.normalize("  Daft   PUNK "))
        assertEquals("", ArtistCatalog.normalize("   "))
    }
}
