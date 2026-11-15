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

data class AcquisitionState(
    val searching: Boolean = false,
    val acquiring: Boolean = false,
    val progress: String = "",
    val error: String = "",
    val search: AcquisitionSearch? = null,
    val outcome: AcquisitionOutcome? = null,
)

private data class SearchSession(
    val search: AcquisitionSearch,
    val botChatId: Long,
    val resultMessageId: Long,
)

private data class AcquiredItem(
    val option: AcquisitionOption,
    val tracks: List<Track>,
    val importedIds: Set<String> = emptySet(),
    val error: String = "",
)

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
    private val searches = linkedMapOf<String, SearchSession>()

    suspend fun search(
        rawQuery: String,
        contentType: AcquisitionContentType,
        provider: AcquisitionProvider,
    ): AcquisitionSearch = botLane.withLock {
        checkTelegramReady()
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
            var resultMessage = waitForBotMessage(botChatId, sent.getLong("id"), 30_000) {
                !it.optBoolean("is_outgoing") &&
                    (messageText(it).contains("searching for", ignoreCase = true) ||
                        hasInlineKeyboard(it))
            } ?: run {
                val reply = history(botChatId, 50)
                    .asSequence()
                    .filter { it.optLong("id") > sent.getLong("id") }
                    .filter { !it.optBoolean("is_outgoing") }
                    .maxByOrNull { it.optLong("id") }
                val replyText = reply?.let(::messageText)?.trim().orEmpty()
                if (replyText.isNotBlank()) {
                    error("Resposta do bot não reconhecida: ${replyText.take(180)}")
                }
                error("O bot de música não respondeu à pesquisa em 30 segundos.")
            }

            val typeMarker = "in '${contentType.wireValue}'"
            if (!messageText(resultMessage).contains(typeMarker, ignoreCase = true)) {
                state.value = state.value.copy(progress = "A seleccionar o tipo de conteúdo…")
                if (clickButton(resultMessage, contentType.wireValue)) {
                    resultMessage = waitForBotUpdate(
                        botChatId, resultMessage.getLong("id"), 12_000,
                    ) { messageText(it).contains(typeMarker, ignoreCase = true) } ?: resultMessage
                }
            }
            check(messageText(resultMessage).contains(typeMarker, ignoreCase = true)) {
                "O bot não confirmou o tipo de conteúdo ${contentType.wireValue}."
            }

            val providerMarker = "from '${provider.wireValue}'"
            if (!messageText(resultMessage).contains(providerMarker, ignoreCase = true)) {
                state.value = state.value.copy(progress = "A seleccionar o fornecedor…")
                if (clickButton(resultMessage, provider.wireValue)) {
                    resultMessage = waitForBotUpdate(
                        botChatId, resultMessage.getLong("id"), 12_000,
                    ) { messageText(it).contains(providerMarker, ignoreCase = true) } ?: resultMessage
                }
            }
            check(messageText(resultMessage).contains(providerMarker, ignoreCase = true)) {
                "O bot não confirmou o fornecedor ${provider.wireValue}."
            }

            var effectiveProvider = provider
            var options = parseResults(messageText(resultMessage))
            // MCat Player falls back to SoundCloud when Deezer has no album
            // results. Keep the same search message/session and record the
            // provider that actually produced the options.
            if (options.isEmpty() && contentType == AcquisitionContentType.ALBUM &&
                provider == AcquisitionProvider.DEEZER
            ) {
                state.value = state.value.copy(progress = "Sem álbuns no Deezer; a tentar SoundCloud…")
                if (clickButton(resultMessage, AcquisitionProvider.SOUNDCLOUD.wireValue)) {
                    resultMessage = waitForBotUpdate(
                        botChatId, resultMessage.getLong("id"), 12_000,
                    ) { messageText(it).contains("from 'soundcloud'", ignoreCase = true) } ?: resultMessage
                    if (messageText(resultMessage).contains("from 'soundcloud'", ignoreCase = true)) {
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
            searches[search.id] = SearchSession(search, bot.getLong("id"), resultMessage.getLong("id"))
            while (searches.size > 12) searches.remove(searches.keys.first())
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

        state.value = state.value.copy(acquiring = true, searching = false, progress = "", error = "", outcome = null)
        try {
            val destination = app.telegram.request(json("getChat", "chat_id" to destinationChatId))
            val destinationTitle = destination.optString("title").ifBlank { "Telegram" }
            val destinationChoice = app.telegram.rememberChat(destination)
            app.library.source(destinationChoice, true)
            if (app.library.state.value.sources.firstOrNull { it.id == destinationChatId }?.fullyIndexed != true) {
                state.value = state.value.copy(progress = "A indexar o histórico de música do destino para evitar duplicados…")
                app.refresh()
                check(app.library.state.value.sources.firstOrNull { it.id == destinationChatId }?.fullyIndexed == true) {
                    "Não foi possível indexar completamente o destino. A aquisição foi interrompida para evitar duplicados."
                }
            }

            val completed = mutableListOf<AcquiredItem>()
            selected.forEachIndexed { index, option ->
                state.value = state.value.copy(
                    progress = "A obter opção ${index + 1}/${selected.size}: ${option.title}",
                )
                try {
                    completed += acquireOption(session, option, destinationChatId, destinationTitle)
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    completed += AcquiredItem(option, emptyList(), error = friendly(error))
                }
            }

            val allTrackIds = completed.flatMap { it.tracks }.map { it.id }.distinct()
            val importedIds = completed.flatMap { it.importedIds }.toSet()
            val importedCount = importedIds.size
            val reusedCount = (allTrackIds.size - importedIds.size).coerceAtLeast(0)
            val failedCount = completed.count { it.error.isNotEmpty() }

            var playlistId: Long? = null
            val acquiredAlbums = mutableListOf<String>()
            if (session.search.contentType == AcquisitionContentType.ALBUM) {
                for (item in completed.filter { it.tracks.isNotEmpty() }) {
                    val first = item.tracks.first()
                    val title = item.option.title.substringBefore(" (").trim().ifBlank { item.option.title }
                    val albumId = app.library.createOrUpdateAlbum(
                        title = title,
                        artist = first.artist,
                        tracks = item.tracks.map { it.id },
                        year = "",
                        coverUrl = first.art,
                    )
                    acquiredAlbums += albumId
                }
            }
            val autoCreatePlaylist = app.prefs.getBoolean("auto_create_acquisition_playlists", true)
            if (session.search.contentType == AcquisitionContentType.PLAYLIST && autoCreatePlaylist) {
                for (item in completed.filter { it.tracks.isNotEmpty() }) {
                    val ids = item.tracks.map { it.id }.distinct()
                    val existing = app.library.state.value.playlists.firstOrNull {
                        normalizeMusicText(it.name) == normalizeMusicText(item.option.title)
                    }
                    playlistId =
                        if (existing != null) {
                            app.library.addToPlaylist(existing.id, ids)
                            existing.id
                        } else {
                            app.library.createPlaylist(item.option.title, ids)
                        }
                }
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
                        else "${acquiredAlbums.size} álbum(ns) criado(s) na biblioteca."
                    AcquisitionContentType.ARTIST ->
                        "As faixas seleccionadas foram adicionadas; a aquisição integral e organização por artista ainda precisa de validação."
                    AcquisitionContentType.PLAYLIST ->
                        if (autoCreatePlaylist) "" else "As faixas foram adicionadas à biblioteca sem criar uma playlist local."
                    else -> ""
                },
            )
            state.value = state.value.copy(
                acquiring = false,
                progress = "",
                outcome = outcome,
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

    private suspend fun acquireOption(
        session: SearchSession,
        option: AcquisitionOption,
        destinationChatId: Long,
        destinationTitle: String,
    ): AcquiredItem {
        val botChatId = session.botChatId
        val latestBeforeSelection = latestMessageId(botChatId)
        val searchMessage = app.telegram.request(
            json("getMessage", "chat_id" to botChatId, "message_id" to session.resultMessageId)
        )
        val clicked = clickNumberButton(searchMessage, option.number)
        if (!clicked) sendText(botChatId, option.number.toString())

        val expectedCount = Regex("\\((\\d+)\\s*(?:🎵|faixas?|tracks?|musicas?)?\\)", RegexOption.IGNORE_CASE)
            .find(option.title)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val multiTrack = session.search.contentType == AcquisitionContentType.PLAYLIST ||
            session.search.contentType == AcquisitionContentType.ALBUM || expectedCount > 1
        val audioMessages = collectAudioMessages(
            botChatId = botChatId,
            afterMessageId = latestBeforeSelection,
            expectedCount = expectedCount,
            multiTrack = multiTrack,
        )
        if (audioMessages.isEmpty()) error("O bot não devolveu áudio para esta opção.")
        var partialError =
            if (expectedCount > 0 && audioMessages.size < expectedCount)
                "O bot enviou ${audioMessages.size} de ${expectedCount} faixas."
            else ""

        val parsedPairs = audioMessages.mapNotNull { message ->
            app.parseTrack(message, "Music bot")?.let { message to it }
        }
        if (parsedPairs.isEmpty()) error("As mensagens recebidas não contêm áudio compatível.")

        val existingTracks = app.library.state.value.tracks
        val orderedIds = MutableList<String?>(parsedPairs.size) { null }
        val pendingMessages = mutableListOf<JSONObject>()
        val pendingPositions = mutableListOf<Int>()
        val pendingCandidates = mutableListOf<Track>()
        val duplicatePendingPositions = mutableMapOf<Int, Int>()
        val seen = mutableSetOf<String>()
        for ((index, pair) in parsedPairs.withIndex()) {
            val sourceTrack = pair.second
            val recordingKey = sourceTrack.fileKey.ifBlank {
                "${normalizeMusicText(sourceTrack.title)}|${normalizeMusicText(sourceTrack.artist)}|${sourceTrack.duration}"
            }
            if (!seen.add(recordingKey)) continue
            val existing = existingTracks.firstOrNull { isSameRecording(sourceTrack, it) }
            if (existing != null) {
                orderedIds[index] = existing.id
            } else {
                // Also deduplicate against candidates in this same bot response.
                // Different Telegram file IDs can still describe the same recording.
                val pendingDuplicate = pendingCandidates.indexOfFirst {
                    isSameRecording(sourceTrack, it)
                }
                if (pendingDuplicate >= 0) {
                    duplicatePendingPositions[index] = pendingDuplicate
                } else {
                    pendingMessages += pair.first
                    pendingPositions += index
                    pendingCandidates += sourceTrack
                }
            }
        }

        val importedIds = mutableSetOf<String>()
        if (pendingMessages.isNotEmpty()) {
            val importedTracks = mutableListOf<Track>()
            var deliveryFailures = 0
            // MCat forwards each Telegram message independently. Keep that
            // isolation: one failed message must not cancel the remaining tracks.
            for ((index, sourceMessage) in pendingMessages.withIndex()) {
                try {
                    val forwarded = app.telegram.request(
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
                    val track = destinationMessage?.let { app.parseTrack(it, destinationTitle) }
                    if (track == null) {
                        deliveryFailures++
                        continue
                    }
                    importedTracks += track
                    orderedIds[pendingPositions[index]] = track.id
                    importedIds += track.id
                } catch (error: Throwable) {
                    if (error is CancellationException) throw error
                    deliveryFailures++
                }
            }
            if (importedTracks.isNotEmpty()) {
                app.library.upsert(importedTracks)
                app.library.reload()
            }
            if (importedTracks.isEmpty()) {
                error("O Telegram não confirmou a entrega do áudio ao destino.")
            }
            if (deliveryFailures > 0) {
                partialError = listOf(
                    partialError,
                    "$deliveryFailures de ${pendingMessages.size} mensagens não foram confirmadas no destino.",
                ).filter { it.isNotBlank() }.joinToString(" ")
            }
        }

        // A duplicate candidate follows the outcome of the first matching message.
        duplicatePendingPositions.forEach { (duplicateIndex, pendingIndex) ->
            val originalIndex = pendingPositions.getOrNull(pendingIndex)
            if (originalIndex != null) orderedIds[duplicateIndex] = orderedIds[originalIndex]
        }

        val byId = app.library.state.value.byId
        val tracks = orderedIds.filterNotNull().distinct().mapNotNull { id -> byId[id] }
        if (tracks.isEmpty()) error("Não foi possível importar nenhuma faixa desta opção.")
        return AcquiredItem(option, tracks, importedIds, partialError)
    }

    private suspend fun collectAudioMessages(
        botChatId: Long,
        afterMessageId: Long,
        expectedCount: Int,
        multiTrack: Boolean,
    ): List<JSONObject> {
        val collected = linkedMapOf<Long, JSONObject>()
        val started = System.currentTimeMillis()
        var lastAudioAt = 0L
        var finished = false
        val maxWait = if (multiTrack) 240_000L else 45_000L

        while (System.currentTimeMillis() - started < maxWait) {
            val messages = history(botChatId, 100)
            for (message in messages) {
                val id = message.optLong("id")
                if (id <= afterMessageId) continue
                if (isAudioMessage(message)) {
                    if (collected.putIfAbsent(id, message) == null) lastAudioAt = System.currentTimeMillis()
                }
                if (isCompletionMessage(message)) finished = true
            }
            if (expectedCount > 0 && collected.size >= expectedCount) break
            if (finished && collected.isNotEmpty()) break
            if (!multiTrack && collected.isNotEmpty()) break
            if (multiTrack && collected.isNotEmpty() && lastAudioAt > 0 &&
                System.currentTimeMillis() - lastAudioAt >= 9_000L
            ) break
            delay(2_000)
        }
        return collected.values.sortedBy { it.getLong("id") }
    }

    private suspend fun resolveBot(): JSONObject {
        val bot = app.telegram.request(json("searchPublicChat", "username" to "MusicsHuntersbot"))
        val account = app.telegram.request(json("getMe"))
        val startedKey = "music_bot_started_${account.getLong("id")}"
        if (!app.prefs.getBoolean(startedKey, false)) {
            sendText(bot.getLong("id"), "/start")
            app.prefs.edit().putBoolean(startedKey, true).apply()
        }
        return bot
    }

    private suspend fun sendText(chatId: Long, text: String): JSONObject =
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
                app.telegram.request(
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
                app.telegram.request(
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
        val started = System.currentTimeMillis()
        while (System.currentTimeMillis() - started < timeoutMs) {
            val match = history(chatId, 50)
                .asSequence()
                .filter { it.optLong("id") > afterMessageId }
                .filter(predicate)
                .maxByOrNull { it.getLong("id") }
            if (match != null) return match
            delay(500)
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
                app.telegram.request(json("getMessage", "chat_id" to chatId, "message_id" to messageId))
            }.getOrNull()
            if (message != null && predicate(message)) return message
            delay(400)
        }
        return null
    }

    /**
     * A Telegram bot may edit the message carrying its inline keyboard or send
     * a replacement message after a callback. Accept both forms, but only after
     * the callback's message ID so stale keyboards cannot satisfy the wait.
     */
    private suspend fun waitForBotUpdate(
        chatId: Long,
        previousMessageId: Long,
        timeoutMs: Long,
        predicate: (JSONObject) -> Boolean,
    ): JSONObject? {
        val started = System.currentTimeMillis()
        while (System.currentTimeMillis() - started < timeoutMs) {
            val edited = runCatching {
                app.telegram.request(
                    json("getMessage", "chat_id" to chatId, "message_id" to previousMessageId)
                )
            }.getOrNull()
            if (edited != null && predicate(edited)) return edited

            val replacement = history(chatId, 50)
                .asSequence()
                .filter { it.optLong("id") > previousMessageId }
                .filter { !it.optBoolean("is_outgoing") }
                .filter(predicate)
                .maxByOrNull { it.optLong("id") }
            if (replacement != null) return replacement
            delay(400)
        }
        return null
    }

    private suspend fun history(chatId: Long, limit: Int): List<JSONObject> {
        val response = app.telegram.request(
            json(
                "getChatHistory",
                "chat_id" to chatId,
                "from_message_id" to 0,
                "offset" to 0,
                "limit" to limit.coerceIn(1, 100),
                "only_local" to false,
            ),
            timeout = 15_000,
        )
        val messages = response.optJSONArray("messages") ?: return emptyList()
        return (0 until messages.length()).mapNotNull { messages.optJSONObject(it) }
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

    private fun isCompletionMessage(message: JSONObject): Boolean {
        val text = messageText(message).lowercase()
        return listOf(
            "finished", "download complete", "completed", "concluído",
            "todas as faixas", "process complete", "done!", "pronto!",
        ).any(text::contains)
    }

    private fun isSameRecording(candidate: Track, existing: Track): Boolean =
        MusicAcquisitionRules.isSameRecording(candidate, existing)

    private fun normalizeMusicText(value: String): String =
        MusicAcquisitionRules.normalizeMusicText(value)

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