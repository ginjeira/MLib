package app.spotygram

import java.util.Base64

/** One entry exposed to Android Auto; the service maps it to a Media3 MediaItem. */
class AutoNode(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val artworkUrl: String = "",
    val artworkData: ByteArray? = null,
    val playable: Boolean = false,
    val playableCollection: Boolean = false,
) {
    override fun equals(other: Any?): Boolean =
        other is AutoNode &&
            id == other.id &&
            title == other.title &&
            artist == other.artist &&
            album == other.album &&
            artworkUrl == other.artworkUrl &&
            artworkData.contentEquals(other.artworkData) &&
            playable == other.playable &&
            playableCollection == other.playableCollection

    override fun hashCode(): Int =
        listOf(id, title, artist, album, artworkUrl, playable, playableCollection).hashCode() * 31 +
            (artworkData?.contentHashCode() ?: 0)
}

/**
 * Pure Android Auto browse model. Keeping it out of the service makes the tree,
 * the filters and the search testable without an Android runtime.
 */
object AutoLibrary {
    const val ROOT = "mlib:root"
    const val HOME = "mlib:home"
    const val ALL_TRACKS = "mlib:tracks"
    const val PLAYLISTS = "mlib:playlists"
    const val ALBUMS = "mlib:albums"
    const val ARTISTS = "mlib:artists"
    const val FAVORITES = "mlib:favorites"
    const val OFFLINE = "mlib:offline"
    const val RECENT = "mlib:recent"
    const val TRACK_PREFIX = "track:"
    const val PLAYLIST_PREFIX = "playlist:"
    const val ALBUM_PREFIX = "album:"
    const val ARTIST_PREFIX = "artist:"
    const val RECENT_LIMIT = 100
    const val SEARCH_LIMIT = 100

    data class Labels(
        val home: String,
        val allTracks: String,
        val playlists: String,
        val albums: String,
        val artists: String,
        val favorites: String,
        val offline: String,
        val recent: String,
    )

    fun root(labels: Labels): List<AutoNode> =
        listOf(
            folder(HOME, labels.home),
            folder(PLAYLISTS, labels.playlists),
            folder(ALBUMS, labels.albums),
            folder(ARTISTS, labels.artists),
            collection(FAVORITES, labels.favorites),
            collection(OFFLINE, labels.offline),
            collection(ALL_TRACKS, labels.allTracks),
        )

    fun children(parentId: String, library: LibraryState, labels: Labels): List<AutoNode> =
        when (parentId) {
            ROOT -> root(labels)
            HOME ->
                listOf(
                    collection(RECENT, labels.recent),
                    collection(FAVORITES, labels.favorites),
                    collection(OFFLINE, labels.offline),
                    folder(PLAYLISTS, labels.playlists),
                )
            ALL_TRACKS -> library.tracks.playable().sortedByTitle().map(::trackNode)
            FAVORITES ->
                library.tracks.playable().filter { it.liked }.sortedByTitle().map(::trackNode)
            OFFLINE ->
                library.tracks.filter { it.local && !it.temporary }.sortedByTitle().map(::trackNode)
            RECENT ->
                library.tracks.playable().sortedByDescending { it.date }
                    .take(RECENT_LIMIT).map(::trackNode)
            PLAYLISTS ->
                library.playlists.map { collection("$PLAYLIST_PREFIX${it.id}", it.name) }
            ARTISTS ->
                library.tracks.playable().filter { it.artist.isNotBlank() }
                    .groupBy { it.artist }
                    .toList()
                    .sortedBy { it.first.lowercase() }
                    .map { (artist, _) -> collection("$ARTIST_PREFIX$artist", artist) }
            ALBUMS -> library.albums.map { albumNode(it) }
            else -> childrenOfFolder(parentId, library)
        }

    fun item(id: String, library: LibraryState, labels: Labels): AutoNode? =
        when {
            id == ROOT -> folder(ROOT, labels.home)
            id == HOME -> folder(HOME, labels.home)
            id == ALL_TRACKS -> collection(ALL_TRACKS, labels.allTracks)
            id == PLAYLISTS -> folder(PLAYLISTS, labels.playlists)
            id == ALBUMS -> folder(ALBUMS, labels.albums)
            id == ARTISTS -> folder(ARTISTS, labels.artists)
            id == FAVORITES -> collection(FAVORITES, labels.favorites)
            id == OFFLINE -> collection(OFFLINE, labels.offline)
            id == RECENT -> collection(RECENT, labels.recent)
            id.startsWith(TRACK_PREFIX) ->
                library.byId[id.removePrefix(TRACK_PREFIX)]?.let(::trackNode)
            id.startsWith(PLAYLIST_PREFIX) -> {
                val playlistId = id.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
                library.playlists.firstOrNull { it.id == playlistId }?.let {
                    collection(id, it.name)
                }
            }
            id.startsWith(ARTIST_PREFIX) -> collection(id, id.removePrefix(ARTIST_PREFIX))
            id.startsWith(ALBUM_PREFIX) -> {
                val albumId = id.removePrefix(ALBUM_PREFIX)
                library.albums.firstOrNull { it.id == albumId }?.let(::albumNode)
            }
            else -> null
        }

