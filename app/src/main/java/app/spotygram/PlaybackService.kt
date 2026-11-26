package app.spotygram

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.media3.common.C
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
    private var browseTracks: List<String> = emptyList()
    private val app get() = (application as AndroidApplication).model

    companion object {

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
                val root = toMediaItem(AutoNode(AutoLibrary.ROOT, "MLib"))
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
                val children =
                    AutoLibrary.children(parentId, app.library.state.value, labels())
                rememberBrowse(children)
                val start = (page * pageSize).coerceAtMost(children.size)
                val end = (start + pageSize).coerceAtMost(children.size)
                val pageItems = children.subList(start, end).map(::toMediaItem)
                return Futures.immediateFuture(
                    LibraryResult.ofItemList(ImmutableList.copyOf(pageItems), params)
                )
            }

            override fun onGetItem(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                mediaId: String,
            ): ListenableFuture<LibraryResult<androidx.media3.common.MediaItem>> {
                val node = AutoLibrary.item(mediaId, app.library.state.value, labels())
                return Futures.immediateFuture(
                    if (node != null) LibraryResult.ofItem(toMediaItem(node), null)
                    else LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                )
            }

            override fun onSearch(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                params: LibraryParams?,
            ): ListenableFuture<LibraryResult<Void>> {
                val count = AutoLibrary.search(query, app.library.state.value).size
                session.notifySearchResultChanged(browser, query, count, params)
                return Futures.immediateFuture(LibraryResult.ofVoid())
            }

            override fun onGetSearchResult(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                query: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?,
            ): ListenableFuture<LibraryResult<ImmutableList<androidx.media3.common.MediaItem>>> {
                val results = AutoLibrary.search(query, app.library.state.value)
                rememberBrowse(results)
                val start = (page * pageSize).coerceAtMost(results.size)
                val end = (start + pageSize).coerceAtMost(results.size)
                val pageItems = results.subList(start, end).map(::toMediaItem)
                return Futures.immediateFuture(
                    LibraryResult.ofItemList(ImmutableList.copyOf(pageItems), params)
                )
            }
            override fun onSetMediaItems(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: List<androidx.media3.common.MediaItem>,
                startIndex: Int,
                startPositionMs: Long,
            ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                val requested = playableTracks(mediaItems)
                val collection =
                    if (requested.isEmpty() && mediaItems.size == 1) {
                        collectionTracks(mediaItems[0])
                    } else {
                        emptyList()
                    }
                if (requested.isEmpty() && collection.isEmpty()) return emptySetMediaItems()
                val tracks: List<Track>
                val selected: Track
                val index: Int
                if (collection.isNotEmpty()) {
                    // Android Auto can ask to play a browsable collection as a whole.
                    tracks = collection
                    index = 0
                    selected = tracks[0]
                } else {
                    val requestedIndex = if (startIndex in requested.indices) startIndex else 0
                    selected = requested[requestedIndex]
                    // Android Auto selects one track from the list it is showing. Rebuild the
                    // queue from that visible list so the car gets previous/next and autoplay
                    // instead of a one-item timeline. Multi-item requests are left untouched.
                    val surrounding =
                        if (mediaItems.size == 1) {
                            browseQueueContaining(selected.id)
                        } else {
                            emptyList()
                        }
                    tracks = surrounding.ifEmpty { requested }
                    index = tracks.indexOfFirst { it.id == selected.id }.coerceAtLeast(0)
                }
                // PlaybackQueue owns ordering, persistence, resume and listening history.
                queue.start(tracks, index)
                // Media3 applies whatever this future returns with Player.setMediaItems(). An
                // empty list would clear the window PlaybackQueue just installed, which is why
                // "play" from Android Auto did nothing. Return that same window instead.
                if (player.currentMediaItem?.mediaId != selected.id) {
                    return emptySetMediaItems()
                }
                val window = (0 until player.mediaItemCount).map(player::getMediaItemAt)
                return Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(
                        window, player.currentMediaItemIndex, player.currentPosition,
                    )
                )
            }

            override fun onAddMediaItems(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: List<androidx.media3.common.MediaItem>,
            ): ListenableFuture<List<androidx.media3.common.MediaItem>> =
                Futures.immediateFuture(mediaItems.mapNotNull { playableMediaItem(it) })
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
                val state = app.library.state.value
                listOf(
                    AutoLibrary.ROOT,
                    AutoLibrary.HOME,
                    AutoLibrary.ALL_TRACKS,
                    AutoLibrary.PLAYLISTS,
                    AutoLibrary.ALBUMS,
                    AutoLibrary.ARTISTS,
                    AutoLibrary.FAVORITES,
                    AutoLibrary.OFFLINE,
                    AutoLibrary.RECENT,
                ).forEach { session.notifyChildrenChanged(it, Int.MAX_VALUE, null) }
                state.playlists.forEach {
                    session.notifyChildrenChanged(
                        "${AutoLibrary.PLAYLIST_PREFIX}${it.id}", Int.MAX_VALUE, null,
                    )
                }
                state.albums.forEach {
                    session.notifyChildrenChanged(
                        "${AutoLibrary.ALBUM_PREFIX}${it.id}", Int.MAX_VALUE, null,
                    )
                }
                state.tracks
                    .asSequence()
                    .map { it.artist }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(400)
                    .forEach {
                        session.notifyChildrenChanged(
                            "${AutoLibrary.ARTIST_PREFIX}$it", Int.MAX_VALUE, null,
                        )
                    }
            }
        }
    }

    private fun emptySetMediaItems() =
        Futures.immediateFuture(
            MediaSession.MediaItemsWithStartPosition(emptyList(), C.INDEX_UNSET, C.TIME_UNSET)
        )

    private fun playableTracks(
        items: List<androidx.media3.common.MediaItem>,
    ): List<Track> =
        items.mapNotNull { item ->
            app.library.state.value.byId[item.mediaId.removePrefix(AutoLibrary.TRACK_PREFIX)]
        }.filter { it.local || it.available }

    /** Expands a browsable collection (playlist, album, artist...) into its tracks. */
    private fun collectionTracks(
        item: androidx.media3.common.MediaItem,
    ): List<Track> {
        val state = app.library.state.value
        val node = AutoLibrary.item(item.mediaId, state, labels())
        if (node?.playableCollection != true) return emptyList()
        return AutoLibrary.children(item.mediaId, state, labels())
            .asSequence().filter { it.playable }
            .mapNotNull { state.byId[it.id.removePrefix(AutoLibrary.TRACK_PREFIX)] }
            .filter { it.local || it.available }
            .toList()
    }

    /** Remembers the ordered tracks of the list the car is browsing or searching. */
    private fun rememberBrowse(nodes: List<AutoNode>) {
        val ids =
            nodes.asSequence().filter { it.playable }
                .map { it.id.removePrefix(AutoLibrary.TRACK_PREFIX) }
                .toList()
        if (ids.isNotEmpty()) browseTracks = ids
    }

    /** Returns the browsed list around [id], or empty when that list cannot be rebuilt. */
    private fun browseQueueContaining(id: String): List<Track> {
        if (browseTracks.size < 2 || id !in browseTracks) return emptyList()
        val byId = app.library.state.value.byId
        return browseTracks.mapNotNull { byId[it] }.filter { it.local || it.available }
    }

    private fun playableMediaItem(
        item: androidx.media3.common.MediaItem,
    ): androidx.media3.common.MediaItem? {
        val id = item.mediaId.removePrefix(AutoLibrary.TRACK_PREFIX)
        val track = app.library.state.value.byId[id] ?: return null
        if (!track.local && !track.available) return null
        return item.buildUpon()
            .setUri(Uri.parse("spotygram://track/${Uri.encode(id)}"))
            .build()
    }

    private fun labels() =
        AutoLibrary.Labels(
            home = tr(R.string.auto_home),
            allTracks = tr(R.string.all_tracks),
            playlists = tr(R.string.playlists),
            albums = tr(R.string.albums),
            artists = tr(R.string.artists),
            favorites = tr(R.string.favorites),
            offline = tr(R.string.offline_tracks),
            recent = tr(R.string.recently_added),
        )

    private fun toMediaItem(node: AutoNode): androidx.media3.common.MediaItem {
        val metadata =
            MediaMetadata.Builder()
                .setTitle(node.title)
                .setArtist(node.artist.takeIf { it.isNotBlank() })
                .setAlbumTitle(node.album.takeIf { it.isNotBlank() })
                .setIsBrowsable(!node.playable)
                .setIsPlayable(node.playable || node.playableCollection)
        when {
            node.artworkData != null -> metadata.setArtworkData(node.artworkData)
            node.artworkUrl.isNotBlank() -> metadata.setArtworkUri(Uri.parse(node.artworkUrl))
        }
        val builder =
            androidx.media3.common.MediaItem.Builder()
                .setMediaId(node.id)
                .setMediaMetadata(metadata.build())
        if (node.playable) {
            val trackId = node.id.removePrefix(AutoLibrary.TRACK_PREFIX)
            builder.setUri(Uri.parse("mlib://track/${Uri.encode(trackId)}"))
        }
        return builder.build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onDestroy() {
        libraryObserver?.cancel()
        session.release()
        queue.close()
        super.onDestroy()
    }
}
