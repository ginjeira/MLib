package app.spotygram

import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

enum class AcquisitionContentType(val wireValue: String) {
    TRACK("track"),
    ALBUM("album"),
    PLAYLIST("playlist"),
    ARTIST("artist"),
}

enum class AcquisitionProvider(val wireValue: String) {
    DEEZER("deezer"),
    SOUNDCLOUD("soundcloud"),
}

data class AcquisitionOption(val number: Int, val title: String)

data class AcquisitionSearch(
    val id: String,
    val query: String,
    val contentType: AcquisitionContentType,
    val provider: AcquisitionProvider,
    val results: List<AcquisitionOption>,
    val expiresAt: Long,
)

data class AcquisitionOutcome(
    val imported: Int,
    val reused: Int,
    val failed: Int,
    val playlistId: Long? = null,
    val collectionTitle: String = "",
    val note: String = "",
)

enum class AcquisitionJobStatus(val wireValue: String) {
    QUEUED("queued"),
    RUNNING("running"),
    FORWARDING("forwarding"),
    COMPLETED("completed"),
    PARTIAL("partial"),
    FAILED("failed"),
    SKIPPED_DUPLICATE("skipped_duplicate"),
}

data class AcquisitionJobItem(
    val optionNumber: Int,
    val title: String,
    val status: AcquisitionJobStatus = AcquisitionJobStatus.QUEUED,
    val trackIds: List<String> = emptyList(),
    val error: String = "",
)

data class AcquisitionJob(
    val id: String,
    val query: String,
    val contentType: AcquisitionContentType,
    val provider: AcquisitionProvider,
    val destinationChatId: Long,
    val destinationTitle: String,
    val status: AcquisitionJobStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val items: List<AcquisitionJobItem>,
    val error: String = "",
)

data class PendingAcquisitionJob(
    val query: String,
    val contentType: AcquisitionContentType,
    val provider: AcquisitionProvider,
    val selectedTitles: List<String>,
    val destinationChatId: Long,
    val telegramUserId: Long,
    val targetAlbumId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val awaitingBotCompletion: Boolean = false,
    val botChatId: Long = 0,
    val botMessageCursor: Long = 0,
)

data class AcquisitionState(
    val searching: Boolean = false,
    val acquiring: Boolean = false,
    val progress: String = "",
    val error: String = "",
    val search: AcquisitionSearch? = null,
    val outcome: AcquisitionOutcome? = null,
    val pendingJob: PendingAcquisitionJob? = null,
    val jobs: List<AcquisitionJob> = emptyList(),
)

private data class SearchSession(
    val search: AcquisitionSearch,
    val botChatId: Long,
    val resultMessageId: Long,
    val targetAlbumId: String? = null,
)

private data class AcquiredItem(
    val option: AcquisitionOption,
    val tracks: List<Track>,
    val importedIds: Set<String> = emptySet(),
    val error: String = "",
    val skippedDuplicate: Boolean = false,
    val partial: Boolean = false,
    val botFinished: Boolean = true,
    val botChatId: Long = 0,
    val botMessageCursor: Long = 0,
)

private data class AudioCollection(
    val messages: List<JSONObject>,
    val completionReason: AcquisitionCompletionReason,
    val botFinished: Boolean,
    val lastObservedMessageId: Long,
) {
    val partial: Boolean
        get() = completionReason == AcquisitionCompletionReason.DEADLINE_PARTIAL
}

/**
 * Native MLib implementation of the Telegram bot workflow used by MCat Player.
 *
 * It deliberately uses the already-authenticated TDLib client. No MCat HTTP API,
 * MCat server session, GramJS client, or second Telegram login is involved.
 */
class MusicAcquisition(private val app: SpotygramApp) {
    val state = MutableStateFlow(AcquisitionState())

    // A single lane is intentional: the bot conversation is stateful and its
    // messages/callbacks must not be interleaved by simultaneous MLib requests.
    private val botLane = Mutex()
    private val resumeLane = Mutex()
    private val searches = linkedMapOf<String, SearchSession>()
    private var activeSearchId: String? = null
    private val pendingJobKey = "mlib_pending_music_acquisition"

    init {
        state.value = state.value.copy(pendingJob = readPendingJob())
    }

