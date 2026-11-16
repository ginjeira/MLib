package app.spotygram

/**
 * Direct Kotlin port of the parsing, sanitizing and duplicate-matching helpers
 * used by ginjeira/MCat-Player (server.ts and src/utils/trackSanitizer.ts).
 * Only the language/runtime syntax is adapted; matching rules stay equivalent.
 */
internal data class ParsedMusicName(val artist: String, val title: String)

internal object MusicAcquisitionRules {
    fun parseResults(text: String): List<AcquisitionOption> {
        val results = mutableListOf<AcquisitionOption>()
        if (text.isEmpty()) return results
        text.lineSequence().forEach { line ->
            val match = Regex("^(\\d+)\\.\\s*(.+)").find(line)
            if (match != null) {
                results += AcquisitionOption(
                    match.groupValues[1].toInt(),
                    match.groupValues[2].trim(),
                )
            }
        }
        return results
    }

    fun sanitizeSearchQuery(value: String): String {
        if (value.isEmpty()) return ""
        var query = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
        query = query.replace(
            Regex("[\\(\\[]\\s*\\d+\\s*(?:🎵|🎶|track|tracks|faixas?)?\\s*[\\)\\]]", RegexOption.IGNORE_CASE),
            " ",
        )
        query = query.replace(
            Regex("[\\(\\[]\\s*(?:19|20)\\d{2}(?:\\s*[-–/]\\s*(?:(?:19|20)?\\d{2}))?\\s*[\\)\\]]"),
            " ",
        )
        query = query.replace(
            Regex("[\\(\\[]\\s*(?:official|video|audio|lyrics?|remaster(?:ed)?|hd|4k|hq|visualizer|clip|feat\\.?|ft\\.?|320\\s*kbps|flac|mp3|lossless|cd\\s*rip|mono|stereo|live|acoustic|edit|version|deluxe|bonus|single|ep).*?[\\)\\]]", RegexOption.IGNORE_CASE),
            " ",
        )
        query = query.replace(Regex("\\s*-\\s*sele[cç][aã]o\\b", RegexOption.IGNORE_CASE), " ")
        query = query.replace(Regex("\\.(mp3|flac|m4a|wav|aac|ogg|wma)$", RegexOption.IGNORE_CASE), "")
        query = query.replace(
            Regex("[\\x{1F300}-\\x{1F9FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}\\x{1FA00}-\\x{1FAFF}🎵🎶🎧📻🎼🎤🎸🎹🥁🎺🎷🎻🎙️▶️►▪️●★☆•⭐✨🔥💯]"),
            " ",
        )
        query = query.replace(Regex("[()\\[\\]{}【】《》〔〕「」『』]"), " ")
        query = query.replace(Regex("^\\s*\\d+[.\\-)]\\s*"), " ")
        query = query.replace(Regex("[•·~_#|\\\\/^*+=<>§©®™✓✗►▶◀«»\\x22“”„‟`´]"), " ")
        query = query.replace(Regex("\\s+"), " ").trim()
        return query.ifEmpty { value.trim() }
    }

    private fun sanitizeTrackTitle(rawTitle: String): String {
        if (rawTitle.isEmpty()) return ""
        var title = java.text.Normalizer.normalize(rawTitle, java.text.Normalizer.Form.NFKC)
        title = title.replace(
            Regex("[\\(\\[]\\s*\\d+\\s*(?:🎵|🎶|track|tracks|faixas?)?\\s*[\\)\\]]", RegexOption.IGNORE_CASE),
            " ",
        )
        title = title.replace(Regex("[\\u200B-\\u200D\\uFEFF\\u00A0\\u200E\\u200F\\u202A-\\u202E]"), " ")
        title = title.replace(Regex("\\.(mp3|flac|m4a|wav|aac|ogg|wma)$", RegexOption.IGNORE_CASE), "")
        title = title.replace(
            Regex("[\\x{1F300}-\\x{1F9FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}\\x{1FA00}-\\x{1FAFF}🎵🎶🎧📻🎼🎤🎸🎹🥁🎺🎷🎻🎙️▶️►▪️●★☆•⭐✨🔥💯]"),
            " ",
        )
        title = title.replace(
            Regex("\\s*\\[(?:official|video|audio|lyrics?|remaster(?:ed)?|hd|4k|hq|visualizer|clip|feat\\.?|ft\\.?|320\\s*kbps|flac|mp3|lossless|cd\\s*rip|mono|stereo|live|acoustic|edit|version).*?\\]", RegexOption.IGNORE_CASE),
            " ",
        )
        title = title.replace(
            Regex("\\s*\\((?:official|video|audio|lyrics?|remaster(?:ed)?|hd|4k|hq|visualizer|clip|feat\\.?|ft\\.?|320\\s*kbps|flac|mp3|lossless|cd\\s*rip|mono|stereo|live|acoustic|edit|version).*?\\)", RegexOption.IGNORE_CASE),
            " ",
        )
        title = title.replace(Regex("[【】《》〔〕「」『』]"), " ")
        title = title.replace(Regex("^\\s*\\d+[.\\-)]\\s*"), " ")
        title = title.replace(Regex("[•·~_#|\\\\/^*+=<>§©®™✓✗►▶◀«»\\x22“”„‟`´]"), " ")
        title = title.replace(Regex("\\s+"), " ").trim()
        return title.ifEmpty { rawTitle.trim() }
    }

