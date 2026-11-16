package app.spotygram

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AcquisitionSheet(
    app: SpotygramApp,
    onClose: () -> Unit,
) {
    val acquisition = app.acquisition
    val state by acquisition.state.collectAsStateWithLifecycle()
    val hasPendingJob = state.pendingJob != null
    val auth by app.telegram.auth.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var contentType by remember { mutableStateOf(AcquisitionContentType.TRACK) }
    var provider by remember { mutableStateOf(AcquisitionProvider.DEEZER) }
    var destinations by remember { mutableStateOf<List<ChatChoice>>(emptyList()) }
    var destinationId by rememberSaveable { mutableLongStateOf(0L) }
    var destinationMenu by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Int>() }

    suspend fun loadDestinations() {
        val localMatches = runCatching { app.searchChats("GinjMusic") }.getOrDefault(emptyList())
        destinations = if (localMatches.isNotEmpty()) localMatches else
            runCatching { app.searchChats("GinjMusic", onServer = true) }.getOrDefault(emptyList())
        if (destinations.none { it.id == destinationId }) {
            // Never silently fall back to an arbitrary chat: an acquisition could
            // otherwise forward music to the wrong conversation.
            destinationId = destinations.firstOrNull {
                it.title.contains("GinjMusic", ignoreCase = true)
            }?.id ?: 0L
        }
    }

    LaunchedEffect(auth.type) {
        if (auth.type == "authorizationStateReady") loadDestinations()
    }
    LaunchedEffect(state.search?.id) {
        selected.clear()
        state.search?.let { search ->
            query = search.query
            contentType = search.contentType
            provider = search.provider
        }
    }

    Column(
        Modifier.fillMaxWidth()
            .heightIn(max = 720.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsHeader(R.string.music_acquisition, onClose)

        Text(
            tr(R.string.acquisition_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // The pending journal is written before forwarding starts. While
        // acquisition is active it is not an interrupted job yet: hide the
        // resume/discard controls so they are not shown as inert buttons.
        if (!state.acquiring && !state.searching) state.pendingJob?.let { pending ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr(R.string.acquisition_interrupted, pending.query),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Button(
                        onClick = { scope.launch { withContext(Dispatchers.IO) { runCatching { acquisition.resumePending() } } } },
                        enabled = auth.type == "authorizationStateReady" && !state.searching && !state.acquiring,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(tr(R.string.acquisition_resume_pending))
                    }
                    TextButton(
                        onClick = { acquisition.discardPending() },
                        enabled = !state.searching && !state.acquiring,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(tr(R.string.acquisition_discard_pending))
                    }
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr(R.string.acquisition_query)) },
            singleLine = true,
            enabled = !state.searching && !state.acquiring && !hasPendingJob,
        )

        if (contentType == AcquisitionContentType.ARTIST) {
            Text(
                tr(R.string.acquisition_artist_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(tr(R.string.acquisition_type), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AcquisitionContentType.entries.forEach { option ->
                FilterChip(
                    selected = contentType == option,
                    onClick = { contentType = option },
                    label = { Text(tr(acquisitionTypeLabel(option))) },
                    enabled = !state.searching && !state.acquiring && !hasPendingJob,
                )
            }
        }

        Text(tr(R.string.acquisition_provider), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AcquisitionProvider.entries.forEach { option ->
                FilterChip(
                    selected = provider == option,
                    onClick = { provider = option },
                    label = { Text(tr(if (option == AcquisitionProvider.DEEZER) R.string.acquisition_deezer else R.string.acquisition_soundcloud)) },
                    enabled = !state.searching && !state.acquiring && !hasPendingJob,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr(R.string.acquisition_destination), style = MaterialTheme.typography.labelLarge)
                Text(
                    destinations.firstOrNull { it.id == destinationId }?.displayTitle
                        ?: tr(R.string.acquisition_destination_missing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                OutlinedButton(
                    onClick = { destinationMenu = true },
                    enabled = destinations.isNotEmpty() && !state.acquiring && !hasPendingJob,
                ) { Text(tr(R.string.acquisition_choose_destination)) }
                DropdownMenu(
                    expanded = destinationMenu,
                    onDismissRequest = { destinationMenu = false },
                ) {
                    destinations.forEach { chat ->
                        DropdownMenuItem(
                            text = { Text(chat.displayTitle) },
                            onClick = {
                                destinationId = chat.id
                                destinationMenu = false
                            },
                        )
                    }
                }
            }
            IconButton(
                onClick = { scope.launch { loadDestinations() } },
                enabled = !state.acquiring && !hasPendingJob,
            ) {
                Icon(Icons.Rounded.Refresh, tr(R.string.acquisition_refresh))
            }
        }

        Button(
            onClick = {
                selected.clear()
                scope.launch {
                    withContext(Dispatchers.IO) { runCatching { acquisition.search(query, contentType, provider) } }
                }
            },
            enabled = auth.type == "authorizationStateReady" &&
                query.trim().length >= 2 && !state.searching && !state.acquiring && !hasPendingJob,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(tr(if (state.searching) R.string.acquisition_searching else R.string.acquisition_search))
        }

        if (state.searching || state.acquiring) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.progress.isNotBlank()) {
                Text(state.progress, style = MaterialTheme.typography.bodySmall)
            }
        }

        state.search?.let { search ->
            HorizontalDivider()
            Text(
                "${search.provider.wireValue.uppercase()} · ${search.contentType.wireValue.uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(search.query, style = MaterialTheme.typography.titleMedium)
            if (search.results.isEmpty()) {
                Text(tr(R.string.acquisition_no_results), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tr(R.string.acquisition_results, search.results.size),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    TextButton(
                        onClick = {
                            if (selected.size == search.results.size) selected.clear()
                            else {
                                selected.clear()
                                selected.addAll(search.results.map { it.number })
                            }
                        },
                    ) {
                        Text(tr(if (selected.size == search.results.size) R.string.acquisition_deselect_all else R.string.acquisition_select_all))
                    }
                }
                search.results.forEach { result ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = result.number in selected,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (result.number !in selected) selected.add(result.number)
                                } else selected.remove(result.number)
                            },
                            enabled = !state.acquiring && !hasPendingJob,
                        )
                        Text(
                            "${result.number}. ${result.title}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                withContext(Dispatchers.IO) { acquisition.acquire(search.id, selected.toList(), destinationId) }
                            }
                        }
                    },
                    enabled = selected.isNotEmpty() && destinationId != 0L && !state.acquiring && !hasPendingJob,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(tr(R.string.acquisition_start))
                }
            }
        }

        state.outcome?.let { outcome ->
            HorizontalDivider()
            Text(
                tr(R.string.acquisition_complete, outcome.imported, outcome.reused, outcome.failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (outcome.playlistId != null) {
                Text(
                    tr(R.string.acquisition_playlist_created, outcome.collectionTitle),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (outcome.note.isNotBlank()) {
                Text(outcome.note, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (state.jobs.isNotEmpty()) {
            HorizontalDivider()
            Text("Pedidos recentes", style = MaterialTheme.typography.titleSmall)
            state.jobs.take(5).forEach { job ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    ),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(job.query, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${job.provider.wireValue.uppercase()} · ${job.contentType.wireValue.uppercase()} · ${job.destinationTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Estado: ${job.status.wireValue}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (job.status == AcquisitionJobStatus.FAILED)
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                        job.items.forEach { item ->
                            Text(
                                "${item.optionNumber}. ${item.title} — ${item.status.wireValue}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (item.error.isNotBlank()) {
                                Text(
                                    item.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        if (job.error.isNotBlank()) {
                            Text(job.error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        if (state.error.isNotBlank()) {
            Text(
                state.error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun acquisitionTypeLabel(type: AcquisitionContentType): Int =
    when (type) {
        AcquisitionContentType.TRACK -> R.string.acquisition_track
        AcquisitionContentType.ALBUM -> R.string.acquisition_album
        AcquisitionContentType.PLAYLIST -> R.string.acquisition_playlist
        AcquisitionContentType.ARTIST -> R.string.acquisition_artist
    }
