package app.spotygram

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeResolutionTest {
    @Test
    fun explicitModesPassThrough() {
        assertEquals("light", resolvedTheme("light", systemDark = true))
        assertEquals("dark", resolvedTheme("dark", systemDark = false))
        assertEquals("amoled", resolvedTheme("amoled", systemDark = false))
        assertEquals("contrast", resolvedTheme("contrast", systemDark = false))
    }

    @Test
    fun systemFollowsTheDevice() {
        assertEquals("dark", resolvedTheme("system", systemDark = true))
        assertEquals("light", resolvedTheme("system", systemDark = false))
    }

    @Test
    fun unknownModesFollowTheDevice() {
        assertEquals("dark", resolvedTheme("sepia", systemDark = true))
        assertEquals("light", resolvedTheme("", systemDark = false))
    }
}
