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

@Composable
fun AlbumsScreen(
    library: LibraryState,
    onOpen: (String) -> Unit,
    onPlay: (Track, List<Track>) -> Unit,
    onDelete: (String) -> Unit,
    onCreate: (String, String, List<String>) -> Unit,
    onDeduplicate: () -> Unit,
    onUpdate: (String, String, String, String, String, String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Album?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newArtist by remember { mutableStateOf("") }
    var selectedTracks by remember { mutableStateOf(setOf<String>()) }
    var editing by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf("") }
    var editArtist by remember { mutableStateOf("") }
    var editYear by remember { mutableStateOf("") }
    var editCover by remember { mutableStateOf("") }
    var editGenre by remember { mutableStateOf("") }
    val current = selected?.let { album -> library.albums.firstOrNull { it.id == album.id } }
    LaunchedEffect(current?.id, library.albums) {
        if (selected != null && current == null) selected = null
    }
    if (current != null) {
        val tracks = current.tracks.mapNotNull { library.byId[it] }
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { selected = null }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = tr(R.string.back))
                }
                AlbumArtwork(current, tracks.firstOrNull(), 48.dp)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(current.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(current.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = {
                    editTitle = current.title; editArtist = current.artist; editYear = current.year
                    editCover = current.coverUrl; editGenre = current.genre; editing = true
                }) { Text("Editar") }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.Delete, contentDescription = tr(R.string.album_delete))
                }
            }
            Text(tr(R.string.album_tracks, tracks.size), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(tracks, key = { it.id }) { track ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPlay(track, tracks) }.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(seconds(track.duration.toLong()), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        if (editing) {
            AlertDialog(
                onDismissRequest = { editing = false },
                title = { Text("Editar álbum") },
                text = {
                    Column {
                        OutlinedTextField(value = editTitle, onValueChange = { editTitle = it }, label = { Text("Título") }, singleLine = true)
                        OutlinedTextField(value = editArtist, onValueChange = { editArtist = it }, label = { Text("Artista") }, singleLine = true)
                        OutlinedTextField(value = editYear, onValueChange = { editYear = it }, label = { Text("Ano") }, singleLine = true)
                        OutlinedTextField(value = editGenre, onValueChange = { editGenre = it }, label = { Text("Género") }, singleLine = true)
                        OutlinedTextField(value = editCover, onValueChange = { editCover = it }, label = { Text("URL da capa") }, singleLine = true)
                    }
                },
                confirmButton = {
                    TextButton(enabled = editTitle.isNotBlank() && editArtist.isNotBlank(), onClick = {
                        onUpdate(current.id,editTitle.trim(),editArtist.trim(),editYear.trim(),editCover.trim(),editGenre.trim())
                        editing = false
                    }) { Text("Guardar") }
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
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tr(R.string.albums), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                Text(albums.size.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onDeduplicate) { Text("Unificar duplicados") }
                TextButton(onClick = { creating = true }) { Text("Novo álbum") }
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
                    title = { Text("Criar álbum") },
                    text = {
                        Column(Modifier.heightIn(max = 520.dp)) {
                            OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Título") }, singleLine = true)
                            OutlinedTextField(value = newArtist, onValueChange = { newArtist = it }, label = { Text("Artista") }, singleLine = true)
                            Text("Selecciona as faixas", Modifier.padding(vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
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
                        }) { Text("Criar") }
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
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                                Text(album.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(tr(R.string.album_tracks, library.albumCounts[album.id] ?: album.tracks.size), style = MaterialTheme.typography.labelSmall)
                            }
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