    fun search(query: String, library: LibraryState): List<AutoNode> {
        val term = query.trim()
        if (term.isBlank()) return emptyList()
        val albumTitles = albumTitlesByTrack(library)
        return library.tracks.playable()
            .asSequence()
            .filter { track ->
                track.title.contains(term, true) ||
                    track.artist.contains(term, true) ||
                    albumTitles[track.id].orEmpty().contains(term, true)
            }
            .distinctBy { it.id }
            .take(SEARCH_LIMIT)
            .map { trackNode(it, albumTitles) }
            .toList()
    }

    private fun childrenOfFolder(parentId: String, library: LibraryState): List<AutoNode> =
        when {
            parentId.startsWith(PLAYLIST_PREFIX) -> {
                val playlistId = parentId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
                val playlist = library.playlists.firstOrNull { it.id == playlistId }
                playlist?.tracks.orEmpty().mapNotNull(library.byId::get)
                    .playable().map(::trackNode)
            }
            parentId.startsWith(ARTIST_PREFIX) -> {
                val artist = parentId.removePrefix(ARTIST_PREFIX)
                library.tracks.playable()
                    .filter { it.artist.equals(artist, ignoreCase = true) }
                    .sortedByTitle().map(::trackNode)
            }
            parentId.startsWith(ALBUM_PREFIX) -> {
                val albumId = parentId.removePrefix(ALBUM_PREFIX)
                val album = library.albums.firstOrNull { it.id == albumId }
                album?.tracks.orEmpty().mapNotNull(library.byId::get)
                    .playable().map(::trackNode)
            }
            else -> emptyList()
        }

    private fun albumTitlesByTrack(library: LibraryState): Map<String, String> {
        val titles = HashMap<String, String>()
        for (album in library.albums) {
            for (trackId in album.tracks) titles.putIfAbsent(trackId, album.title)
        }
        return titles
    }

    private fun Sequence<Track>.playable(): Sequence<Track> =
        filter { it.local || it.available }

    private fun List<Track>.playable(): List<Track> = filter { it.local || it.available }

    private fun List<Track>.sortedByTitle(): List<Track> =
        sortedWith(compareBy<Track> { it.title.lowercase() }.thenBy { it.artist.lowercase() })

    private fun folder(id: String, title: String): AutoNode =
        AutoNode(id = id, title = title)

    /** A browsable folder that is also playable as a whole, like a playlist. */
    private fun collection(id: String, title: String): AutoNode =
        AutoNode(id = id, title = title, playableCollection = true)

    private fun albumNode(album: Album): AutoNode {
        val artwork = artworkOf(album.coverUrl)
        return AutoNode(
            id = "$ALBUM_PREFIX${album.id}",
            title = album.title,
            artist = listOf(album.artist, album.year).filter(String::isNotBlank).joinToString(" · "),
            artworkUrl = artwork.first,
            artworkData = artwork.second,
            playableCollection = true,
        )
    }

    private fun trackNode(track: Track): AutoNode =
        trackNode(track, emptyMap())

    private fun trackNode(track: Track, albumTitles: Map<String, String>): AutoNode {
        val artwork = artworkOf(track.art)
        return AutoNode(
            id = "$TRACK_PREFIX${track.id}",
            title = track.title,
            artist = track.artist,
            album = albumTitles[track.id].orEmpty(),
            artworkUrl = artwork.first,
            artworkData = artwork.second,
            playable = true,
        )
    }

    private fun artworkOf(url: String): Pair<String, ByteArray?> =
        when {
            url.startsWith("data:image/") -> "" to decodeDataUri(url)
            url.startsWith("https://") || url.startsWith("http://") -> url to null
            else -> "" to null
        }

    /** Decodes the payload of a base64 data URI; other data URIs are ignored. */
    fun decodeDataUri(url: String): ByteArray? {
        val comma = url.indexOf(',')
        if (comma <= 0 || !url.substring(0, comma).contains(";base64")) return null
        val payload = url.substring(comma + 1)
        if (payload.isBlank()) return null
        return try {
            Base64.getDecoder().decode(payload).takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}
