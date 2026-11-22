package app.spotygram

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/** Artist list and artist page, both derived from the library tracks. */
@Composable
fun ArtistsScreen(
    library: LibraryState,
    playingId: String,
    onPlay: (Track, List<Track>) -> Unit,
    onDownload: (List<String>) -> Unit,
    onSearchTelegram: (String) -> Unit,
    onKnowledge: suspend (String) -> KnowledgeInfo?,
) {
    val entries = remember(library.tracks, library.albums) { ArtistCatalog.entries(library) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    val profile = selected?.let { key -> entries.firstOrNull { it.key == key } }
    if (profile == null) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (entries.isEmpty()) {
                item(key = "empty") {
                    Text(
                        tr(R.string.home_empty_title),
                        Modifier.padding(24.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            items(entries, key = { "artist:${it.key}" }) { entry ->
                ArtistRow(entry, playingId) { selected = entry.key }
            }
        }
        return
    }
    ArtistPage(
        profile,
        playingId,
        onBack = { selected = null },
        onPlay = onPlay,
        onDownload = onDownload,
        onSearchTelegram = onSearchTelegram,
        onKnowledge = onKnowledge,
    )
}

@Composable
private fun ArtistRow(entry: ArtistProfile, playingId: String, onClick: () -> Unit) {
    val playing = entry.tracks.any { it.id == playingId }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(entry.playable.firstOrNull() ?: entry.tracks.firstOrNull(), 48.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                entry.name.ifBlank { tr(R.string.unknown_artist) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color =
                    if (playing) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                tr(
                    R.string.artist_summary,
                    entry.tracks.size,
                    entry.albums.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArtistPage(
    profile: ArtistProfile,
    playingId: String,
    onBack: () -> Unit,
    onPlay: (Track, List<Track>) -> Unit,
    onDownload: (List<String>) -> Unit,
    onSearchTelegram: (String) -> Unit,
    onKnowledge: suspend (String) -> KnowledgeInfo?,
) {
    var knowledge by remember(profile.key) { mutableStateOf<KnowledgeInfo?>(null) }
    var knowledgeBusy by remember(profile.key) { mutableStateOf(false) }
    var knowledgeError by remember(profile.key) { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(profile.key) {
        if (profile.unknown) return@LaunchedEffect
        knowledgeBusy = true
        knowledge =
            runCatching { onKnowledge(profile.name) }
                .getOrElse {
                    knowledgeError = it.message ?: tr(R.string.album_knowledge_failed)
                    null
                }
        knowledgeBusy = false
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
        item(key = "header") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr(R.string.back))
                }
                Artwork(
                    profile.playable.firstOrNull() ?: profile.tracks.firstOrNull(),
                    88.dp,
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        profile.name.ifBlank { tr(R.string.unknown_artist) },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        tr(R.string.artist_summary, profile.tracks.size, profile.albums.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item(key = "actions") {
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TextButton(
                    enabled = profile.playable.isNotEmpty(),
                    onClick = { onPlay(profile.playable.first(), profile.playable) },
                ) { Text(tr(R.string.play_all)) }
                TextButton(
                    enabled = profile.playable.size > 1,
                    onClick = {
                        val shuffled = profile.playable.shuffled()
                        onPlay(shuffled.first(), shuffled)
                    },
                ) { Text(tr(R.string.shuffle)) }
                TextButton(
                    enabled = profile.playable.any { it.available && !it.offlineCopyValid },
                    onClick = { onDownload(profile.playable.map { it.id }) },
                ) { Text(tr(R.string.download_offline)) }
                TextButton(
                    enabled = profile.name.isNotBlank(),
                    onClick = { onSearchTelegram(profile.name) },
                ) { Text(tr(R.string.search_telegram)) }
            }
        }

        if (profile.playableAlbums.isNotEmpty()) {
            item(key = "albums") {
                Text(
                    tr(R.string.albums),
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(profile.playableAlbums, key = { "album:${it.id}" }) { album ->
                        ArtistAlbumTile(album, profile) { tracks ->
                            onPlay(tracks.first(), tracks)
                        }
                    }
                }
            }
        }
        item(key = "tracks-header") {
            Text(
                tr(R.string.all_tracks),
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        items(profile.tracks, key = { "track:${it.id}" }) { track ->
            Row(
                Modifier.fillMaxWidth().clickable { onPlay(track, profile.playable) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Artwork(track, 44.dp)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(
                        track.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color =
                            if (track.id == playingId) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
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
        if (profile.sources.isNotEmpty()) {
            item(key = "sources") {
                Text(
                    tr(R.string.sources),
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    profile.sources.joinToString(" · "),
                    Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item(key = "about") {
            ArtistAbout(knowledge, knowledgeBusy, knowledgeError) { url ->
                uriHandler.openUri(url)
            }
        }
    }
}

@Composable
private fun ArtistAbout(
    info: KnowledgeInfo?,
    busy: Boolean,
    error: String,
    onOpenSource: (String) -> Unit,
) {
    if (info == null && !busy && error.isBlank()) return
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(tr(R.string.about_artist), style = MaterialTheme.typography.titleMedium)
        if (busy) {
            Text(
                tr(R.string.album_knowledge_loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (error.isNotBlank()) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (info != null) {
            if (info.releaseDate.isNotBlank() || info.kind.isNotBlank()) {
                Text(
                    listOf(info.releaseDate, info.kind).filter(String::isNotBlank)
                        .joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (info.genres.isNotBlank()) {
                Text(info.genres, style = MaterialTheme.typography.bodySmall)
            }
            if (info.summary.isNotBlank()) {
                Text(info.summary, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                tr(R.string.album_knowledge_source, info.sourceNames),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (info.fetchedAt > 0) {
                val collected =
                    java.time.Instant.ofEpochMilli(info.fetchedAt)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .toString()
                Text(
                    tr(R.string.album_knowledge_updated, collected),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (info.sourceUrl.isNotBlank()) {
                TextButton(onClick = { onOpenSource(info.sourceUrl) }) {
                    Text(tr(R.string.album_knowledge_open_source))
                }
            }
        }
    }
}

@Composable
private fun ArtistAlbumTile(
    album: Album,
    profile: ArtistProfile,
    onPlay: (List<Track>) -> Unit,
) {
    val tracks =
        album.tracks.mapNotNull { id -> profile.tracks.firstOrNull { it.id == id } }
            .filter { it.local || it.available }
    Column(Modifier.width(124.dp).clip(RoundedCornerShape(14.dp))) {
        val cover = album.coverUrl
        if (cover.startsWith("https://") || cover.startsWith("http://") || cover.startsWith("data:image/")) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                modifier =
                    Modifier.size(124.dp).clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = tracks.isNotEmpty()) { onPlay(tracks) },
                contentScale = ContentScale.Crop,
            )
        } else {
            Column(Modifier.clickable(enabled = tracks.isNotEmpty()) { onPlay(tracks) }) {
                Artwork(tracks.firstOrNull(), 124.dp)
            }
        }
        Text(
            album.title,
            Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(2.dp))
        Text(
            tr(R.string.home_track_count, tracks.size),
            Modifier.padding(horizontal = 2.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