    suspend fun search(
        rawQuery: String,
        contentType: AcquisitionContentType,
        provider: AcquisitionProvider,
        targetAlbumId: String? = null,
        allowPendingJob: Boolean = false,
    ): AcquisitionSearch = botLane.withLock {
        checkTelegramReady()
        check(activeSearchId == null) {
            "O bot ainda está a aguardar a conclusão da aquisição anterior. Termina-a antes de iniciar outra pesquisa."
        }
        check(state.value.pendingJob == null || allowPendingJob) {
            "Existe uma aquisição interrompida. Retoma-a ou descarta-a antes de iniciar outra."
        }
        val query = sanitizeSearchQuery(rawQuery)
        require(query.length >= 2) { "Introduz pelo menos dois caracteres para pesquisar." }

        state.value = state.value.copy(searching = true, acquiring = false, progress = "", error = "", search = null, outcome = null)
        try {
            val bot = resolveBot()
            val sent = sendText(bot.getLong("id"), query)
            state.value = state.value.copy(progress = "A aguardar resposta do bot…")

            val botChatId = bot.getLong("id")
            // Match the MCat Player protocol: the bot first acknowledges the
            // query with "Searching for", then the same inline message is used
            // to select content type and provider. Do not invent a separate
            // Track -> Playlist -> resend-query sequence.
            var resultMessage = waitForBotMessage(botChatId, sent.getLong("id"), 8_000) {
                messageText(it).contains("Searching for")
            } ?: error("O bot do Telegram não respondeu a tempo à pesquisa.")

            val typeMarker = "in '${contentType.wireValue}'"
            if (!messageText(resultMessage).contains(typeMarker)) {
                state.value = state.value.copy(progress = "A seleccionar o tipo de conteúdo…")
                if (clickButton(resultMessage, contentType.wireValue)) {
                    resultMessage = waitForBotUpdate(
                        botChatId, resultMessage.getLong("id"), 4_800,
                    ) { messageText(it).contains(typeMarker) } ?: resultMessage
                }
            }

            val providerMarker = "from '${provider.wireValue}'"
            if (!messageText(resultMessage).contains(providerMarker)) {
                state.value = state.value.copy(progress = "A seleccionar o fornecedor…")
                if (clickButton(resultMessage, provider.wireValue)) {
                    resultMessage = waitForBotUpdate(
                        botChatId, resultMessage.getLong("id"), 4_800,
                    ) { messageText(it).contains(providerMarker) } ?: resultMessage
                }
            }

            var effectiveProvider = provider
            var options = parseResults(messageText(resultMessage))
            // The dedicated MCat album-download flow falls back to SoundCloud;
            // ordinary album searches do not.
            if (options.isEmpty() && targetAlbumId != null &&
                contentType == AcquisitionContentType.ALBUM &&
                provider == AcquisitionProvider.DEEZER
            ) {
                state.value = state.value.copy(progress = "Sem álbuns no Deezer; a tentar SoundCloud…")
                if (clickButton(resultMessage, AcquisitionProvider.SOUNDCLOUD.wireValue)) {
                    resultMessage = waitForBotUpdate(
                        botChatId, resultMessage.getLong("id"), 3_600,
                    ) { messageText(it).contains("from 'soundcloud'") } ?: resultMessage
                    if (messageText(resultMessage).contains("from 'soundcloud'")) {
                        effectiveProvider = AcquisitionProvider.SOUNDCLOUD
                        options = parseResults(messageText(resultMessage))
                    }
                }
            }
            val search = AcquisitionSearch(
                id = UUID.randomUUID().toString(),
                query = query,
                contentType = contentType,
                provider = effectiveProvider,
                results = options,
                expiresAt = System.currentTimeMillis() + 20 * 60 * 1000L,
            )
            searches[search.id] = SearchSession(
                search, bot.getLong("id"), resultMessage.getLong("id"), targetAlbumId,
            )
            if (options.isNotEmpty()) activeSearchId = search.id
            while (searches.size > 12) {
                val oldest = searches.keys.first()
                searches.remove(oldest)
                if (activeSearchId == oldest) activeSearchId = null
            }
            state.value = state.value.copy(
                searching = false,
                progress = "",
                search = search,
                error = if (options.isEmpty()) "O bot não devolveu resultados para esta pesquisa." else "",
            )
            search
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            state.value = state.value.copy(searching = false, progress = "", error = friendly(error))
            throw error
        }
    }

