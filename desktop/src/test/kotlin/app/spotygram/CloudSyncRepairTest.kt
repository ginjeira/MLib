package app.spotygram

import org.junit.Assert.assertEquals
import org.junit.Test

class CloudSyncRepairTest {
    @Test
    fun ignoredChannelsMergeWithoutZeroAndStaySorted() {
        assertEquals("1,2,3", mergeIgnoredChannels("2,1", listOf(0L, 3L, 2L)))
        assertEquals("5", mergeIgnoredChannels("", listOf(5L)))
        assertEquals(emptySet<Long>(), ignoredChannelIds(""))
        assertEquals(setOf(7L, 8L), ignoredChannelIds("7,8,7"))
    }
}
