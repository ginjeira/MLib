package app.spotygram

/** One artist built from the library tracks; no new database entity. */
class ArtistProfile(
    val key: String,
    val name: String,
    val tracks: List<Track>,
    val albums: List<Album>,
    val sources: List<String>,
    val unknown: Boolean,
) {
    val playable: List<Track>
        get() = tracks.filter { it.local || it.available }

    val playableAlbums: List<Album>
        get() {
            val ids = playable.mapTo(HashSet()) { it.id }
            return albums.filter { album -> album.tracks.any { it in ids } }
        }
}

/**
 * Groups library tracks by a normalized artist name. Spelling variants stay
 * separate on purpose: merging artists needs an MBID and an explicit decision.
 */
object ArtistCatalog {
    fun normalize(name: String): String = name.trim().replace(Regex("\\s+"), " ").lowercase()

    fun entries(library: LibraryState): List<ArtistProfile> {
        val groups = LinkedHashMap<String, MutableList<Track>>()
        for (track in library.tracks) {
            if (!track.inLibrary) continue
            groups.getOrPut(normalize(track.artist)) { mutableListOf() } += track
        }
        return groups
            .map { (key, tracks) -> profileOf(key, tracks, library) }
            .sortedWith(
                compareBy<ArtistProfile> { it.unknown }.thenBy { it.name.lowercase() }
            )
    }

    fun profile(name: String, library: LibraryState): ArtistProfile? =
        entries(library).firstOrNull { it.key == normalize(name) }

    private fun profileOf(
        key: String,
        tracks: List<Track>,
        library: LibraryState,
    ): ArtistProfile {
        val ordered =
            tracks.sortedWith(
                compareBy<Track> { it.title.lowercase() }.thenBy { it.artist.lowercase() }
            )
        val trackIds = ordered.mapTo(HashSet()) { it.id }
        val albums =
            library.albums
                .filter { album -> album.tracks.any { it in trackIds } }
                .sortedWith(compareBy<Album> { it.artist.lowercase() }.thenBy { it.title.lowercase() })
        val sources =
            ordered.mapNotNull { it.source.takeIf(String::isNotBlank) }
                .distinct()
                .sorted()
        return ArtistProfile(
            key = key,
            name = displayName(ordered),
            tracks = ordered,
            albums = albums,
            sources = sources,
            unknown = key.isBlank(),
        )
    }

    /** Picks the most common spelling, breaking ties alphabetically. */
    private fun displayName(tracks: List<Track>): String {
        val names = tracks.map { it.artist.trim() }.filter(String::isNotBlank)
        if (names.isEmpty()) return ""
        return names
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .first()
            .key
    }
}