    suspend fun acquire(
        searchId: String,
        selectedNumbers: List<Int>,
        destinationChatId: Long,
    ): AcquisitionOutcome = botLane.withLock {
        checkTelegramReady()
        check(activeSearchId == null || activeSearchId == searchId) {
            "O bot ainda está ocupado com outra pesquisa/aquisição."
        }
        require(destinationChatId != 0L) { "Selecciona um destino Telegram válido." }
        val session = searches[searchId] ?: error("A pesquisa expirou. Faz uma nova pesquisa.")
        require(System.currentTimeMillis() < session.search.expiresAt) {
            searches.remove(searchId)
            "A pesquisa expirou. Faz uma nova pesquisa."
        }
        val selected = selectedNumbers.distinct().map { number ->
            session.search.results.firstOrNull { it.number == number }
                ?: error("A opção $number já não pertence a esta pesquisa.")
        }
        require(selected.isNotEmpty()) { "Selecciona pelo menos um resultado." }
        require(session.targetAlbumId == null || selected.size == 1) {
            "Para completar um álbum existente, selecciona apenas uma edição do bot de cada vez."
        }
        val telegramUserId = request(json("getMe")).getLong("id")
        val currentPending = state.value.pendingJob
        require(currentPending == null ||
            (currentPending.query == session.search.query &&
                currentPending.contentType == session.search.contentType &&
                currentPending.provider == session.search.provider &&
                currentPending.selectedTitles.size == selected.size &&
                currentPending.selectedTitles.zip(selected).all { (saved, current) ->
                    MusicAcquisitionRules.cleanCollectionTitle(saved).equals(
                        MusicAcquisitionRules.cleanCollectionTitle(current.title),
                        ignoreCase = true,
                    )
                } &&
                currentPending.destinationChatId == destinationChatId &&
                currentPending.telegramUserId == telegramUserId &&
                currentPending.targetAlbumId == session.targetAlbumId)
        ) {
            "Existe outra aquisição interrompida. Retoma-a ou descarta-a antes de iniciar esta."
        }
        val pending = PendingAcquisitionJob(
            query = session.search.query,
            contentType = session.search.contentType,
            provider = session.search.provider,
            selectedTitles = selected.map { it.title },
            destinationChatId = destinationChatId,
            telegramUserId = telegramUserId,
            targetAlbumId = session.targetAlbumId,
        )
        persistPendingJob(pending)

        state.value = state.value.copy(acquiring = true, searching = false, progress = "", error = "", outcome = null)
        try {
            val destination = request(json("getChat", "chat_id" to destinationChatId))
            val destinationTitle = destination.optString("title").ifBlank { "Telegram" }
            val jobId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val job = AcquisitionJob(
                id = jobId,
                query = session.search.query,
                contentType = session.search.contentType,
                provider = session.search.provider,
                destinationChatId = destinationChatId,
                destinationTitle = destinationTitle,
                status = AcquisitionJobStatus.RUNNING,
                createdAt = now,
                updatedAt = now,
                items = selected.map { AcquisitionJobItem(it.number, it.title) },
            )
            state.value = state.value.copy(jobs = (listOf(job) + state.value.jobs).take(30))
            val destinationChoice = app.telegram.rememberChat(destination)

            val completed = mutableListOf<AcquiredItem>()
            val retryTitles = mutableListOf<String>()
            var botTurnFinished = true
            var resumeBotChatId = 0L
            var resumeBotCursor = 0L
            for ((index, option) in selected.withIndex()) {
                state.value = state.value.copy(
                    progress = "A obter opção ${index + 1}/${selected.size}: ${option.title}",
                )
                updateJobItem(jobId, index) { it.copy(status = AcquisitionJobStatus.FORWARDING) }
                val item = try {
                    acquireOption(session, option, destinationChatId, destinationTitle)
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    val cursor = state.value.pendingJob?.botMessageCursor
                        ?.takeIf { it > 0L }
                        ?: runCatching { latestMessageId(session.botChatId) }.getOrDefault(0L)
                    AcquiredItem(
                        option, emptyList(), error = friendly(error), botFinished = false,
                        botChatId = session.botChatId, botMessageCursor = cursor,
                    )
                }
                completed += item
                updateJobItem(jobId, index) {
                    it.copy(
                        status = when {
                            item.partial || !item.botFinished -> AcquisitionJobStatus.PARTIAL
                            item.skippedDuplicate -> AcquisitionJobStatus.SKIPPED_DUPLICATE
                            item.error.isNotBlank() -> AcquisitionJobStatus.FAILED
                            else -> AcquisitionJobStatus.COMPLETED
                        },
                        trackIds = item.tracks.map { track -> track.id },
                        error = item.error,
                    )
                }
                if (item.error.isNotBlank()) retryTitles += option.title
                if (!item.botFinished) {
                    botTurnFinished = false
                    resumeBotChatId = item.botChatId.takeIf { it != 0L } ?: session.botChatId
                    resumeBotCursor = item.botMessageCursor
                    retryTitles.clear()
                    retryTitles += selected.drop(index).map { it.title }
                    break
                }
                delay(1_000)
            }

            // MCat adds the destination to the active sources and refreshes
            // its messages after processing the job, not before selecting bot results.
            app.library.source(destinationChoice, true)

            val allTrackIds = completed.flatMap { it.tracks }.map { it.id }.distinct()
            val importedIds = completed.flatMap { it.importedIds }.toSet()
            val importedCount = importedIds.size
            val reusedCount = (allTrackIds.size - importedIds.size).coerceAtLeast(0)
            val failedCount = completed.count { it.error.isNotEmpty() }
            var playlistId: Long? = null
            val acquiredAlbums = mutableListOf<String>()
            if (session.search.contentType == AcquisitionContentType.ALBUM) {
                for (item in completed.filter { it.tracks.isNotEmpty() && !it.skippedDuplicate }) {
                    val first = item.tracks.first()
                    val targetAlbum = session.targetAlbumId?.let { id ->
                        app.library.state.value.albums.firstOrNull { it.id == id }
                    }
                    val parsedName = MusicAcquisitionRules.parseArtistAndTitle(item.option.title)
                    val title = targetAlbum?.title
                        ?: parsedName.title.ifBlank { MusicAcquisitionRules.cleanCollectionTitle(item.option.title) }
                    val artist = targetAlbum?.artist?.takeIf {
                        it.isNotBlank() && !it.equals("Artista Desconhecido", ignoreCase = true)
                    } ?: parsedName.artist.ifBlank { first.artist }
                    // MCat first checks the explicitly selected album, then its
                    // own areAlbumsDuplicate() rules before creating a new one.
                    val existingAlbum = targetAlbum ?: app.library.state.value.albums.firstOrNull {
                        MusicAcquisitionRules.areAlbumsDuplicate(it.title, it.artist, title, artist)
                    }
                    val existingCover = existingAlbum?.coverUrl.orEmpty()
                    val trackCover = first.art
                    val cover = when {
                        existingCover.isNotBlank() && !MusicAcquisitionRules.isMCatJobGenericAlbumCover(existingCover) -> existingCover
                        trackCover.isNotBlank() && !MusicAcquisitionRules.isMCatJobGenericAlbumCover(trackCover) -> trackCover
                        else -> MusicAcquisitionRules.genreCoverPreset("$title $artist")
                    }
                    val albumId = app.library.createOrUpdateAlbum(
                        title = existingAlbum?.title ?: title,
                        artist = existingAlbum?.artist?.takeIf(String::isNotBlank) ?: artist,
                        tracks = item.tracks.map { it.id },
                        year = existingAlbum?.year ?: java.time.Year.now().value.toString(),
                        coverUrl = cover,
                        genre = existingAlbum?.genre.orEmpty(),
                        targetAlbumId = existingAlbum?.id,
                    )
                    acquiredAlbums += albumId
                }
            }
            if (session.search.contentType == AcquisitionContentType.PLAYLIST) {
                // A playlist is a collection in its own right: reuse existing tracks,
                // but never suppress the playlist merely because every track
                // was already present in the library.
                for (item in completed.filter { it.tracks.isNotEmpty() }) {
                    val ids = item.tracks.map { it.id }.distinct()
                    val playlistName = MusicAcquisitionRules.cleanCollectionTitle(item.option.title)
                    val existing = app.library.state.value.playlists.firstOrNull {
                        it.name.equals(playlistName, ignoreCase = true)
                    }
                    val description = "Playlist obtida via MCat (${session.search.provider.wireValue.uppercase()}) para $destinationTitle"
                    val firstTrackCover = item.tracks.firstOrNull()?.art.orEmpty()
                    val cover = firstTrackCover.takeIf {
                        it.isNotBlank() && !MusicAcquisitionRules.isMCatJobGenericAlbumCover(it)
                    } ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80"
                    playlistId =
                        if (existing != null) {
                            app.library.addToPlaylist(existing.id, ids)
                            if (existing.description.isBlank() || existing.coverUrl.isBlank()) {
                                app.library.updatePlaylistMetadata(
                                    existing.id,
                                    existing.description.ifBlank { description },
                                    existing.coverUrl.ifBlank { cover },
                                )
                            }
                            existing.id
                        } else {
                            app.library.createPlaylist(
                                name = playlistName,
                                tracks = ids,
                                description = description,
                                coverUrl = cover,
                            )
                        }
                }
            }

            val finalItems = state.value.jobs.firstOrNull { it.id == jobId }?.items.orEmpty()
            val jobSucceeded = finalItems.any {
                it.status == AcquisitionJobStatus.COMPLETED ||
                    it.status == AcquisitionJobStatus.PARTIAL ||
                    it.status == AcquisitionJobStatus.SKIPPED_DUPLICATE
            }
            val jobPartial = finalItems.any { it.status == AcquisitionJobStatus.PARTIAL } ||
                (jobSucceeded && finalItems.any { it.status == AcquisitionJobStatus.FAILED })
            updateJob(jobId) {
                it.copy(
                    status = when {
                        !jobSucceeded -> AcquisitionJobStatus.FAILED
                        jobPartial -> AcquisitionJobStatus.PARTIAL
                        else -> AcquisitionJobStatus.COMPLETED
                    },
                    error = when {
                        !jobSucceeded -> "Nenhuma opção foi adquirida com sucesso."
                        jobPartial -> "Aquisição parcial. Consulta o detalhe de cada opção."
                        else -> ""
                    },
                )
            }

            // MCat refreshes the destination after setting the final job state.
            try {
                app.refresh()
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
            }

            val outcome = AcquisitionOutcome(
                imported = importedCount,
                reused = reusedCount,
                failed = failedCount,
                playlistId = playlistId,
                collectionTitle = completed.filter { it.tracks.isNotEmpty() }
                    .joinToString(", ") { it.option.title },
                note = when (session.search.contentType) {
                    AcquisitionContentType.ALBUM ->
                        if (acquiredAlbums.isEmpty()) "Não foi possível criar um álbum com as faixas recebidas."
                        else "Faixas adicionadas a ${acquiredAlbums.size} álbum(ns) na biblioteca; gravações já existentes foram reutilizadas."
                    AcquisitionContentType.ARTIST ->
                        "A pesquisa por artista devolve opções do bot; cada opção pode produzir uma ou várias faixas, conforme a contagem indicada. As faixas são importadas individualmente, sem criar um álbum ou playlist fictícios."
                    AcquisitionContentType.PLAYLIST ->
                        ""
                    else -> ""
                },
            )
            val incompleteTitles = completed
                .filter { it.error.isNotEmpty() }
                .map { it.option.title }
            val remainingTitles = if (!botTurnFinished) retryTitles else incompleteTitles
            val remainingJob = pending.copy(
                selectedTitles = remainingTitles,
                awaitingBotCompletion = !botTurnFinished,
                botChatId = if (!botTurnFinished) resumeBotChatId else 0L,
                botMessageCursor = if (!botTurnFinished) resumeBotCursor else 0L,
            ).takeIf { remainingTitles.isNotEmpty() }
            if (remainingJob == null) clearPendingJob() else persistPendingJob(remainingJob)
            if (botTurnFinished) activeSearchId = null
            state.value = state.value.copy(
                acquiring = false,
                progress = "",
                outcome = outcome,
                pendingJob = remainingJob,
                error = completed.filter { it.error.isNotEmpty() }
                    .joinToString("\n") { "${it.option.title}: ${it.error}" },
            )
            outcome
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            state.value = state.value.copy(acquiring = false, progress = "", error = friendly(error))
            throw error
        }
    }

