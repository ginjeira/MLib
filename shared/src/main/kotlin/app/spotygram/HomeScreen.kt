package app.spotygram

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** Home shelves: continue listening, recent, favourites, playlists and albums. */
@Composable
fun HomeScreen(
    library: LibraryState,
    playingId: String,
    onPlay: (Track, List<Track>) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onSearchTelegram: (String) -> Unit,
    missingFor: (Album) -> Int? = { null },
) {
    val playable = remember(library.tracks) {
        library.tracks.filter { it.inLibrary && (it.local || it.available) }
    }
    val recent = remember(playable) { playable.sortedByDescending { it.date }.take(24) }
    val favorites = remember(playable) { playable.filter { it.liked }.take(24) }
    val continuing = library.byId[playingId]
    if (playable.isEmpty() && library.playlists.isEmpty()) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(tr(R.string.home_empty_title), style = MaterialTheme.typography.titleMedium)
            Text(
                tr(R.string.home_empty_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (continuing != null) {
            item(key = "continue") {
                Shelf(title = tr(R.string.home_continue)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(listOf(continuing), key = { "continue:${it.id}" }) { track ->
                            TrackTile(track, selected = true) {
                                onPlay(track, listOf(track) + recent.filter { it.id != track.id })
                            }
                        }
                    }
                }
            }
        }
        if (recent.isNotEmpty()) {
            item(key = "recent") {
                Shelf(title = tr(R.string.recently_added)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(recent, key = { "recent:${it.id}" }) { track ->
                            TrackTile(track) { onPlay(track, recent) }
                        }
                    }
                }
            }
        }
        if (favorites.isNotEmpty()) {
            item(key = "favorites") {
                Shelf(title = tr(R.string.favorites)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(favorites, key = { "fav:${it.id}" }) { track ->
                            TrackTile(track) { onPlay(track, favorites) }
                        }
                    }
                }
            }
        }
        if (library.playlists.isNotEmpty()) {
            item(key = "playlists") {
                Shelf(title = tr(R.string.playlists)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(library.playlists, key = { "pl:${it.id}" }) { playlist ->
                            PlaylistTile(playlist) { onOpenPlaylist(playlist.id) }
                        }
                    }
                }
            }
        }
        if (library.albums.isNotEmpty()) {
            item(key = "albums") {
                Shelf(title = tr(R.string.albums)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            library.albums.sortedWith(
                                compareBy<Album> { missingFor(it) == null }
                                    .thenByDescending { missingFor(it) ?: 0 }
                            ),
                            key = { "al:${it.id}" },
                        ) { album ->
                            AlbumTile(album, library, missingFor(album)) {
                                val tracks =
                                    album.tracks.mapNotNull(library.byId::get)
                                        .filter { it.local || it.available }
                                tracks.firstOrNull()?.let { onPlay(it, tracks) }
                            }
                        }
                    }
                }
            }
        }
        item(key = "discover") {
            TextButton(
                onClick = { onSearchTelegram("") },
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Icon(Icons.Rounded.Search, null)
                Spacer(Modifier.width(8.dp))
                Text(tr(R.string.home_discover))
            }
        }
    }
}

@Composable
private fun Shelf(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        content()
    }
}

@Composable
private fun TrackTile(track: Track, selected: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier.width(124.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
    ) {
        Artwork(
            track,
            124.dp,
        )
        Text(
            track.title,
            Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            track.artist,
            Modifier.padding(horizontal = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlaylistTile(playlist: Playlist, onClick: () -> Unit) {
    Column(
        Modifier.width(124.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
    ) {
        val cover = playlist.coverUrl
        if (cover.startsWith("https://") || cover.startsWith("http://") || cover.startsWith("data:image/")) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                modifier = Modifier.size(124.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            Column(
                Modifier.size(124.dp).clip(RoundedCornerShape(14.dp)).padding(8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    playlist.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            playlist.name,
            Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            tr(R.string.home_track_count, playlist.tracks.size),
            Modifier.padding(horizontal = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AlbumTile(
    album: Album,
    library: LibraryState,
    missing: Int?,
    onClick: () -> Unit,
) {
    Column(
        Modifier.width(124.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
    ) {
        val cover = album.coverUrl
        if (cover.startsWith("https://") || cover.startsWith("http://") || cover.startsWith("data:image/")) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                modifier = Modifier.size(124.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop,
            )
        } else {
            val first = album.tracks.firstNotNullOfOrNull(library.byId::get)
            Artwork(first, 124.dp)
        }
        Text(
            album.title,
            Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            album.artist,
            Modifier.padding(horizontal = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (missing != null) {
            Text(
                if (missing == 0) tr(R.string.home_album_complete)
                else tr(R.string.home_album_missing, missing),
                Modifier.padding(horizontal = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color =
                    if (missing == 0) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

/** Library search with an explicit hand-off to the Telegram acquisition flow. */
@Composable
fun SearchScreen(
    library: LibraryState,
    onPlay: (Track, List<Track>) -> Unit,
    onSearchTelegram: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val playable = remember(library.tracks) {
        library.tracks.filter { it.inLibrary && (it.local || it.available) }
    }
    val results =
        remember(query, playable) {
            val term = query.trim()
            if (term.isBlank()) {
                emptyList()
            } else {
                val albumTitles = buildMap {
                    for (album in library.albums) {
                        for (trackId in album.tracks) putIfAbsent(trackId, album.title)
                    }
                }
                playable.filter { track ->
                    track.title.contains(term, true) ||
                        track.artist.contains(term, true) ||
                        albumTitles[track.id].orEmpty().contains(term, true)
                }
            }
        }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text(tr(R.string.search_library)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        TextButton(
            onClick = { onSearchTelegram(query.trim()) },
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            Text(tr(R.string.search_telegram))
        }
        if (query.isNotBlank() && results.isEmpty()) {
            Text(
                tr(R.string.search_no_results),
                Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            items(results, key = { it.id }) { track ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { onPlay(track, results) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(track, 44.dp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(
                            track.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
