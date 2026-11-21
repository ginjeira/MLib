package app.spotygram

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AlbumsScreen(
    library: LibraryState,
    onOpen: (String) -> Unit,
    onPlay: (Track, List<Track>) -> Unit,
    onDelete: (String) -> Unit,
    onCreate: (String, String, List<String>) -> Unit,
    onDeduplicate: () -> Unit,
    onAutoDetect: () -> Unit,
    onDownload: (Album) -> Unit,
    onDownloadToDevice: (List<String>) -> Unit,
    download: DownloadState,
    onRemoveTrack: (String, String) -> Unit,
    onReorder: (String, List<String>) -> Unit,
    onUpdate: (String, String, String, String, String, String) -> Unit,
    onSuggestCovers: suspend (String, String) -> List<String>,
    onRepairUnavailable: suspend (Album) -> Unit,
    onEnrich: suspend (Album) -> AlbumEnrichment?,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Album?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDeduplicate by remember { mutableStateOf(false) }
    var confirmComplete by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newArtist by remember { mutableStateOf("") }
    var selectedTracks by remember { mutableStateOf(setOf<String>()) }
    var editing by remember { mutableStateOf(false) }
    var enriching by remember { mutableStateOf(false) }
    var enrichError by remember { mutableStateOf("") }
    var editTitle by remember { mutableStateOf("") }
    var editArtist by remember { mutableStateOf("") }
    var editYear by remember { mutableStateOf("") }
    var editCover by remember { mutableStateOf("") }
    var editGenre by remember { mutableStateOf("") }
    var coverSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var coverSearchBusy by remember { mutableStateOf(false) }
    var coverSearchError by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val current = selected?.let { album -> library.albums.firstOrNull { it.id == album.id } }
    LaunchedEffect(current?.id, library.albums) {
        if (selected != null && current == null) selected = null
    }
    if (current != null) {
        val tracks = current.tracks.mapNotNull { library.byId[it] }
        LaunchedEffect(current.id, current.tracks.size) {
            onRepairUnavailable(current)
        }
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { selected = null }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = tr(R.string.back))
                    }
                    AlbumArtwork(current, tracks.firstOrNull(), 56.dp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(current.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(listOf(current.artist, current.year, current.genre).filter(String::isNotBlank).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OfflineDownloadButton(
                        downloaded = tracks.isNotEmpty() && tracks.size == current.tracks.size && tracks.all { it.offlineCopyValid },
                        downloading = download.trackId in tracks.map { it.id },
                        enabled = tracks.any { it.available && !it.offlineCopyValid },
                        contentDescription = tr(R.string.album_download_offline),
                        onClick = { onDownloadToDevice(tracks.map { it.id }) },
                    )
                    TextButton(onClick = { confirmComplete = true }) { Text(tr(R.string.album_find_tracks)) }
                    TextButton(onClick = {
                        editTitle = current.title; editArtist = current.artist; editYear = current.year
                        editCover = current.coverUrl; editGenre = current.genre; coverSuggestions = emptyList(); coverSearchError = ""; editing = true
                    }) { Text(tr(R.string.edit)) }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = tr(R.string.album_delete))
                    }
                }
            }
            Text(tr(R.string.album_tracks, tracks.size), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(tracks, key = { it.id }) { track ->
                    val index = tracks.indexOfFirst { it.id == track.id }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            if (index > 0) onReorder(current.id, tracks.map { it.id }.toMutableList().apply { add(index - 1, removeAt(index)) })
                        }, enabled = index > 0) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = tr(R.string.move_up))
                        }
                        IconButton(onClick = {
                            if (index < tracks.lastIndex) onReorder(current.id, tracks.map { it.id }.toMutableList().apply { add(index + 1, removeAt(index)) })
                        }, enabled = index < tracks.lastIndex) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = tr(R.string.move_down))
                        }
                        Column(Modifier.weight(1f).clickable { onPlay(track, tracks) }.padding(vertical = 8.dp)) {
                            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(seconds(track.duration.toLong()), style = MaterialTheme.typography.labelSmall)
                        IconButton(onClick = { onRemoveTrack(current.id, track.id) }) {
                            Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = tr(R.string.album_remove_track))
                        }
                    }
                }
            }
        }
        if (editing) {
            AlertDialog(
                onDismissRequest = { editing = false },
                title = { Text(tr(R.string.album_edit_title)) },
                text = {
                    Column {
                        OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text(tr(R.string.title)) }, singleLine = true)
                        OutlinedTextField(value = editArtist, onValueChange = { editArtist = it }, label = { Text(tr(R.string.artist)) }, singleLine = true)
                        OutlinedTextField(value = editYear, onValueChange = { editYear = it }, label = { Text(tr(R.string.year)) }, singleLine = true)
                        OutlinedTextField(value = editGenre, onValueChange = { editGenre = it }, label = { Text(tr(R.string.genre)) }, singleLine = true)
                        OutlinedTextField(value = editCover, onValueChange = { editCover = it }, label = { Text(tr(R.string.cover_url)) }, singleLine = true)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                enabled = !enriching,
                                onClick = {
                                    scope.launch {
                                        enriching = true
                                        enrichError = ""
                                        val enriched =
                                            runCatching { onEnrich(current) }
                                                .getOrElse {
                                                    enrichError =
                                                        it.message ?: tr(R.string.album_enrich_failed)
                                                    null
                                                }
                                        if (enriched != null) {
                                            editTitle = editTitle.ifBlank { enriched.title }
                                            editArtist = editArtist.ifBlank { enriched.artist }
                                            editYear = editYear.ifBlank { enriched.year }
                                            editGenre = editGenre.ifBlank { enriched.genre }
                                            editCover = editCover.ifBlank { enriched.coverUrl }
                                        }
                                        enriching = false
                                    }
                                },
                            ) {
                                Text(
                                    tr(
                                        if (enriching) R.string.album_enriching
                                        else R.string.album_enrich
                                    )
                                )
                            }
                        }
                        if (enrichError.isNotBlank()) {
                            Text(
                                enrichError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(enabled = !coverSearchBusy && editTitle.isNotBlank(), onClick = {
                                scope.launch {
                                    coverSearchBusy = true
                                    coverSearchError = ""
                                    coverSuggestions = runCatching { onSuggestCovers(editTitle, editArtist) }
                                        .getOrElse { coverSearchError = it.message ?: tr(R.string.album_cover_search_failed); emptyList() }
                                    coverSearchBusy = false
                                    if (coverSuggestions.isEmpty() && coverSearchError.isBlank()) coverSearchError = tr(R.string.album_no_covers)
                                }
                            }) { Text(tr(if (coverSearchBusy) R.string.album_searching_covers else R.string.album_search_covers)) }
                            if (coverSuggestions.isNotEmpty()) Text(tr(R.string.album_cover_suggestions, coverSuggestions.size), style = MaterialTheme.typography.labelSmall)
                        }
                        if (coverSearchError.isNotBlank()) Text(coverSearchError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        coverSuggestions.take(8).forEach { cover ->
                            Row(
                                Modifier.fillMaxWidth().clickable { editCover = cover }.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AsyncImage(model = cover, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                                Text(tr(R.string.album_use_cover), Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(enabled = editTitle.isNotBlank() && editArtist.isNotBlank(), onClick = {
                        onUpdate(current.id,editTitle.trim(),editArtist.trim(),editYear.trim(),editCover.trim(),editGenre.trim())
                        editing = false
                    }) { Text(tr(R.string.save)) }
                },
                dismissButton = { TextButton(onClick = { editing = false }) { Text(tr(R.string.cancel)) } },
            )
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text(tr(R.string.album_delete)) },
                text = { Text(current.title) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        onDelete(current.id)
                        selected = null
                    }) { Text(tr(R.string.delete)) }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr(R.string.cancel)) } },
            )
        }
    } else {
        val albums = remember(library.albums, query) {
            library.albums.filter {
                query.isBlank() || it.title.contains(query, true) || it.artist.contains(query, true)
            }
        }
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(tr(R.string.albums), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                    Text(albums.size.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onAutoDetect, modifier = Modifier.weight(1f)) {
                        Text(tr(R.string.album_detect), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { confirmDeduplicate = true }, modifier = Modifier.weight(1f)) {
                        Text(tr(R.string.album_deduplicate), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { creating = true }, modifier = Modifier.weight(1f)) {
                        Text(tr(R.string.album_new), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (confirmComplete) {
                AlertDialog(
                    onDismissRequest = { confirmComplete = false },
                    title = { Text(tr(R.string.album_complete_title)) },
                    text = {
                        Text(
                            tr(
                                R.string.album_complete_help,
                                current?.title.orEmpty(),
                                current?.tracks?.size ?: 0,
                            )
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmComplete = false
                            current?.let(onDownload)
                        }) { Text(tr(R.string.album_complete_action)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmComplete = false }) {
                            Text(tr(R.string.cancel))
                        }
                    },
                )
            }
            if (confirmDeduplicate) {
                AlertDialog(
                    onDismissRequest = { confirmDeduplicate = false },
                    title = { Text(tr(R.string.album_deduplicate_title)) },
                    text = { Text(tr(R.string.album_deduplicate_help)) },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmDeduplicate = false
                            onDeduplicate()
                        }) { Text(tr(R.string.album_deduplicate_action)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmDeduplicate = false }) {
                            Text(tr(R.string.cancel))
                        }
                    },
                )
            }
            if (library.albums.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    label = { Text(tr(R.string.acquisition_query)) },
                )
            }
            if (creating) {
                AlertDialog(
                    onDismissRequest = { creating = false },
                    title = { Text(tr(R.string.album_create)) },
                    text = {
                        Column(Modifier.heightIn(max = 520.dp)) {
                            OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text(tr(R.string.title)) }, singleLine = true)
                            OutlinedTextField(value = newArtist, onValueChange = { newArtist = it }, label = { Text(tr(R.string.artist)) }, singleLine = true)
                            Text(tr(R.string.album_select_tracks), Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
                            LazyColumn(Modifier.weight(1f)) {
                                items(library.tracks.filter { it.inLibrary }, key = { it.id }) { track ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = track.id in selectedTracks, onCheckedChange = { checked ->
                                            selectedTracks = if (checked) selectedTracks + track.id else selectedTracks - track.id
                                        })
                                        Column(Modifier.weight(1f)) {
                                            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(track.artist, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(enabled = newTitle.isNotBlank() && newArtist.isNotBlank() && selectedTracks.isNotEmpty(), onClick = {
                            onCreate(newTitle.trim(), newArtist.trim(), library.tracks.map { it.id }.filter { it in selectedTracks })
                            creating = false
                            newTitle = ""; newArtist = ""; selectedTracks = emptySet()
                        }) { Text(tr(R.string.create)) }
                    },
                    dismissButton = { TextButton(onClick = { creating = false }) { Text(tr(R.string.cancel)) } },
                )
            }
            if (albums.isEmpty()) {
                Text(
                    if (query.isBlank()) tr(R.string.albums_empty) else tr(R.string.no_results),
                    Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(albums, key = { it.id }) { album ->
                        Row(
                            Modifier.fillMaxWidth().clickable { selected = album; onOpen(album.id) }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AlbumArtwork(album, library.byId[album.tracks.firstOrNull()], 52.dp)
                            val albumTracks = album.tracks.mapNotNull { library.byId[it] }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                                Text(album.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(tr(R.string.album_tracks, library.albumCounts[album.id] ?: album.tracks.size), style = MaterialTheme.typography.labelSmall)
                            }
                            OfflineDownloadButton(
                                downloaded = albumTracks.isNotEmpty() && albumTracks.size == album.tracks.size && albumTracks.all { it.offlineCopyValid },
                                downloading = download.trackId in album.tracks,
                                enabled = albumTracks.any { it.available && !it.offlineCopyValid },
                                contentDescription = tr(R.string.album_download_offline),
                                onClick = { onDownloadToDevice(album.tracks) },
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun AlbumArtwork(album: Album, fallback: Track?, size: androidx.compose.ui.unit.Dp) {
    val url = album.coverUrl.takeIf { it.startsWith("https://") || it.startsWith("http://") }
    if (url != null) {
        AsyncImage(
            model = url,
            contentDescription = album.title,
            modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.23f)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Artwork(fallback, size)
    }
}