    fun parseArtistAndTitle(raw: String): ParsedMusicName {
        val clean = sanitizeTrackTitle(raw).replace(Regex("@\\w+"), "").trim()
        val parts = clean.split('-')
        if (parts.size >= 2) {
            val artist = parts.first().trim()
            val title = parts.drop(1).joinToString("-").trim()
            if (artist.isNotEmpty() && title.isNotEmpty()) return ParsedMusicName(artist, title)
        }
        val by = Regex("^(.*?)\\s+by\\s+(.*?)$", RegexOption.IGNORE_CASE).matchEntire(clean)
        if (by != null && by.groupValues[1].isNotEmpty() && by.groupValues[2].isNotEmpty()) {
            return ParsedMusicName(by.groupValues[2].trim(), by.groupValues[1].trim())
        }
        return ParsedMusicName("Artista Desconhecido", clean.ifEmpty { raw })
    }

    fun cleanCollectionTitle(rawName: String): String {
        if (rawName.isEmpty()) return ""
        var name = java.text.Normalizer.normalize(rawName, java.text.Normalizer.Form.NFKC)
        name = name.replace(
            Regex("[\\(\\[]\\s*\\d+\\s*(?:🎵|🎶|track|tracks|faixas?)?\\s*[\\)\\]]", RegexOption.IGNORE_CASE),
            " ",
        )
        name = name.replace(Regex("\\s*[🎵🎶🎧📻🎼🎤🎸🎹🥁🎺🎷🎻🎙️▶️►▪️●★☆•⭐✨🔥💯]+\\s*$"), "")
        name = name.replace(Regex("^\\s*\\d+[.\\-)]\\s*"), " ")
        name = name.replace(Regex("\\s+"), " ").trim()
        return name.ifEmpty { rawName.trim() }
    }

    private fun cleanMusicString(value: String): String {
        if (value.isEmpty()) return ""
        return java.text.Normalizer.normalize(value.lowercase(), java.text.Normalizer.Form.NFKD)
            .replace(Regex("[\\u0300-\\u036f]"), "")
            .replace(
                Regex("\\s*\\((?:official|video|audio|lyrics|lyric|remastered|remaster|hd|4k|hq|visualizer|clip|feat\\.?|ft\\.?).*?\\)", RegexOption.IGNORE_CASE),
                "",
            )
            .replace(
                Regex("\\s*\\[(?:official|video|audio|lyrics|lyric|remastered|remaster|hd|4k|hq|visualizer|clip|feat\\.?|ft\\.?).*?\\]", RegexOption.IGNORE_CASE),
                "",
            )
            .replace(
                Regex("\\s*-\\s*(?:official|video|audio|lyrics|lyric|remastered|remaster).*$", RegexOption.IGNORE_CASE),
                "",
            )
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
    }

    /** Exact port of MCat Player's areTracksDuplicate(). */
    fun areTracksDuplicate(
        candidateTitle: String,
        candidateArtist: String,
        existingTitle: String,
        existingArtist: String,
    ): Boolean {
        val cTitle = cleanMusicString(candidateTitle)
        val eTitle = cleanMusicString(existingTitle)
        if (cTitle.isEmpty() || eTitle.isEmpty()) return false
        val cArtist = cleanMusicString(candidateArtist)
        val eArtist = cleanMusicString(existingArtist)
        if (cTitle == eTitle) {
            if (cArtist.isEmpty() || eArtist.isEmpty() ||
                cArtist == "artista desconhecido" || eArtist == "artista desconhecido" ||
                cArtist.contains(eArtist) || eArtist.contains(cArtist)
            ) return true
        }
        val cFull = "$cArtist $cTitle".trim()
        val eFull = "$eArtist $eTitle".trim()
        if (cFull == eFull) return true
        if (cArtist.isNotEmpty() && eFull.contains(cTitle) && eFull.contains(cArtist)) return true
        if (eArtist.isNotEmpty() && cFull.contains(eTitle) && cFull.contains(eArtist)) return true
        val cReversed = "$cTitle $cArtist".trim()
        val eReversed = "$eTitle $eArtist".trim()
        if (cReversed == eFull || eReversed == cFull) return true
        return false
    }

    fun isSameRecording(candidate: Track, existing: Track): Boolean =
        areTracksDuplicate(candidate.title, candidate.artist, existing.title, existing.artist)

    fun findEquivalentCandidate(candidate: Track, candidates: List<Track>): Int =
        candidates.indexOfFirst { isSameRecording(candidate, it) }
}
