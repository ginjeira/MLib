package app.spotygram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TdRoutingTest {
    @Test
    fun routesToTheOwningClient() {
        val seen = mutableListOf<String>()
        val client =
            TdRouting.route("""{"@client_id":7,"@type":"updateConnectionState"}""") { id, text ->
                if (id == 7) {
                    seen += text
                    true
                } else {
                    false
                }
            }
        assertEquals(7, client)
        assertEquals(1, seen.size)
    }

    @Test
    fun ignoresUpdatesWithoutAnOwner() {
        assertNull(TdRouting.route("""{"@type":"updateOption"}""") { _, _ -> true })
        assertNull(TdRouting.route("not json") { _, _ -> true })
        assertNull(TdRouting.route("""{"@client_id":3}""") { _, _ -> false })
    }

    @Test
    fun clientIdIsParsedWhenPresent() {
        assertEquals(12, TdRouting.clientId("""{"@client_id":12}"""))
        assertNull(TdRouting.clientId("{}"))
    }
}