    /**
     * Rebuilds the bot search after process death and retries the selected results.
     * The destination is refreshed first, so audio forwarded just before a crash
     * is indexed and reused instead of being forwarded a second time.
     */
    suspend fun resumePending(): AcquisitionOutcome = resumeLane.withLock {
        var pending = state.value.pendingJob ?: readPendingJob()
            ?: error("Não existe nenhuma aquisição interrompida para retomar.")
        state.value = state.value.copy(error = "", progress = "A recuperar a aquisição interrompida…")
        // Block all other entry points while the persisted bot turn is being reconciled.
        activeSearchId = "pending-resume"
        try {
            if (pending.awaitingBotCompletion) {
                waitForBotCompletionSignal(pending.botChatId, pending.botMessageCursor)
                activeSearchId = null
                pending = pending.copy(
                    awaitingBotCompletion = false,
                    botChatId = 0L,
                    botMessageCursor = 0L,
                )
                persistPendingJob(pending)
            } else {
                activeSearchId = null
            }
            val currentUserId = request(json("getMe")).getLong("id")
            check(currentUserId == pending.telegramUserId) {
                "Esta aquisição pertence a outra conta Telegram. Liga a conta original ou descarta o trabalho pendente."
            }
            val destination = request(json("getChat", "chat_id" to pending.destinationChatId))
            app.library.source(app.telegram.rememberChat(destination), true)

            val refreshedSearch = search(
                pending.query,
                pending.contentType,
                pending.provider,
                pending.targetAlbumId,
                allowPendingJob = true,
            )
            val selectedNumbers = pending.selectedTitles.map { title ->
                val matches = refreshedSearch.results.filter {
                    MusicAcquisitionRules.cleanCollectionTitle(it.title).equals(
                        MusicAcquisitionRules.cleanCollectionTitle(title),
                        ignoreCase = true,
                    )
                }
                check(matches.size == 1) {
                    "Não foi possível identificar sem ambiguidades o resultado '${title}' após recuperar a pesquisa."
                }
                matches.single().number
            }
            return acquire(refreshedSearch.id, selectedNumbers, pending.destinationChatId)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            state.value = state.value.copy(
                acquiring = false,
                searching = false,
                progress = "",
                pendingJob = pending,
                error = friendly(error),
            )
            throw error
        }
    }

