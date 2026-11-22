package app.spotygram

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.app.Activity
import android.app.KeyguardManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val app
        get() = (application as AndroidApplication).model

    private val playerConnection by lazy { PlayerConnection(this, app) }
    private var unlocked = false
    private var unlockInProgress = false
    private val unlockLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            unlockInProgress = false
            if (result.resultCode == Activity.RESULT_OK) {
                unlocked = true
            } else {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) receiveShared(intent)
        setContent {
            val mode by app.theme.collectAsStateWithLifecycle()
            val dark = resolvedTheme(mode, isSystemInDarkTheme()) != "light"
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            val controller by playerConnection.controller.collectAsStateWithLifecycle()
            val waitingForPlayer by playerConnection.waiting.collectAsStateWithLifecycle()
            val picker =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenMultipleDocuments()
                ) { uris ->
                    app.action {
                        uris.forEach { app.importAudio(it) }
                        app.localMode()
                    }
                }
            val permission =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            var coverTarget by remember { mutableStateOf<((ByteArray?) -> Unit)?>(null) }
            val coverPicker =
                rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    val target = coverTarget
                    coverTarget = null
                    target?.invoke(
                        uri?.let {
                            runCatching {
                                contentResolver.openInputStream(it)?.use { stream ->
                                    stream.readBytes()
                                }
                            }.getOrNull()
                        }
                    )
                }
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            CompositionLocalProvider(
                LocalApp provides app,
                LocalConfiguration provides ViewConfiguration(configuration.screenWidthDp,
                    configuration.screenHeightDp, configuration.fontScale,
                    listOf(configuration.locales[0])),
            ) { SpotygramTheme(mode) {
                SpotygramUI(
                    app,
                    controller,
                    playerConnecting = waitingForPlayer,
                    onStartPlayback = { tracks, index, order ->
                        playerConnection.play(tracks, index, order)
                    },
                    onImport = { picker.launch(arrayOf("audio/*")) },
                    onPickCover = { callback ->
                        coverTarget = callback
                        coverPicker.launch("image/*")
                    },
                    onDownload = { ids ->
                        if (
                            Build.VERSION.SDK_INT >= 33 && ids.any { app.track(it)?.local == false }
                        )
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        app.action { app.download(ids) }
                    },
                )
            } }
        }
    }

    override fun onStart() {
        super.onStart()
        if (app.prefs.getBoolean("app_lock", false) && !unlocked && !unlockInProgress) {
            val manager = getSystemService(KeyguardManager::class.java)
            if (manager?.isDeviceSecure == true) {
                val intent =
                    manager.createConfirmDeviceCredentialIntent(
                        tr(R.string.app_lock_title),
                        tr(R.string.app_lock_prompt),
                    )
                if (intent != null) {
                    unlockInProgress = true
                    unlockLauncher.launch(intent)
                }
            }
        }
        app.headerSticker.visibility(true)
        playerConnection.start()
        // Automatic checks happen only when the app is opened, never in the background.
        app.updates.check()
    }

    override fun onStop() {
        unlocked = false
        app.headerSticker.visibility(false)
        app.playback?.cancelPendingStart()
        app.playback?.savePosition()
        playerConnection.stop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        (app.updates.installer as UpdateInstaller).resume(this)
    }

    override fun onPause() {
        (app.updates.installer as UpdateInstaller).pause(this)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveShared(intent)
    }

    private fun receiveShared(intent: Intent?) {
        if (intent?.action == "app.spotygram.SHOW_UPDATES") app.updates.showSettings.value = true
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("audio/") == true) {
            @Suppress("DEPRECATION")
            val uri = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
            uri?.let {
                app.localMode()
                app.action { app.importAudio(it) }
            }
        }
    }
}
