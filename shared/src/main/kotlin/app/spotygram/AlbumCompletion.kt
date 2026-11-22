package app.spotygram

/** Track-list matching used by the “Complete album” flow. */
object AlbumCompletion {
    fun normalize(title: String): String =
        title.substringBefore(" (")
            .substringBefore(" [")
            .lowercase()
            .filter(Char::isLetterOrDigit)

    fun missing(expected: List<String>, existing: List<String>): List<String> {
        val have = existing.mapTo(mutableSetOf(), ::normalize)
        val seen = mutableSetOf<String>()
        return expected.filter { title ->
            val key = normalize(title)
            key.isNotBlank() && seen.add(key) && key !in have
        }
    }
}
