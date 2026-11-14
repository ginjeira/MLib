package app.spotygram

/**
 * Pure parsing and matching rules shared by the native TDLib acquisition flow.
 * Kept separate so the bot protocol can be regression-tested without a live account.
 */
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
        if (candidateTitle.isEmpty() || candidateTitle != existingTitle) return false

        val candidateArtist = normalizeMusicText(candidate.artist)
        val existingArtist = normalizeMusicText(existing.artist)
        val bothArtistsKnown = candidateArtist.isNotEmpty() && existingArtist.isNotEmpty()
        if (bothArtistsKnown && candidateArtist != existingArtist) return false

        val bothDurationsKnown = candidate.duration > 0 && existing.duration > 0
        if (bothArtistsKnown) {
            return !bothDurationsKnown ||
                kotlin.math.abs(candidate.duration - existing.duration) <= 3
        }

        if (bothDurationsKnown &&
            kotlin.math.abs(candidate.duration - existing.duration) <= 2
        ) return true

        val bothSizesKnown = candidate.size > 0 && existing.size > 0
        return bothSizesKnown && candidate.size == existing.size &&
            bothArtistsKnown && candidateArtist == existingArtist
    }
}
