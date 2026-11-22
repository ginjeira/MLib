package app.spotygram

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoLibraryTest {
    private val labels =
        AutoLibrary.Labels(
            home = "Home",
            allTracks = "All tracks",
            playlists = "Playlists",
            albums = "Albums",
            artists = "Artists",
            favorites = "Favorites",
            offline = "Offline",
            recent = "Recent",
        )

    private fun track(
        id: String,
        title: String = "Track $id",
        artist: String = "Artist",
        liked: Boolean = false,
        path: String = "",
        available: Boolean = true,
        temporary: Boolean = false,
        date: Long = 0,
        art: String = "",
    ) = Track(
        id = id,
        chatId = 1,
        messageId = id.hashCode().toLong(),
        fileId = 0,
        title = title,
        artist = artist,
        duration = 200,
        size = 1_000,
        source = "GinjMusic",
        date = date,
        art = art,
        path = path,
        liked = liked,
        available = available,
        temporary = temporary,
    )

    private fun library(
        tracks: List<Track> = emptyList(),
        albums: List<Album> = emptyList(),
        playlists: List<Playlist> = emptyList(),
    ) = LibraryState(tracks = tracks, albums = albums, playlists = playlists)

    @Test
    fun rootOffersHomeAndCoreSections() {
        val ids = AutoLibrary.root(labels).map { it.id }
        assertEquals(
            listOf(
                AutoLibrary.HOME,
                AutoLibrary.PLAYLISTS,
                AutoLibrary.ALBUMS,
                AutoLibrary.ARTISTS,
                AutoLibrary.FAVORITES,
                AutoLibrary.OFFLINE,
                AutoLibrary.ALL_TRACKS,
            ),
            ids,
        )
        assertTrue(AutoLibrary.root(labels).none { it.playable })
    }

    @Test
    fun homeGroupsRecentFavoritesOfflineAndPlaylists() {
        val ids = AutoLibrary.children(AutoLibrary.HOME, library(), labels).map { it.id }
        assertEquals(
            listOf(
                AutoLibrary.RECENT,
                AutoLibrary.FAVORITES,
                AutoLibrary.OFFLINE,
                AutoLibrary.PLAYLISTS,
            ),
            ids,
        )
    }

    @Test
    fun playableFiltersOutUnavailableRemoteTracks() {
        val state =
            library(
                tracks =
                    listOf(
                        track("1", available = true),
                        track("2", available = false),
                        track("3", available = false, path = "/music/3.mp3"),
                    )
            )
        val ids = AutoLibrary.children(AutoLibrary.ALL_TRACKS, state, labels).map { it.id }
        assertEquals(listOf("track:1", "track:3"), ids)
    }

    @Test
    fun favoritesOnlyIncludeLikedPlayableTracks() {
        val state =
            library(
                tracks =
                    listOf(
                        track("1", liked = true),
                        track("2", liked = false),
                        track("3", liked = true, available = false),
                    )
            )
        val ids = AutoLibrary.children(AutoLibrary.FAVORITES, state, labels).map { it.id }
        assertEquals(listOf("track:1"), ids)
    }

    @Test
    fun offlineExcludesTemporaryFiles() {
        val state =
            library(
                tracks =
                    listOf(
                        track("1", path = "/music/1.mp3"),
                        track("2", path = "/cache/2.mp3", temporary = true),
                        track("3"),
                    )
            )
        val ids = AutoLibrary.children(AutoLibrary.OFFLINE, state, labels).map { it.id }
        assertEquals(listOf("track:1"), ids)
    }

    @Test
    fun recentIsNewestFirstAndCapped() {
        val tracks =
            (1..AutoLibrary.RECENT_LIMIT + 5).map { i ->
                track(i.toString(), date = i.toLong())
            }
        val ids = AutoLibrary.children(AutoLibrary.RECENT, library(tracks), labels).map { it.id }
        assertEquals(AutoLibrary.RECENT_LIMIT, ids.size)
        assertEquals("track:${AutoLibrary.RECENT_LIMIT + 5}", ids.first())
        assertEquals("track:6", ids.last())
    }

    @Test
    fun albumsAndTheirTracksKeepStoredOrder() {
        val state =
            library(
                tracks = listOf(track("a"), track("b"), track("c", available = false)),
                albums =
                    listOf(
                        Album(
                            id = "album-1",
                            title = "Discovery",
                            artist = "Daft Punk",
                            year = "2001",
                            tracks = listOf("b", "a", "c"),
                            createdAt = 1,
                            updatedAt = 2,
                        )
                    ),
            )
        val albumNode = AutoLibrary.children(AutoLibrary.ALBUMS, state, labels).single()
        assertEquals("album:album-1", albumNode.id)
        assertEquals("Daft Punk · 2001", albumNode.artist)
        assertEquals(
            listOf("track:b", "track:a"),
            AutoLibrary.children("album:album-1", state, labels).map { it.id },
        )
    }

    @Test
    fun artistsAreGroupedAndSorted() {
        val state =
            library(
                tracks =
                    listOf(
                        track("1", artist = "Zeta"),
                        track("2", artist = "alpha"),
                        track("3", artist = "Alpha"),
                        track("4", artist = ""),
                    )
            )
        val ids = AutoLibrary.children(AutoLibrary.ARTISTS, state, labels).map { it.id }
        assertEquals(listOf("artist:alpha", "artist:Alpha", "artist:Zeta"), ids)
        assertEquals(
            listOf("track:2", "track:3"),
            AutoLibrary.children("artist:alpha", state, labels).map { it.id },
        )
    }

    @Test
    fun playlistChildrenFollowStoredOrderAndSkipUnknownTracks() {
        val state =
            library(
                tracks = listOf(track("1"), track("2")),
                playlists = listOf(Playlist(id = 7, name = "Road trip", tracks = listOf("2", "missing", "1"))),
            )
        assertEquals(
            listOf("track:2", "track:1"),
            AutoLibrary.children("playlist:7", state, labels).map { it.id },
        )
    }

    @Test
    fun searchMatchesTitleArtistAndAlbum() {
        val state =
            library(
                tracks =
                    listOf(
                        track("1", title = "One More Time", artist = "Daft Punk"),
                        track("2", title = "Aerodynamic", artist = "Daft Punk"),
                        track("3", title = "Other", artist = "Someone"),
                    ),
                albums =
                    listOf(
                        Album(
                            id = "a",
                            title = "Discovery",
                            artist = "Daft Punk",
                            tracks = listOf("1"),
                            createdAt = 1,
                            updatedAt = 1,
                        )
                    ),
            )
        assertEquals(listOf("track:3"), AutoLibrary.search("someone", state).map { it.id })
        assertEquals(listOf("track:1"), AutoLibrary.search("discovery", state).map { it.id })
        assertTrue(AutoLibrary.search("   ", state).isEmpty())
    }

    @Test
    fun searchResultsRespectTheLimit() {
        val state =
            library(
                tracks = (1..AutoLibrary.SEARCH_LIMIT + 10).map { track(it.toString(), title = "Same") }
            )
        assertEquals(AutoLibrary.SEARCH_LIMIT, AutoLibrary.search("same", state).size)
    }

    @Test
    fun artworkKeepsHttpUrlsAndDecodesDataUris() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val encoded = java.util.Base64.getEncoder().encodeToString(bytes)
        val state =
            library(
                tracks =
                    listOf(
                        track("1", art = "https://example.invalid/cover.jpg"),
                        track("2", art = "data:image/jpeg;base64,$encoded"),
                        track("3", art = "file:///tmp/cover.jpg"),
                    )
            )
        val items = AutoLibrary.children(AutoLibrary.ALL_TRACKS, state, labels)
        assertEquals("https://example.invalid/cover.jpg", items[0].artworkUrl)
        assertEquals("", items[1].artworkUrl)
        assertArrayEquals(bytes, items[1].artworkData)
        assertEquals("", items[2].artworkUrl)
        assertNull(items[2].artworkData)
    }

    @Test
    fun itemResolvesFoldersAndUnknownIds() {
        val state = library()
        assertEquals(AutoLibrary.HOME, AutoLibrary.item(AutoLibrary.HOME, state, labels)?.id)
        assertEquals(AutoLibrary.RECENT, AutoLibrary.item(AutoLibrary.RECENT, state, labels)?.id)
        assertNull(AutoLibrary.item("nope", state, labels))
        assertNull(AutoLibrary.item("track:missing", state, labels))
    }

    @Test
    fun malformedDataUriIsIgnored() {
        assertNull(AutoLibrary.decodeDataUri("data:image/jpeg,notbase64"))
        assertNull(AutoLibrary.decodeDataUri("data:image/png;base64,"))
        assertNull(AutoLibrary.decodeDataUri("https://example.invalid/x.jpg"))
    }
}
