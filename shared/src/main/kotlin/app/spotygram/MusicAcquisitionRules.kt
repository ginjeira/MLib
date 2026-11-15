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

    fun cleanCollectionTitle(value: String): String {
        val cleaned = value.trim()
            .replace(
                Regex("\\s*[\\(\\[]\\s*\\d{1,3}\\s*(?:🎵|🎶|tracks?|faixas?|musicas?)?\\s*[\\)\\]]\\s*$", RegexOption.IGNORE_CASE),
                "",
            )
            .trim()
        return cleaned.ifBlank { value.trim() }
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
        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)
        for (i in left.indices) {
            current[0] = i + 1
            for (j in right.indices) {
                val substitution = if (left[i] == right[j]) 0 else 1
                current[j + 1] = minOf(
                    previous[j + 1] + 1,
                    current[j] + 1,
                    previous[j] + substitution,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return 1.0 - previous[right.length].toDouble() / longest
    }
}
