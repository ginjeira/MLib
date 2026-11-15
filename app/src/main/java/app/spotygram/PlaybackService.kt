package app.spotygram

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * Single playback engine exposed both to the MLib UI and to Android Auto.
 *
 * Android Auto browses the same SQLite catalogue and starts tracks through the
 * same PlaybackQueue used by the phone UI. It must never create a second player.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackService : MediaLibraryService() {
    private lateinit var session: MediaLibrarySession
    private lateinit var queue: PlaybackQueue
    private var libraryObserver: Job? = null
    private val app get() = (application as AndroidApplication).model

    companion object {
        private const val ROOT = "mlib:root"
        private const val ALL_TRACKS = "mlib:tracks"
        private const val PLAYLISTS = "mlib:playlists"
        private const val FAVORITES = "mlib:favorites"
        private const val TRACK_PREFIX = "track:"
        private const val PLAYLIST_PREFIX = "playlist:"
         private const val ALBUM_PREFIX = "album:"
    }

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this).apply {
            setSmallIcon(R.drawable.ic_notification)
        })

        val player = ExoPlayer.Builder(this)
            .setLoadControl(DefaultLoadControl.Builder()
                .setBufferDurationsMs(15000, 30000, 1000, 2000).build())
            .setMediaSourceFactory(DefaultMediaSourceFactory { TelegramDataSource(app) })
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
            .build()

        val callback = object : MediaLibrarySession.Callback {
            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?,
            ): ListenableFuture<LibraryResult<androidx.media3.common.MediaItem>> {
                val root = folder(ROOT, "MLib")
                return Futures.immediateFuture(LibraryResult.ofItem(root, params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?,
            ): ListenableFuture<LibraryResult<ImmutableList<androidx.media3.common.MediaItem>>> {
                val library = app.library.state.value
                val children = when (parentId) {
                    ROOT -> listOf(
                        folder(ALL_TRACKS, "Todas as músicas"),
                        folder(PLAYLISTS, "Playlists"),
                        folder(FAVORITES, "Favoritos"),
                    )
                    ALL_TRACKS -> library.tracks
                        .asSequence()
                        .filter { it.local || it.available }
                        .sortedWith(compareBy<Track> { it.title.lowercase() }.thenBy { it.artist.lowercase() })
                        .map(::trackItem)
                        .toList()
                    FAVORITES -> library.tracks
                        .asSequence()
                        .filter { it.liked && (it.local || it.available) }
                        .sortedWith(compareBy<Track> { it.title.lowercase() }.thenBy { it.artist.lowercase() })
                        .map(::trackItem)
                        .toList()
                    PLAYLISTS -> library.playlists.map { playlist ->
                        folder("$PLAYLIST_PREFIX${playlist.id}", playlist.name)
                    }
                    else -> {
                        val playlistId = parentId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
                        val playlist = playlistId?.let { id -> library.playlists.firstOrNull { it.id == id } }
                        playlist?.tracks.orEmpty().mapNotNull(library.byId::get)
                            .filter { it.local || it.available }
                            .map(::trackItem)
                    }
                }
                val start = (page * pageSize).coerceAtMost(children.size)
                val end = (start + pageSize).coerceAtMost(children.size)
                val pageItems = children.subList(start, end)
                return Futures.immediateFuture(
                    LibraryResult.ofItemList(ImmutableList.copyOf(pageItems), params)
                )
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String,
            ): ListenableFuture<LibraryResult<androidx.media3.common.MediaItem>> {
                val trackId = mediaId.removePrefix(TRACK_PREFIX)
                val track = app.library.state.value.byId[trackId]
                val item = track?.let(::trackItem)
                return Futures.immediateFuture(
                    if (item != null) LibraryResult.ofItem(item, null)
                    else LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                )
            }

            override fun onAddMediaItems(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: List<androidx.media3.common.MediaItem>,
            ): ListenableFuture<List<androidx.media3.common.MediaItem>> {
                val tracks = mediaItems.mapNotNull { item ->
                    app.library.state.value.byId[item.mediaId.removePrefix(TRACK_PREFIX)]
                }.filter { it.local || it.available }
                if (tracks.isNotEmpty()) {
                    // PlaybackQueue owns ordering, persistence, resume and listening history.
                    queue.start(tracks, 0)
                }
                // The queue has already populated the same ExoPlayer; avoid replacing its
                // managed timeline with the browser's transient request.
                return Futures.immediateFuture(emptyList())
            }
        }

        session = MediaLibrarySession.Builder(this, player, callback)
            .setId("MLibMediaLibrary")
            .setSessionActivity(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .build()
        queue = PlaybackQueue(app, AndroidPlayer(player))

        libraryObserver = app.scope.launch {
            app.library.state.collect {
                session.notifyChildrenChanged(ROOT, Int.MAX_VALUE, null)
                session.notifyChildrenChanged(ALL_TRACKS, Int.MAX_VALUE, null)
                session.notifyChildrenChanged(PLAYLISTS, Int.MAX_VALUE, null)
                session.notifyChildrenChanged(FAVORITES, Int.MAX_VALUE, null)
                app.library.state.value.playlists.forEach {
                    session.notifyChildrenChanged("$PLAYLIST_PREFIX${it.id}", Int.MAX_VALUE, null)
                }
            }
        }
    }

    private fun folder(id: String, title: String) =
        androidx.media3.common.MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private fun trackItem(track: Track) =
        androidx.media3.common.MediaItem.Builder()
            .setMediaId("$TRACK_PREFIX${track.id}")
            .setUri(Uri.parse("mlib://track/${Uri.encode(track.id)}"))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        libraryObserver?.cancel()
        session.release()
        queue.close()
        super.onDestroy()
    }
}