    private fun updateJob(jobId: String, transform: (AcquisitionJob) -> AcquisitionJob) {
        val now = System.currentTimeMillis()
        state.value = state.value.copy(
            jobs = state.value.jobs.map { job ->
                if (job.id == jobId) transform(job).copy(updatedAt = now) else job
            },
        )
    }

    private fun updateJobItem(
        jobId: String,
        index: Int,
        transform: (AcquisitionJobItem) -> AcquisitionJobItem,
    ) {
        updateJob(jobId) { job ->
            job.copy(items = job.items.mapIndexed { itemIndex, item ->
                if (itemIndex == index) transform(item) else item
            })
        }
    }

    private fun readPendingJob(): PendingAcquisitionJob? = runCatching {
        val raw = app.prefs.getString(pendingJobKey, null) ?: return null
        val json = JSONObject(raw)
        val titles = json.optJSONArray("selected_titles") ?: JSONArray()
        PendingAcquisitionJob(
            query = json.getString("query"),
            contentType = AcquisitionContentType.valueOf(json.getString("content_type")),
            provider = AcquisitionProvider.valueOf(json.getString("provider")),
            selectedTitles = (0 until titles.length()).map { titles.getString(it) },
            destinationChatId = json.getLong("destination_chat_id"),
            telegramUserId = json.optLong("telegram_user_id"),
            targetAlbumId = json.opt("target_album_id")
                ?.takeUnless { it == JSONObject.NULL }
                ?.toString()
                ?.takeIf(String::isNotBlank),
            updatedAt = json.optLong("updated_at", System.currentTimeMillis()),
            awaitingBotCompletion = json.optBoolean("awaiting_bot_completion", false),
            botChatId = json.optLong("bot_chat_id", 0L),
            botMessageCursor = json.optLong("bot_message_cursor", 0L),
        )
    }.getOrNull()

    private fun persistPendingJob(job: PendingAcquisitionJob) {
        val json = JSONObject()
            .put("query", job.query)
            .put("content_type", job.contentType.name)
            .put("provider", job.provider.name)
            .put("selected_titles", JSONArray(job.selectedTitles))
            .put("destination_chat_id", job.destinationChatId)
            .put("telegram_user_id", job.telegramUserId)
            .put("target_album_id", job.targetAlbumId ?: JSONObject.NULL)
            .put("awaiting_bot_completion", job.awaitingBotCompletion)
            .put("bot_chat_id", job.botChatId)
            .put("bot_message_cursor", job.botMessageCursor)
            .put("updated_at", System.currentTimeMillis())
        check(app.prefs.edit().putString(pendingJobKey, json.toString()).commit()) {
            "Não foi possível guardar o estado da aquisição interrompida."
        }
        state.value = state.value.copy(pendingJob = job)
    }

    fun discardPending() {
        check(state.value.pendingJob?.awaitingBotCompletion != true) {
            "O bot ainda não confirmou o fim do pedido. Aguarda a conclusão antes de libertar a conversa."
        }
        activeSearchId = null
        clearPendingJob()
    }

    private fun clearPendingJob() {
        check(app.prefs.edit().remove(pendingJobKey).commit()) {
            "Não foi possível limpar o registo da aquisição."
        }
        state.value = state.value.copy(pendingJob = null)
    }

