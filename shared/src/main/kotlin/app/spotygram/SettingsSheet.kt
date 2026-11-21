package app.spotygram

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun SettingsHeader(
    title: Int,
    onClose: () -> Unit,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null)
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, tr(R.string.back))
            }
        Text(
            tr(title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )
        actions()
        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, tr(R.string.close)) }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SettingsSheet(
    app: SpotygramApp,
    library: LibraryState,
    connected: Boolean,
    onConnect: () -> Unit,
    onLogout: () -> Unit,
    onImport: () -> Unit,
    onLocal: () -> Unit,
    onResume: () -> Unit,
    onCache: () -> Unit,
    onSharing: () -> Unit,
    onAcquisition: () -> Unit,
    onClose: () -> Unit,
) {
    val theme by app.theme.collectAsStateWithLifecycle()
    val hideDuplicates by app.hideDuplicates.collectAsStateWithLifecycle()
    var dedupDestinationOnly by remember {
        mutableStateOf(app.prefs.getBoolean("dedup_destination_only", false))
    }
    val resumeLongAudio by app.resumeLongAudio.collectAsStateWithLifecycle()
    val resumeMinutes by app.resumeMinutes.collectAsStateWithLifecycle()
    var editingResume by rememberSaveable { mutableStateOf(false) }
    var editingSyncKey by rememberSaveable { mutableStateOf(false) }
    var repairSync by rememberSaveable { mutableStateOf(false) }
    var confirmArchive by remember { mutableStateOf(false) }
    var sleepDialog by remember { mutableStateOf(false) }
    var recoveryCode by remember { mutableStateOf("") }
    var syncKeyInput by remember { mutableStateOf("") }
    var syncKeyError by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxWidth()) {
        SettingsHeader(R.string.settings, onClose)
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f, fill = false).navigationBarsPadding(),
            state =
                rememberLazyListState(
                    cacheWindow =
                        remember { LazyLayoutCacheWindow(aheadFraction = 1f, behindFraction = 1f) }
                ),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            item(key = "acquisition") {
                ListItem(
                    headlineContent = { Text(tr(R.string.music_acquisition)) },
                    supportingContent = { Text(tr(R.string.acquisition_help)) },
                    leadingContent = { Icon(Icons.Rounded.Search, null) },
                    trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                    modifier = Modifier.clickable(onClick = onAcquisition),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            item(key = "sleep") {
                ActionRow(Icons.Rounded.Bedtime, tr(R.string.sleep_timer)) {
                    sleepDialog = true
                }
            }
            item(key = "health") {
                val unavailable =
                    library.tracks.count { !it.available && !it.local && !it.imported }
                val duplicates = library.duplicates.groupCount()
                val noCover = library.tracks.count { it.art.isBlank() }
                Text(
                    tr(R.string.library_health),
                    Modifier.padding(top = 8.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (unavailable + duplicates + noCover == 0) {
                        tr(R.string.library_health_ok)
                    } else {
                        tr(R.string.library_health_summary, unavailable, duplicates, noCover)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ActionRow(Icons.Rounded.Build, tr(R.string.repair_library)) {
                    app.action {
                        val repaired = app.repairUnavailableLibrary()
                        app.notices.emit(
                            if (repaired == 0) tr(R.string.library_repair_none)
                            else tr(R.string.library_repaired, repaired)
                        )
                    }
                }
                ListItem(
                    headlineContent = { Text(tr(R.string.dedup_destination_only)) },
                    supportingContent = { Text(tr(R.string.dedup_destination_only_help)) },
                    trailingContent = {
                        GlassSwitch(
                            checked = dedupDestinationOnly,
                            onCheckedChange = {
                                dedupDestinationOnly = it
                                app.prefs.edit()
                                    .putBoolean("dedup_destination_only", it)
                                    .apply()
                            },
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            item(key = "appearance") {
                Text(
                    tr(R.string.appearance),
                    Modifier.padding(top = 8.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                val selectedTheme =
                    if (theme == "system") {
                        if (isSystemInDarkTheme()) "dark" else "light"
                    } else theme
                Row(Modifier.fillMaxWidth().liquidGlass(22.dp).padding(4.dp)) {
                    listOf("light" to tr(R.string.light_theme), "dark" to tr(R.string.dark_theme))
                        .forEach { (key, label) ->
                            Box(
                                Modifier.weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .then(
                                        if (selectedTheme == key)
                                            Modifier.liquidGlass(24.dp, tint = spectrumAccent(0))
                                        else Modifier
                                    )
                                    .clickable { app.changeTheme(key) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    label,
                                    color =
                                        if (selectedTheme == key) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                }
                ActionRow(Icons.Rounded.Language, tr(R.string.language)) {
                    app.platform.languageSettings(app)
                }
                Text(
                    tr(R.string.language_help),
                    Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(
                    Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            item(key = "sharing") {
                ListItem(
                    headlineContent = { Text(tr(R.string.share_settings)) },
                    supportingContent = { Text(tr(R.string.share_settings_help)) },
                    leadingContent = { Icon(Icons.Rounded.IosShare, null) },
                    trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                    modifier = Modifier.clickable(onClick = onSharing),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            item(key = "positions") {
                Row(
                    Modifier.fillMaxWidth()
                        .toggleable(
                            value = resumeLongAudio,
                            role = Role.Switch,
                            onValueChange = app::changeResumeLongAudio,
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(tr(R.string.resume_long_audio))
                        Text(
                            tr(R.string.resume_long_audio_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    GlassSwitch(checked = resumeLongAudio, onCheckedChange = null)
                }
                ListItem(
                    headlineContent = { Text(tr(R.string.resume_threshold)) },
                    supportingContent = {
                        Text(tr(R.string.resume_threshold_minutes, resumeMinutes))
                    },
                    leadingContent = { Icon(Icons.Rounded.History, null) },
                    trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                    modifier = Modifier.clickable { editingResume = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                HorizontalDivider(
                    Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            item(key = "duplicates") {
                Row(
                    Modifier.fillMaxWidth()
                        .toggleable(
                            value = hideDuplicates,
                            role = Role.Switch,
                            onValueChange = app::changeHideDuplicates,
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(tr(R.string.hide_duplicates))
                        Text(
                            tr(R.string.hide_duplicates_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    GlassSwitch(checked = hideDuplicates, onCheckedChange = null)
                }
            }
            item(key = "local") {
                val localCount =
                    if (hideDuplicates) library.localRecordingCount else library.localTracks.size
                ListItem(
                    headlineContent = { Text(tr(R.string.on_device)) },
                    supportingContent = {
                        Text("${trackCount(localCount)} · ${bytes(library.localSize)}")
                    },
                    leadingContent = { Icon(Icons.Rounded.DownloadForOffline, null) },
                    trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                    modifier = Modifier.clickable(onClick = onLocal),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                Text(
                    tr(R.string.local_storage_help),
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item(key = "listening-cache") {
                val listening by app.listeningCache.state.collectAsStateWithLifecycle()
                Row(
                    Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(tr(R.string.discard_listened))
                        Text(
                            tr(R.string.discard_listened_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    GlassSwitch(
                        checked = listening.enabled,
                        onCheckedChange = { app.listeningCache.setEnabled(it) },
                        enabled = !listening.changing,
                    )
                }
                Text(
                    tr(R.string.discard_listened_limits),
                    Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (listening.changing) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (listening.deferred) {
                    Text(
                        tr(R.string.temporary_cleanup_deferred),
                        Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { app.listeningCache.retry() }) {
                        Text(tr(R.string.retry_temporary_cleanup))
                    }
                }
                ListItem(
                    headlineContent = { Text(tr(R.string.clear_music_cache)) },
                    supportingContent = { Text(tr(R.string.cache_settings_help)) },
                    leadingContent = { Icon(Icons.Rounded.DeleteSweep, null) },
                    trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                    modifier = Modifier.clickable(onClick = onCache),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            item(key = "downloads") {
                val syncState by app.cloudSync.state.collectAsStateWithLifecycle()
                var wifi by
                    remember(syncState.lastSync) {
                        mutableStateOf(app.prefs.getBoolean("wifi_only", false))
                    }
                val download by app.downloading.collectAsStateWithLifecycle()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(tr(R.string.wifi_only))
                        Text(
                            tr(R.string.wifi_only_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    GlassSwitch(
                        wifi,
                        {
                            wifi = it
                            app.changeSetting("wifi_only", it)
                        },
                    )
                }
                if (download.queued > 0)
                    Text(
                        tr(
                            R.string.download_progress,
                            download.queued,
                            (download.progress * 100).toInt(),
                        ),
                        Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                ActionRow(Icons.Rounded.Download, tr(R.string.resume_downloads), onClick = onResume)
                if (download.queued > 0)
                    ActionRow(Icons.Rounded.Close, tr(R.string.stop_downloads)) {
                        app.platform.stopDownloads(app)
                    }
                ActionRow(Icons.Rounded.Add, tr(R.string.import_files), onClick = onImport)
                ActionRow(Icons.Rounded.Forum, tr(R.string.music_bot_setup_action)) {
                    app.action {
                        val activated = app.acquisition.activateBot()
                        app.notices.emit(
                            tr(
                                if (activated) R.string.music_bot_activated
                                else R.string.music_bot_already_used
                            )
                        )
                    }
                }
                ActionRow(Icons.Rounded.CreateNewFolder, tr(R.string.create_archive_channel)) {
                    confirmArchive = true
                }

                HorizontalDivider(
                    Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            item(key = "account") {
                val syncState by app.cloudSync.state.collectAsStateWithLifecycle()
                ActionRow(
                    Icons.Rounded.Forum,
                    if (connected) tr(R.string.logout_telegram) else tr(R.string.connect_telegram),
                    onClick = if (connected) onLogout else onConnect,
                )
                if (connected) {
                    ListItem(
                        headlineContent = { Text(tr(R.string.sync_title)) },
                        supportingContent = {
                            val status =
                                tr(
                                    when (syncState.phase) {
                                        "ready" -> R.string.sync_ready
                                        "syncing" -> R.string.sync_working
                                        "error" -> R.string.sync_paused
                                        else -> R.string.sync_offline
                                    }
                                )
                            Text(
                                when {
                                    syncState.phase == "key_required" -> tr(R.string.sync_suspended_key_required)
                                    syncState.phase == "error" && syncState.error.isNotBlank() -> "$status\n${syncState.error}"
                                    else -> status
                                }
                            )
                        },
                        leadingContent = {
                            Icon(
                                if (syncState.phase == "ready") Icons.Rounded.CloudDone
                                else Icons.Rounded.CloudSync,
                                null,
                            )
                        },
                        trailingContent =
                            if (syncState.phase == "error") {
                                {
                                    IconButton(onClick = { app.cloudSync.retry() }) {
                                        Icon(Icons.Rounded.Refresh, tr(R.string.retry_chat_loading))
                                    }
                                }
                            } else null,
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    if (syncState.phase == "error") {
                        ActionRow(
                            Icons.Rounded.Build,
                            tr(R.string.sync_repair),
                            onClick = { repairSync = true },
                        )
                    }
                    ActionRow(
                        Icons.Rounded.Key,
                        tr(R.string.sync_key_settings),
                        onClick = {
                            recoveryCode = app.cloudSync.recoveryCode().orEmpty()
                            syncKeyInput = ""
                            syncKeyError = ""
                            editingSyncKey = true
                        },
                    )
                }
            }
            item(key = "updates") {
                UpdateSettings(app)
            }
            item(key = "about") {
                Text(
                    "MLib ${BuildConfig.VERSION_NAME}",
                    Modifier.padding(top = 20.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    tr(R.string.app_description),
                    Modifier.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
    if (sleepDialog) {
        AlertDialog(
            onDismissRequest = { sleepDialog = false },
            title = { Text(tr(R.string.sleep_timer)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(15, 30, 60, 90).forEach { minutes ->
                        TextButton(onClick = {
                            sleepDialog = false
                            app.playback?.setSleepTimer(minutes)
                            app.action {
                                app.notices.emit(tr(R.string.sleep_timer_set, minutes))
                            }
                        }) { Text(tr(R.string.sleep_timer_minutes, minutes)) }
                    }
                    TextButton(onClick = {
                        sleepDialog = false
                        app.playback?.cancelSleepTimer()
                    }) { Text(tr(R.string.sleep_timer_cancel)) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { sleepDialog = false }) { Text(tr(R.string.cancel)) }
            },
        )
    }
    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text(tr(R.string.create_archive_channel)) },
            text = { Text(tr(R.string.create_archive_help)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmArchive = false
                    app.action {
                        val channel = app.createArchiveChannel()
                        app.notices.emit(tr(R.string.archive_channel_created, channel.displayTitle))
                    }
                }) { Text(tr(R.string.create)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }) { Text(tr(R.string.cancel)) }
            },
        )
    }
    if (repairSync) {
        AlertDialog(
            onDismissRequest = { repairSync = false },
            title = { Text(tr(R.string.sync_repair_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr(R.string.sync_repair_help))
                    TextButton(onClick = {
                        repairSync = false
                        app.action { app.cloudSync.repairStorage(clearKey = true) }
                    }) { Text(tr(R.string.sync_repair_forget_key)) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    repairSync = false
                    app.action { app.cloudSync.repairStorage() }
                }) { Text(tr(R.string.sync_repair_action)) }
            },
            dismissButton = {
                TextButton(onClick = { repairSync = false }) { Text(tr(R.string.cancel)) }
            },
        )
    }
    if (editingSyncKey) {
        AlertDialog(
            onDismissRequest = { editingSyncKey = false },
            title = { Text(tr(R.string.sync_key_settings)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(tr(R.string.sync_key_help))
                    if (recoveryCode.isNotBlank()) {
                        OutlinedTextField(
                            value = recoveryCode,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(tr(R.string.sync_recovery_device)) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(onClick = { clipboard.setText(AnnotatedString(recoveryCode)) }) {
                            Text(tr(R.string.copy_code))
                        }
                    } else {
                        Text(tr(R.string.sync_no_key_yet))
                        TextButton(
                            onClick = {
                                runCatching { app.cloudSync.createRecoveryCode() }
                                    .onSuccess { recoveryCode = it; syncKeyError = "" }
                                    .onFailure { syncKeyError = it.message.orEmpty() }
                            },
                        ) { Text(tr(R.string.sync_create_first_key)) }
                    }
                    OutlinedTextField(
                        value = syncKeyInput,
                        onValueChange = { syncKeyInput = it; syncKeyError = "" },
                        label = { Text(tr(R.string.sync_import_device)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (syncKeyError.isNotBlank())
                        Text(syncKeyError, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = syncKeyInput.isNotBlank(),
                    onClick = {
                        runCatching { app.cloudSync.importRecoveryCode(syncKeyInput) }
                            .onSuccess {
                                recoveryCode = app.cloudSync.recoveryCode().orEmpty()
                                syncKeyInput = ""
                                syncKeyError = ""
                            }
                            .onFailure { syncKeyError = it.message.orEmpty() }
                    },
                ) { Text(tr(R.string.sync_import_key)) }
            },
            dismissButton = {
                TextButton(onClick = { editingSyncKey = false }) { Text(tr(R.string.close)) }
            },
        )
    }
    if (editingResume) {
        var value by rememberSaveable { mutableStateOf(resumeMinutes.toString()) }
        val minutes = value.toIntOrNull()?.takeIf { it > 0 }
        val apply = {
            if (minutes != null) {
                app.changeResumeMinutes(minutes)
                editingResume = false
            }
        }
        GlassDialog(
            onDismissRequest = { editingResume = false },
            title = { Text(tr(R.string.resume_threshold)) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(tr(R.string.resume_threshold_help))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { input ->
                            if (input.length <= 10 && input.all { it in '0'..'9' }) value = input
                        },
                        label = { Text(tr(R.string.resume_minutes_label)) },
                        singleLine = true,
                        isError = minutes == null,
                        supportingText =
                            if (minutes == null) {
                                { Text(tr(R.string.resume_minutes_error)) }
                            } else null,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                        keyboardActions = KeyboardActions(onDone = { apply() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = apply, enabled = minutes != null) { Text(tr(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { editingResume = false }) { Text(tr(R.string.cancel)) }
            },
        )
    }
    val choosingLanguage by app.languagePicker.collectAsStateWithLifecycle()
    if (choosingLanguage) {
        GlassDialog(
            onDismissRequest = { app.languagePicker.value = false },
            title = { Text(tr(R.string.language)) },
            text = {
                Column {
                    listOf(
                            "system" to R.string.system_default,
                            "en" to R.string.language_english,
                            "pt" to R.string.language_portuguese,
                        )
                        .forEach { (value, label) ->
                            TextButton(
                                onClick = {
                                    app.changeLanguage(value)
                                    app.languagePicker.value = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(tr(label))
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { app.languagePicker.value = false }) {
                    Text(tr(R.string.cancel))
                }
            },
        )
    }
}
