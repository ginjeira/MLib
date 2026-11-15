package app.spotygram

/**
 * Pure parsing and matching rules shared by the native TDLib acquisition flow.
 * Kept separate so the bot protocol can be regression-tested without a live account.
 */
internal data class ParsedMusicName(val artist: String, val title: String)

internal object MusicAcquisitionRules {
    fun parseResults(text: String): List<AcquisitionOption> =
        text.lineSequence().mapNotNull { line ->
            Regex("^\\s*(\\d+)\\.\\s*(.+)$").find(line)?.let { match ->
                AcquisitionOption(match.groupValues[1].toInt(), match.groupValues[2].trim())
            }
        }.distinctBy { it.number }.toList()

    fun normalizeMusicText(value: String): String =
        java.text.Normalizer.normalize(value.lowercase(), java.text.Normalizer.Form.NFKD)
            .replace(Regex("[\\u0300-\\u036f]"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

    fun cleanCollectionTitle(value: String): String {
        val cleaned = value.trim()
            .replace(
                Regex("\\s*[\\(\\[]\\s*\\d{1,3}\\s*(?:🎵|🎶|tracks?|faixas?|musicas?)?\\s*[\\)\\]]\\s*$", RegexOption.IGNORE_CASE),
                "",
            )
            .replace(Regex("[\\\\p{So}\\\\p{Sk}]+\\\\s*$"), "")
            .replace(Regex("^\\\\s*\\\\d+[.\\\\-)]\\\\s*"), "")
            .trim()
        return cleaned.ifBlank { value.trim() }
    }

    fun parseArtistAndTitle(raw: String): ParsedMusicName {
        val clean = cleanCollectionTitle(raw)
            .replace(Regex("^\\s*\\d+[.)-]\\s*"), "")
            .trim()
        val by = Regex("^(.*?)\\s+by\\s+(.*?)$", RegexOption.IGNORE_CASE).matchEntire(clean)
        if (by != null && by.groupValues[1].isNotBlank() && by.groupValues[2].isNotBlank()) {
            return ParsedMusicName(by.groupValues[2].trim(), by.groupValues[1].trim())
        }
        val separator = Regex("\\s+[-–—]\\s+").find(clean)
        if (separator != null) {
            val artist = clean.substring(0, separator.range.first).trim()
            val title = clean.substring(separator.range.last + 1).trim()
            if (artist.isNotBlank() && title.isNotBlank()) return ParsedMusicName(artist, title)
        }
        return ParsedMusicName("", clean)
    }

    fun sanitizeSearchQuery(value: String): String {
        var query = java.text.Normalizer.normalize(value.trim(), java.text.Normalizer.Form.NFKC)
        query = query.replace(
            Regex("[\\(\\[]\\s*\\d+\\s*(?:🎵|🎶|track|tracks|faixas?)?\\s*[\\)\\]]", RegexOption.IGNORE_CASE),
            " ",
        )
        query = query.replace(Regex("\\.(mp3|flac|m4a|wav|aac|ogg|wma)$", RegexOption.IGNORE_CASE), "")
        query = query.replace(Regex("[\\p{So}\\p{Sk}]"), " ")
        query = query.replace(Regex("[()\\[\\]{}【】《》〔〕「」『』]"), " ")
        query = query.replace(Regex("^\\s*\\d+[.\\-)]\\s*"), " ")
        return query.replace(Regex("\\s+"), " ").trim()
    }

    fun findEquivalentCandidate(candidate: Track, candidates: List<Track>): Int =
        candidates.indexOfFirst { isSameRecording(candidate, it) }

    /**
     * Metadata matching is deliberately conservative. A title alone is not enough:
     * require a matching artist or a matching known duration when Telegram file IDs differ.
     */
    fun isSameRecording(candidate: Track, existing: Track): Boolean {
        if (candidate.fileKey.isNotBlank() && existing.fileKey.isNotBlank() &&
            candidate.fileKey == existing.fileKey
        ) return true

        val candidateTitle = normalizeMusicText(candidate.title)
        val existingTitle = normalizeMusicText(existing.title)
        if (candidateTitle.isEmpty() || existingTitle.isEmpty()) return false

        val candidateArtist = normalizeMusicText(candidate.artist)
        val existingArtist = normalizeMusicText(existing.artist)
        val bothArtistsKnown = candidateArtist.isNotEmpty() && existingArtist.isNotEmpty()
        if (bothArtistsKnown && candidateArtist != existingArtist) return false

        val bothDurationsKnown = candidate.duration > 0 && existing.duration > 0
        val durationDifference = if (bothDurationsKnown)
            kotlin.math.abs(candidate.duration - existing.duration) else Int.MAX_VALUE
        val exactTitle = candidateTitle == existingTitle
        // Fuzzy title matching is deliberately gated by corroborating metadata:
        // same known artist and near-identical known duration. Never merge on
        // a similar title alone.
        val fuzzyTitle = !exactTitle &&
            bothArtistsKnown &&
            durationDifference <= 2 &&
            titleSimilarity(candidateTitle, existingTitle) >= 0.92
        if (!exactTitle && !fuzzyTitle) return false

        if (bothArtistsKnown) {
            return !bothDurationsKnown || durationDifference <= 3
        }

        if (bothDurationsKnown && durationDifference <= 2) return exactTitle

        val bothSizesKnown = candidate.size > 0 && existing.size > 0
        return exactTitle && bothSizesKnown && candidate.size == existing.size &&
            bothArtistsKnown && candidateArtist == existingArtist
    }

    private fun titleSimilarity(left: String, right: String): Double {
        if (left == right) return 1.0
        val longest = maxOf(left.length, right.length)
        if (longest == 0) return 1.0
        if (kotlin.math.abs(left.length - right.length) > (longest * 0.06).toInt().coerceAtLeast(1)) {
            return 0.0
        }

        // Optimal String Alignment distance treats a single adjacent character
        // transposition as one edit (e.g. "Tiem" -> "Time"), which is a common
        // filename/metadata typo. It remains gated by exact artist + duration.
        val distance = Array(left.length + 1) { IntArray(right.length + 1) }
        for (i in 0..left.length) distance[i][0] = i
        for (j in 0..right.length) distance[0][j] = j
        for (i in 1..left.length) {
            for (j in 1..right.length) {
                val substitution = if (left[i - 1] == right[j - 1]) 0 else 1
                var best = minOf(
                    distance[i - 1][j] + 1,
                    distance[i][j - 1] + 1,
                    distance[i - 1][j - 1] + substitution,
                )
                if (i > 1 && j > 1 &&
                    left[i - 1] == right[j - 2] &&
                    left[i - 2] == right[j - 1]
                ) {
                    best = minOf(best, distance[i - 2][j - 2] + 1)
                }
                distance[i][j] = best
            }
        }
        return 1.0 - distance[left.length][right.length].toDouble() / longest
    }
}
