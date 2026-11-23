package app.spotygram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ByteRangesTest {
    @Test
    fun missingHeaderMeansWholeFile() {
        assertTrue(ByteRanges.parse(null, 100) is ByteRanges.Result.None)
        assertTrue(ByteRanges.parse("", 100) is ByteRanges.Result.None)
    }

    @Test
    fun openAndBoundedRanges() {
        val open = ByteRanges.parse("bytes=10-", 100) as ByteRanges.Result.Valid
        assertEquals(10, open.start)
        assertEquals(99, open.end)
        assertEquals(90, open.length)
        val bounded = ByteRanges.parse("bytes=10-19", 100) as ByteRanges.Result.Valid
        assertEquals(10, bounded.start)
        assertEquals(19, bounded.end)
        assertEquals(10, bounded.length)
    }

    @Test
    fun endIsClampedToTheFileSize() {
        val clamped = ByteRanges.parse("bytes=90-999", 100) as ByteRanges.Result.Valid
        assertEquals(90, clamped.start)
        assertEquals(99, clamped.end)
    }

    @Test
    fun suffixRangesReadTheTail() {
        val suffix = ByteRanges.parse("bytes=-10", 100) as ByteRanges.Result.Valid
        assertEquals(90, suffix.start)
        assertEquals(99, suffix.end)
        assertEquals(10, suffix.length)
        val larger = ByteRanges.parse("bytes=-1000", 100) as ByteRanges.Result.Valid
        assertEquals(0, larger.start)
        assertEquals(99, larger.end)
    }

    @Test
    fun invalidRangesAreRejected() {
        assertTrue(ByteRanges.parse("bytes=100-", 100) is ByteRanges.Result.Invalid)
        assertTrue(ByteRanges.parse("bytes=20-10", 100) is ByteRanges.Result.Invalid)
        assertTrue(ByteRanges.parse("bytes=-", 100) is ByteRanges.Result.Invalid)
        assertTrue(ByteRanges.parse("items=0-1", 100) is ByteRanges.Result.Invalid)
        assertTrue(ByteRanges.parse("bytes=0-1", 0) is ByteRanges.Result.Invalid)
    }
}
