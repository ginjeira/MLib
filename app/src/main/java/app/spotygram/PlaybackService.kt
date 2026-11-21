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
        private const val ALBUMS = "mlib:albums"
private const val ARTISTS = "mlib:artists"
        private const val FAVORITES = "mlib:favorites"
private const val OFFLINE = "mlib:offline"
private const val RECENT = "mlib:recent"
        private const val TRACK_PREFIX = "track:"
        private const val PLAYLIST_PREFIX = "playlist:"
        private const val ALBUM_PREFIX = "album:"
private const val ARTIST_PREFIX = "artist:"

        /**
         * The service must be exported for Android Auto and Automotive OS. These are the
         * system and projection controllers that are allowed to browse the private library.
         * Third-party apps can bind to an exported MediaLibraryService, so the default
         * accept-all Media3 connection behaviour is not sufficient.
         */
        private val TRUSTED_CONTROLLER_PACKAGES =
            setOf(
                "com.google.android.projection.gearhead",
                "com.google.android.gms.car",
                "com.google.android.apps.automotive",
                "com.android.car",
                "com.android.systemui",
                "com.google.android.systemui",
                "com.android.bluetooth",
                "com.google.android.bluetooth",
            )
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
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
            ): MediaSession.ConnectionResult {
                val allowed =
                    controller.isTrusted ||
                        controller.packageName == packageName ||
                        controller.packageName in TRUSTED_CONTROLLER_PACKAGES
                return if (allowed) {
                    MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()
                } else {
                    MediaSession.ConnectionResult.reject()
                }
            }

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
                        folder(ALL_TRACKS, tr(R.string.all_tracks)),
                        folder(PLAYLISTS, tr(R.string.playlists)),
                        folder(ALBUMS, tr(R.string.albums)),
                        folder(ARTISTS, tr(R.string.artists)),
                        folder(FAVORITES, tr(R.string.favorites)),
                        folder(OFFLINE, tr(R.string.offline_tracks)),
                        folder(RECENT, tr(R.string.recently_added)),
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
                    OFFLINE -> library.tracks
                        .asSequence()
                        .filter { it.local && !it.temporary }
                        .sortedWith(compareBy<Track> { it.title.lowercase() }.thenBy { it.artist.lowercase() })
                        .map(::trackItem)
                        .toList()
                    RECENT -> library.tracks
                        .asSequence()
                        .filter { it.local || it.available }
                        .sortedByDescending { it.date }
                        .take(100)
                        .map(::trackItem)
                        .toList()
                    PLAYLISTS -> library.playlists.map { playlist ->
                        folder("$PLAYLIST_PREFIX${playlist.id}", playlist.name)
                    }
                    ARTISTS -> library.tracks
                        .asSequence()
                        .filter { it.local || it.available }
                        .filter { it.artist.isNotBlank() }
                        .groupBy { it.artist }
                        .toList()
                        .sortedBy { it.first.lowercase() }
                        .map { (artist, _) -> folder("$ARTIST_PREFIX$artist", artist) }
                    ALBUMS -> library.albums.map { album ->
                        folder(
                            "$ALBUM_PREFIX${album.id}",
                            album.title,
                            artist = listOf(album.artist, album.year).filter(String::isNotBlank).joinToString(" · "),
                            artwork = album.coverUrl,
                        )
                    }
                    else -> when {
                        parentId.startsWith(PLAYLIST_PREFIX) -> {
                            val playlistId = parentId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
                            val playlist = playlistId?.let { id -> library.playlists.firstOrNull { it.id == id } }
                            playlist?.tracks.orEmpty().mapNotNull(library.byId::get)
                                .filter { it.local || it.available }
                                .map(::trackItem)
                        }
                        parentId.startsWith(ARTIST_PREFIX) -> {
                            val artist = parentId.removePrefix(ARTIST_PREFIX)
                            library.tracks
                                .asSequence()
                                .filter {
                                    it.artist.equals(artist, ignoreCase = true) &&
                                        (it.local || it.available)
                                }
                                .sortedWith(compareBy<Track> { it.title.lowercase() })
                                .map(::trackItem)
                                .toList()
                        }
                        parentId.startsWith(ALBUM_PREFIX) -> {
                            val albumId = parentId.removePrefix(ALBUM_PREFIX)
                            val album = library.albums.firstOrNull { it.id == albumId }
                            album?.tracks.orEmpty().mapNotNull(library.byId::get)
                                .filter { it.local || it.available }
                                .map(::trackItem)
                        }
                        else -> emptyList()
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
                val library = app.library.state.value
                val item = when {
                    mediaId == OFFLINE -> folder(OFFLINE, tr(R.string.offline_tracks))
                    mediaId == RECENT -> folder(RECENT, tr(R.string.recently_added))
                    mediaId.startsWith(TRACK_PREFIX) ->
                        library.byId[mediaId.removePrefix(TRACK_PREFIX)]?.let(::trackItem)
                    mediaId.startsWith(PLAYLIST_PREFIX) -> {
                        val id = mediaId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
                        library.playlists.firstOrNull { it.id == id }?.let {
                            folder(mediaId, it.name)
                        }
                    }
                    mediaId.startsWith(ARTIST_PREFIX) ->
                        folder(mediaId, mediaId.removePrefix(ARTIST_PREFIX))
                    mediaId.startsWith(ALBUM_PREFIX) -> {
                        val id = mediaId.removePrefix(ALBUM_PREFIX)
                        library.albums.firstOrNull { it.id == id }?.let {
                            folder(
                                mediaId,
                                it.title,
                                artist = listOf(it.artist, it.year).filter(String::isNotBlank).joinToString(" · "),
                                artwork = it.coverUrl,
                            )
                        }
                    }
                    else -> null
                }
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
                session.notifyChildrenChanged(ALBUMS, Int.MAX_VALUE, null)
                session.notifyChildrenChanged(FAVORITES, Int.MAX_VALUE, null)
                app.library.state.value.playlists.forEach {
                    session.notifyChildrenChanged("$PLAYLIST_PREFIX${it.id}", Int.MAX_VALUE, null)
                }
                app.library.state.value.albums.forEach {
                    session.notifyChildrenChanged("$ALBUM_PREFIX${it.id}", Int.MAX_VALUE, null)
                }
            }
        }
    }

    private fun folder(
        id: String,
        title: String,
        artist: String = "",
        artwork: String = "",
    ) = androidx.media3.common.MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setArtworkUri(artwork.takeIf { it.startsWith("https://") || it.startsWith("http://") }?.let(Uri::parse))
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
                    .setAlbumTitle(app.library.state.value.albums.firstOrNull { album -> track.id in album.tracks }?.title)
                    .setArtworkUri(track.art.takeIf { it.startsWith("https://") || it.startsWith("http://") }?.let(Uri::parse))
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
