package app.spotygram

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AcquisitionSheet(
    app: SpotygramApp,
    onClose: () -> Unit,
    initialQuery: String = "",
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
    var pickingDestination by remember { mutableStateOf(false) }
    var confirmAcquire by remember { mutableStateOf(false) }
    var selectedReview by remember(state.review?.id) {
        mutableStateOf(state.review?.tracks?.map { it.id }?.toSet() ?: emptySet())
    }
    val selected = remember { mutableStateListOf<Int>() }

    suspend fun loadDestinations() {
        val localMatches = runCatching { app.searchChats("GinjMusic") }.getOrDefault(emptyList())
        destinations = if (localMatches.isNotEmpty()) localMatches else
            runCatching { app.searchChats("GinjMusic", onServer = true) }.getOrDefault(emptyList())
        val savedKey = "acquisition_destination_${AppText.savedChatId}"
        val saved = app.prefs.getLong(savedKey, 0L)
        if (saved != 0L && destinations.none { it.id == saved }) {
            runCatching {
                    app.telegram.rememberChat(
                        app.telegram.request(json("getChat", "chat_id" to saved))
                    )
                }
                .getOrNull()
                ?.let { destinations = listOf(it) + destinations }
        }
        if (destinations.none { it.id == destinationId }) {
            // Never silently fall back to an arbitrary chat: an acquisition could
            // otherwise forward music to the wrong conversation.
            destinationId =
                destinations.firstOrNull { it.id == saved }?.id
                    ?: destinations.firstOrNull {
                        it.title.contains("GinjMusic", ignoreCase = true)
                    }?.id
                    ?: 0L
        }
    }

    LaunchedEffect(auth.type) {
        if (auth.type == "authorizationStateReady") loadDestinations()
    }
    LaunchedEffect(initialQuery, auth.type) {
        if (
            initialQuery.trim().length >= 2 &&
                auth.type == "authorizationStateReady" &&
                state.search?.query != initialQuery &&
                !state.searching
        ) {
            query = initialQuery
            withContext(Dispatchers.IO) {
                runCatching { acquisition.search(query, contentType, provider) }
            }
        }
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
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { query = "" },
                        enabled = !state.searching && !state.acquiring && !hasPendingJob,
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = tr(R.string.clear_search))
                    }
                }
            },
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
            OutlinedButton(
                onClick = { pickingDestination = true },
                enabled = !state.acquiring && !hasPendingJob,
            ) { Text(tr(R.string.acquisition_choose_destination)) }
            IconButton(
                onClick = { scope.launch { loadDestinations() } },
                enabled = !state.acquiring && !hasPendingJob,
            ) {
                Icon(Icons.Rounded.Refresh, tr(R.string.acquisition_refresh))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        Button(
            onClick = {
                selected.clear()
                scope.launch {
                    withContext(Dispatchers.IO) { runCatching { acquisition.search(query, contentType, provider) } }
                }
            },
            enabled = auth.type == "authorizationStateReady" &&
                query.trim().length >= 2 && !state.searching && !state.acquiring && !hasPendingJob,
            modifier = Modifier.weight(1f),
        ) {
            Text(tr(if (state.searching) R.string.acquisition_searching else R.string.acquisition_search))
        }
        if (state.searching) {
            OutlinedButton(
                onClick = { acquisition.cancelSearch() },
                modifier = Modifier.weight(1f),
            ) {
                Text(tr(R.string.cancel))
            }
        }
        }

        if (state.searching || state.acquiring) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.progress.isNotBlank()) {
                Text(state.progress, style = MaterialTheme.typography.bodySmall)
            }
        }

        state.search?.let { search ->
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${search.provider.wireValue.uppercase()} · ${search.contentType.wireValue.uppercase()}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(
                    onClick = {
                        selected.clear()
                        query = ""
                        acquisition.clearSearchResults()
                    },
                    enabled = !state.acquiring && !hasPendingJob,
                ) {
                    Text(tr(R.string.clear))
                }
            }
            Text(search.query, style = MaterialTheme.typography.titleMedium)
            if (search.fromHistory) {
                Text(
                    tr(R.string.acq_from_history),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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
                    onClick = { confirmAcquire = true },
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
            Text(tr(R.string.recent_requests), style = MaterialTheme.typography.titleSmall)
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
                            tr(R.string.acquisition_job_status, tr(acquisitionStatusLabel(job.status))),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (job.status == AcquisitionJobStatus.FAILED)
                                MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                        if (job.query.isNotBlank() && !state.searching && !state.acquiring) {
                            TextButton(onClick = {
                                query = job.query
                                contentType = job.contentType
                                provider = job.provider
                                destinationId = job.destinationChatId
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        runCatching {
                                            acquisition.search(query, contentType, provider)
                                        }
                                    }
                                }
                            }) { Text(tr(R.string.acquisition_retry_job)) }
                        }
                        job.items.forEach { item ->
                            Text(
                                "${item.optionNumber}. ${item.title} — ${tr(acquisitionStatusLabel(item.status))}",
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
    if (pickingDestination) {
        Dialog(onDismissRequest = { pickingDestination = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f).heightIn(max = 620.dp),
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tr(R.string.acquisition_destination),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        IconButton(onClick = { pickingDestination = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = tr(R.string.close))
                        }
                    }
                    Text(
                        tr(R.string.acquisition_destination_own_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ChatPicker(
                        app = app,
                        library = app.library.state.value,
                        onDone = { pickingDestination = false },
                        header = false,
                        serverSearch = false,
                        allowPublicSearch = false,
                        destinationOnly = true,
                        onChoice = { choice, _ ->
                            if (choice.title.startsWith("mlib-", ignoreCase = true)) {
                                app.action {
                                    app.notices.emit(
                                        tr(R.string.acquisition_destination_sync_channel)
                                    )
                                }
                            } else {
                                destinationId = choice.id
                                app.prefs.edit()
                                    .putLong(
                                        "acquisition_destination_${AppText.savedChatId}",
                                        choice.id,
                                    )
                                    .apply()
                            }
                            if (destinations.none { it.id == choice.id }) {
                                destinations = destinations + choice
                            }
                            pickingDestination = false
                        },
                    )
                }
            }
        }
    }
    state.review?.let { review ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text(tr(R.string.acq_review_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr(R.string.acq_review_help), style = MaterialTheme.typography.bodySmall)
                    Text(
                        tr(R.string.acq_review_note, review.tracks.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Column(
                        Modifier.heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        review.tracks.forEach { track ->
                            Row(
                                Modifier.fillMaxWidth().toggleable(
                                    value = track.id in selectedReview,
                                    onValueChange = { checked ->
                                        selectedReview =
                                            if (checked) selectedReview + track.id
                                            else selectedReview - track.id
                                    },
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = track.id in selectedReview,
                                    onCheckedChange = null,
                                )
                                Column(Modifier.padding(start = 8.dp)) {
                                    Text(
                                        track.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        if (track.imported) track.artist
                                        else "${track.artist} · ${tr(R.string.acq_review_existing)}",
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
            },
            confirmButton = {
                TextButton(
                    enabled = selectedReview.isNotEmpty(),
                    onClick = {
                        val selected = selectedReview.toSet()
                        app.action { app.acquisition.commitReview(selected) }
                    },
                ) { Text(tr(R.string.acq_review_commit)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    app.action { app.acquisition.cancelReview() }
                }) { Text(tr(R.string.acq_review_cancel)) }
            },
        )
    }
    if (confirmAcquire) {
        val destination = destinations.firstOrNull { it.id == destinationId }
        AlertDialog(
            onDismissRequest = { confirmAcquire = false },
            title = { Text(tr(R.string.acquisition_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        tr(
                            R.string.acquisition_confirm_message,
                            selected.size,
                            destination?.displayTitle ?: "",
                        )
                    )
                    if (selected.size >= 10) {
                        Text(
                            tr(R.string.acquisition_confirm_large, selected.size),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmAcquire = false
                    val chosen = selected.toList()
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                acquisition.acquire(
                                    state.search?.id ?: return@withContext,
                                    chosen,
                                    destinationId,
                                )
                            }
                        }
                    }
                }) { Text(tr(R.string.acquisition_confirm_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmAcquire = false }) {
                    Text(tr(R.string.cancel))
                }
            },
        )
    }
}

private fun acquisitionStatusLabel(status: AcquisitionJobStatus): Int =
    when (status) {
        AcquisitionJobStatus.QUEUED -> R.string.acquisition_status_queued
        AcquisitionJobStatus.RUNNING -> R.string.acquisition_status_running
        AcquisitionJobStatus.FORWARDING -> R.string.acquisition_status_forwarding
        AcquisitionJobStatus.COMPLETED -> R.string.acquisition_status_completed
        AcquisitionJobStatus.PARTIAL -> R.string.acquisition_status_partial
        AcquisitionJobStatus.FAILED -> R.string.acquisition_status_failed
        AcquisitionJobStatus.SKIPPED_DUPLICATE -> R.string.acquisition_status_skipped_duplicate
    }

private fun acquisitionTypeLabel(type: AcquisitionContentType): Int =
    when (type) {
        AcquisitionContentType.TRACK -> R.string.acquisition_track
        AcquisitionContentType.ALBUM -> R.string.acquisition_album
        AcquisitionContentType.PLAYLIST -> R.string.acquisition_playlist
        AcquisitionContentType.ARTIST -> R.string.acquisition_artist
    }
