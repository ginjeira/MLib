package app.spotygram

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelegramOnlineTest {
    @Test
    fun updatingCountsAsOnline() {
        assertTrue(telegramOnline(""))
        assertTrue(telegramOnline("connectionStateUpdating"))
        assertFalse(telegramOnline("connectionStateWaitingForNetwork"))
        assertFalse(telegramOnline("connectionStateConnecting"))
        assertFalse(telegramOnline("connectionStateConnectingToProxy"))
    }
}
