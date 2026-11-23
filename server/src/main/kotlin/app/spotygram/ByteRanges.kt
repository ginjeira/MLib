package app.spotygram

/** HTTP byte-range parsing for media responses. */
object ByteRanges {
    sealed interface Result {
        data object None : Result

        data class Valid(val start: Long, val end: Long) : Result {
            val length: Long get() = end - start + 1
        }

        data object Invalid : Result
    }

    fun parse(header: String?, size: Long): Result {
        if (header.isNullOrBlank()) return Result.None
        if (size <= 0) return Result.Invalid
        val match = Regex("bytes=(\\d*)-(\\d*)").matchEntire(header.trim()) ?: return Result.Invalid
        val startText = match.groupValues[1]
        val endText = match.groupValues[2]
        if (startText.isEmpty()) {
            val suffix = endText.toLongOrNull() ?: return Result.Invalid
            if (suffix <= 0) return Result.Invalid
            val length = suffix.coerceAtMost(size)
            return Result.Valid(size - length, size - 1)
        }
        val start = startText.toLongOrNull() ?: return Result.Invalid
        if (start !in 0 until size) return Result.Invalid
        val end = if (endText.isEmpty()) size - 1 else endText.toLongOrNull() ?: return Result.Invalid
        if (end < start) return Result.Invalid
        return Result.Valid(start, end.coerceAtMost(size - 1))
    }
}