    private suspend fun acquireOption(
        session: SearchSession,
        option: AcquisitionOption,
        destinationChatId: Long,
        destinationTitle: String,
    ): AcquiredItem {
        val botChatId = session.botChatId
        // Do not skip an entire collection because its first track already exists.
        // Partial retries must still fetch missing tracks and release the bot turn.
        val allowedDedupChats = app.library.state.value.sources.mapTo(mutableSetOf()) { it.id }
            .apply { add(destinationChatId) }
        val latestBeforeSelection = latestMessageId(botChatId)
        val searchMessage = request(
            json("getMessage", "chat_id" to botChatId, "message_id" to session.resultMessageId)
        )
        state.value.pendingJob?.let { pending ->
            persistPendingJob(pending.copy(
                awaitingBotCompletion = true,
                botChatId = botChatId,
                botMessageCursor = latestBeforeSelection,
            ))
        }
        val clicked = clickNumberButton(searchMessage, option.number)
        if (!clicked) sendText(botChatId, option.number.toString())

        val expectedCount = AcquisitionCompletionRules.expectedTrackCount(
            option.title, session.search.contentType,
        )
        val collection = collectAudioMessages(
            botChatId = botChatId,
            afterMessageId = latestBeforeSelection,
            expectedCount = expectedCount,
            contentType = session.search.contentType,
        )
        val audioMessages = collection.messages
        if (audioMessages.isEmpty()) {
            return AcquiredItem(
                option = option,
                tracks = emptyList(),
                error = if (collection.botFinished) {
                    "O bot terminou sem devolver áudio para esta opção."
                } else {
                    "O bot ainda não confirmou o fim do pedido; a conversa permanece bloqueada."
                },
                partial = !collection.botFinished,
                botFinished = collection.botFinished,
                botChatId = botChatId,
                botMessageCursor = collection.lastObservedMessageId,
            )
        }
        val partialWarning = if (collection.partial || !collection.botFinished) {
            if (expectedCount > 0) {
                if (collection.botFinished) {
                    "Resultado parcial (${audioMessages.size}/$expectedCount faixas recebidas)."
                } else {
                    "O bot ainda não confirmou o fim do pedido (${audioMessages.size}/$expectedCount faixas recebidas)."
                }
            } else {
                "O bot ainda não confirmou o fim do pedido (${audioMessages.size} faixa(s) recebidas)."
            }
        } else ""
        val deliveryErrors = mutableListOf<String>()

        val importedTracks = mutableListOf<Track>()
        val reusedTracks = mutableListOf<Track>()
        val importedIds = mutableSetOf<String>()

        // Deduplicate each received recording independently. This is essential
        // when a partial collection is resumed: existing tracks stay reused,
        // while newly arrived tracks are still forwarded to the target.
        for (sourceMessage in audioMessages) {
            try {
                val candidate = parseAcquiredTrack(sourceMessage, destinationTitle)
                if (candidate == null) {
                    deliveryErrors += "Não foi possível interpretar uma mensagem de áudio do bot."
                    continue
                }
                val existingTrack = app.library.state.value.tracks.firstOrNull { track ->
                    track.chatId in allowedDedupChats &&
                        MusicAcquisitionRules.areTracksDuplicate(
                            candidate.title, candidate.artist, track.title, track.artist,
                        )
                }
                val batchDuplicate = (importedTracks + reusedTracks).firstOrNull { track ->
                    MusicAcquisitionRules.areTracksDuplicate(
                        candidate.title, candidate.artist, track.title, track.artist,
                    )
                }
                val duplicate = existingTrack ?: batchDuplicate
                if (duplicate != null) {
                    reusedTracks += duplicate
                    continue
                }
                val forwarded = request(
                    json(
                        "forwardMessages",
                        "chat_id" to destinationChatId,
                        "topic_id" to JSONObject.NULL,
                        "from_chat_id" to botChatId,
                        "message_ids" to JSONArray().put(sourceMessage.getLong("id")),
                        "options" to JSONObject.NULL,
                        "send_copy" to false,
                        "remove_caption" to false,
                    ),
                    timeout = 120_000,
                )
                val messages = forwarded.optJSONArray("messages") ?: JSONArray()
                val destinationMessage = messages.optJSONObject(0)
                val track = destinationMessage?.let { parseAcquiredTrack(it, destinationTitle) }
                if (track == null) {
                    deliveryErrors += "O Telegram não confirmou a entrega de uma mensagem de áudio."
                    continue
                }
                importedTracks += track
                importedIds += track.id
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                deliveryErrors += friendly(error)
            }
        }

        if (importedTracks.isNotEmpty()) {
            app.library.upsert(importedTracks)
            app.library.reload()
        }
        val byId = app.library.state.value.byId
        val tracks = (importedTracks.mapNotNull { byId[it.id] } + reusedTracks)
            .distinctBy { it.id }
        if (tracks.isEmpty()) {
            error("O Telegram não confirmou a entrega de áudio novo ou reutilizável para esta opção.")
        }
        val resultWarnings = (listOf(partialWarning) + deliveryErrors)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" ")
        return AcquiredItem(
            option = option,
            tracks = tracks,
            importedIds = importedIds,
            error = resultWarnings,
            skippedDuplicate = importedTracks.isEmpty() && reusedTracks.isNotEmpty(),
            partial = collection.partial || !collection.botFinished ||
                (tracks.isNotEmpty() && deliveryErrors.isNotEmpty()),
            botFinished = collection.botFinished,
            botChatId = botChatId,
            botMessageCursor = collection.lastObservedMessageId,
        )
    }

    /**
     * GramJS reads DocumentAttributeAudio.title/performer/duration when the bot
     * sends audio as a document. TDLib exposes those fields in the document's
     * attributes array, so map them onto the native Track parsed by the app.
     */
    private fun parseAcquiredTrack(message: JSONObject, destinationTitle: String): Track? {
        val track = app.parseTrack(message, destinationTitle) ?: return null
        val content = message.optJSONObject("content") ?: return track
        val isDocument = content.kind() == "messageDocument"
        val media = content.optJSONObject(if (isDocument) "document" else "audio") ?: return track
        val audioAttribute = media.optJSONArray("attributes")?.let { attributes ->
            (0 until attributes.length())
                .mapNotNull { attributes.optJSONObject(it) }
                .firstOrNull { it.kind() == "documentAttributeAudio" }
        }
        val title = media.optString("title")
            .ifBlank { audioAttribute?.optString("title").orEmpty() }
            .ifBlank { track.title }
        val artist = media.optString("performer")
            .ifBlank { audioAttribute?.optString("performer").orEmpty() }
            .ifBlank { track.artist }
        val duration = media.optInt("duration")
            .takeIf { it > 0 }
            ?: audioAttribute?.optInt("duration")?.takeIf { it > 0 }
            ?: track.duration
        return track.copy(title = title, artist = artist, duration = duration)
    }

    private suspend fun collectAudioMessages(
        botChatId: Long,
        afterMessageId: Long,
        expectedCount: Int,
        contentType: AcquisitionContentType,
    ): AudioCollection {
        val collected = linkedMapOf<Long, JSONObject>()
        val started = System.currentTimeMillis()
        val maxWait = AcquisitionCompletionRules.maxWaitMillis(contentType, expectedCount)
        var lastObservedMessageId = afterMessageId
        var lastAudioReceivedAt = 0L
        var hasCompletionSignal = false
        var completionReason: AcquisitionCompletionReason? = null

        while (System.currentTimeMillis() - started < maxWait) {
            delay(2_000)
            val messages = historyAfter(botChatId, lastObservedMessageId)
            for (message in messages) {
                val id = message.optLong("id")
                if (id <= lastObservedMessageId) continue
                lastObservedMessageId = maxOf(lastObservedMessageId, id)
                if (isAudioMessage(message) && collected.putIfAbsent(id, message) == null) {
                    lastAudioReceivedAt = System.currentTimeMillis()
                    state.value = state.value.copy(
                        progress = "A receber áudio: ${collected.size}" +
                            if (expectedCount > 0) "/$expectedCount" else " faixa(s)",
                    )
                }
                if (isCompletionMessage(message)) hasCompletionSignal = true
            }

            val now = System.currentTimeMillis()
            completionReason = AcquisitionCompletionRules.completionReason(
                contentType = contentType,
                expectedCount = expectedCount,
                audioCount = collected.size,
                hasCompletionSignal = hasCompletionSignal,
                silenceMillis = if (lastAudioReceivedAt == 0L) 0L else now - lastAudioReceivedAt,
                elapsedMillis = now - started,
            )
            if (completionReason != null) break
        }

        if (completionReason == null) {
            completionReason = if (collected.isEmpty()) AcquisitionCompletionReason.DEADLINE_NO_AUDIO
            else AcquisitionCompletionReason.DEADLINE_PARTIAL
        }
        return AudioCollection(
            messages = collected.values.sortedBy { it.getLong("id") },
            completionReason = completionReason,
            botFinished = completionReason == AcquisitionCompletionReason.COMPLETION_SIGNAL,
            lastObservedMessageId = lastObservedMessageId,
        )
    }

    private suspend fun waitForBotCompletionSignal(botChatId: Long, afterMessageId: Long) {
        check(botChatId != 0L) { "Não foi possível identificar a conversa do bot para retomar a aquisição." }
        val started = System.currentTimeMillis()
        var cursor = afterMessageId
        while (System.currentTimeMillis() - started < 240_000L) {
            delay(2_000)
            val messages = historyAfter(botChatId, cursor)
            for (message in messages) {
                val id = message.optLong("id")
                if (id <= cursor) continue
                cursor = maxOf(cursor, id)
                if (isCompletionMessage(message)) return
            }
        }
        error("O bot não confirmou o fim do pedido no prazo de recuperação. A conversa continua bloqueada; não foi enviado um novo pedido.")
    }

    private suspend fun resolveBot(): JSONObject {
        val bot = request(json("searchPublicChat", "username" to "MusicsHuntersbot"))
        val account = request(json("getMe"))
        val startedKey = "music_bot_started_${account.getLong("id")}"
        if (!app.prefs.getBoolean(startedKey, false)) {
            sendText(bot.getLong("id"), "/start")
            app.prefs.edit().putBoolean(startedKey, true).apply()
        }
        return bot
    }

    private suspend fun sendText(chatId: Long, text: String): JSONObject =
        withFloodWaitRetry {
            app.telegram.sendConfirmed(
                json(
                    "sendMessage",
                    "chat_id" to chatId,
                    "input_message_content" to
                        json(
                            "inputMessageText",
                            "text" to json("formattedText", "text" to text, "entities" to JSONArray()),
                        ),
                ),
                timeout = 45_000,
            )
        }

    private suspend fun clickButton(message: JSONObject, keyword: String): Boolean {
        val markup = message.optJSONObject("reply_markup") ?: return false
        if (markup.kind() != "replyMarkupInlineKeyboard") return false
        val rows = markup.optJSONArray("rows") ?: return false
        for (rowIndex in 0 until rows.length()) {
            val buttons = rows.optJSONArray(rowIndex) ?: continue
            for (buttonIndex in 0 until buttons.length()) {
                val button = buttons.optJSONObject(buttonIndex) ?: continue
                val type = button.optJSONObject("type") ?: continue
                val buttonText = button.optString("text")
                val callbackData = type.optString("data")
                if (!buttonText.contains(keyword, ignoreCase = true) &&
                    !callbackData.contains(keyword, ignoreCase = true)
                ) continue
                if (type.kind() != "inlineKeyboardButtonTypeCallback") continue
                val data = type.optString("data")
                if (data.isEmpty()) continue
                request(
                    json(
                        "getCallbackQueryAnswer",
                        "chat_id" to message.getLong("chat_id"),
                        "message_id" to message.getLong("id"),
                        "payload" to json("callbackQueryPayloadData", "data" to data),
                    ),
                    timeout = 45_000,
                )
                return true
            }
        }
        return false
    }

    private suspend fun clickNumberButton(message: JSONObject, number: Int): Boolean {
        val markup = message.optJSONObject("reply_markup") ?: return false
        if (markup.kind() != "replyMarkupInlineKeyboard") return false
        val rows = markup.optJSONArray("rows") ?: return false
        for (rowIndex in 0 until rows.length()) {
            val buttons = rows.optJSONArray(rowIndex) ?: continue
            for (buttonIndex in 0 until buttons.length()) {
                val button = buttons.optJSONObject(buttonIndex) ?: continue
                val label = button.optString("text").trim()
                if (label != number.toString() && label.filter(Char::isDigit) != number.toString()) continue
                val type = button.optJSONObject("type") ?: continue
                if (type.kind() != "inlineKeyboardButtonTypeCallback") continue
                val data = type.optString("data")
                if (data.isEmpty()) continue
                request(
                    json(
                        "getCallbackQueryAnswer",
                        "chat_id" to message.getLong("chat_id"),
                        "message_id" to message.getLong("id"),
                        "payload" to json("callbackQueryPayloadData", "data" to data),
                    ),
                    timeout = 45_000,
                )
                return true
            }
        }
        return false
    }

    private suspend fun waitForBotMessage(
        chatId: Long,
        afterMessageId: Long,
        timeoutMs: Long,
        predicate: (JSONObject) -> Boolean,
    ): JSONObject? {
        repeat((timeoutMs / 1_000L).toInt().coerceAtLeast(1)) {
            val match = history(chatId, 5)
                .firstOrNull {
                    AcquisitionCompletionRules.isAfterMessageCursor(it.optLong("id"), afterMessageId) &&
                        predicate(it)
                }
            if (match != null) return match
            delay(1_000)
        }
        return null
    }

    private suspend fun waitForEditedMessage(
        chatId: Long,
        messageId: Long,
        timeoutMs: Long,
        predicate: (JSONObject) -> Boolean,
    ): JSONObject? {
        val started = System.currentTimeMillis()
        while (System.currentTimeMillis() - started < timeoutMs) {
            val message = runCatching {
                request(json("getMessage", "chat_id" to chatId, "message_id" to messageId))
            }.getOrNull()
            if (message != null && predicate(message)) return message
            delay(400)
        }
        return null
    }

    /**
     * A Telegram bot may edit the message carrying its inline keyboard or send
     * a replacement message after a callback. Accept both forms; a replacement
     * must have an ID newer than the callback message to reject stale keyboards.
     */
    private suspend fun waitForBotUpdate(
        chatId: Long,
        previousMessageId: Long,
        timeoutMs: Long,
        predicate: (JSONObject) -> Boolean,
    ): JSONObject? {
        repeat((timeoutMs / 600L).toInt().coerceAtLeast(1)) {
            val edited = runCatching {
                request(json("getMessage", "chat_id" to chatId, "message_id" to previousMessageId))
            }.getOrNull()
            if (edited != null && predicate(edited)) return edited

            val replacement = historyAfter(chatId, previousMessageId).firstOrNull { message ->
                predicate(message)
            }
            if (replacement != null) return replacement
            delay(600)
        }
        return null
    }

    private suspend fun request(
        query: JSONObject,
        timeout: Long = 30_000,
    ): JSONObject = withFloodWaitRetry { app.telegram.request(query, timeout) }

    private suspend fun <T> withFloodWaitRetry(block: suspend () -> T): T {
        var retries = 0
        while (true) {
            try {
                return block()
            } catch (error: TelegramException) {
                if (error.code != 429) throw error
                val waitSeconds = Regex("(?:FLOOD_WAIT_|retry after )(\\d+)", RegexOption.IGNORE_CASE)
                    .find(error.message.orEmpty())
                    ?.groupValues
                    ?.get(1)
                    ?.toLongOrNull()
                    ?: 5L
                if (retries >= 3 || waitSeconds > 900L) throw error
                state.value = state.value.copy(
                    progress = "O Telegram limitou temporariamente os pedidos. A aguardar ${waitSeconds}s…",
                )
                delay(waitSeconds.coerceAtLeast(1L) * 1_000L)
                retries++
            }
        }
    }

    private suspend fun history(
        chatId: Long,
        limit: Int,
        fromMessageId: Long = 0L,
    ): List<JSONObject> {
        val response = request(
            json(
                "getChatHistory",
                "chat_id" to chatId,
                "from_message_id" to fromMessageId,
                "offset" to 0,
                "limit" to limit.coerceIn(1, 100),
                "only_local" to false,
            ),
            timeout = 15_000,
        )
        val messages = response.optJSONArray("messages") ?: return emptyList()
        return (0 until messages.length()).mapNotNull { messages.optJSONObject(it) }
    }

    /**
     * Telegram returns chat history in reverse chronological order and caps
     * each response at 100 messages. Page backwards to the cursor captured
     * immediately before selecting the bot result, otherwise large playlists
     * silently lose the oldest tracks once the bot emits more than 100 messages.
     */
    private suspend fun historyAfter(chatId: Long, afterMessageId: Long): List<JSONObject> {
        val collected = linkedMapOf<Long, JSONObject>()
        var fromMessageId = 0L
        var pages = 0
        while (pages < 20 && collected.size < 2_000) {
            val page = history(chatId, 100, fromMessageId)
            if (page.isEmpty()) break
            page.forEach { message ->
                val id = message.optLong("id")
                if (id > afterMessageId) collected[id] = message
            }
            val oldest = page.minOfOrNull { it.optLong("id") } ?: break
            if (oldest <= afterMessageId || page.size < 100 || oldest == fromMessageId) break
            fromMessageId = oldest
            pages++
        }
        return collected.values.sortedBy { it.optLong("id") }
    }

    private suspend fun latestMessageId(chatId: Long): Long =
        history(chatId, 20).maxOfOrNull { it.optLong("id") } ?: 0L

    private fun parseResults(text: String): List<AcquisitionOption> =
        MusicAcquisitionRules.parseResults(text)

    private fun messageText(message: JSONObject): String {
        val content = message.optJSONObject("content") ?: return ""
        val text = content.optJSONObject("text")?.optString("text")
        if (!text.isNullOrBlank()) return text
        return content.optJSONObject("caption")?.optString("text").orEmpty()
    }

    private fun hasInlineKeyboard(message: JSONObject): Boolean =
        message.optJSONObject("reply_markup")?.kind() == "replyMarkupInlineKeyboard"

    private fun isAudioMessage(message: JSONObject): Boolean {
        val content = message.optJSONObject("content") ?: return false
        return when (content.kind()) {
            "messageAudio" -> true
            "messageDocument" -> {
                val document = content.optJSONObject("document") ?: return false
                val audioAttribute = document.optJSONArray("attributes")?.let { attributes ->
                    (0 until attributes.length()).any { index ->
                        attributes.optJSONObject(index)?.kind() == "documentAttributeAudio"
                    }
                } == true
                document.optString("mime_type").startsWith("audio/") ||
                    audioAttribute ||
                    document.optString("file_name").substringAfterLast('.').lowercase() in
                    setOf("mp3", "m4a", "flac", "ogg", "opus", "aac", "wav", "aiff", "alac")
            }
            else -> false
        }
    }

    private fun isCompletionMessage(message: JSONObject): Boolean =
        AcquisitionCompletionRules.isCompletionSignal(messageText(message))

    private fun isSameRecording(candidate: Track, existing: Track): Boolean =
        MusicAcquisitionRules.isSameRecording(candidate, existing)

    private fun sanitizeSearchQuery(value: String): String =
        MusicAcquisitionRules.sanitizeSearchQuery(value)

    private fun checkTelegramReady() {
        check(app.telegram.auth.value.type == "authorizationStateReady") {
            "Liga a tua conta Telegram antes de pesquisar música."
        }
    }

    private fun friendly(error: Throwable): String =
        error.message?.takeIf { it.isNotBlank() }?.take(240) ?: "Falha na aquisição musical."
}