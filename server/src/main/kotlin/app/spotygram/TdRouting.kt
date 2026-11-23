package app.spotygram

import org.json.JSONObject

/** Pure routing of TDLib updates by @client_id; testable without native code. */
object TdRouting {
    fun clientId(text: String): Int? =
        try {
            JSONObject(text).optInt("@client_id", -1).takeIf { it >= 0 }
        } catch (_: Throwable) {
            null
        }

    fun route(text: String, offer: (Int, String) -> Boolean): Int? {
        val client = clientId(text) ?: return null
        return if (offer(client, text)) client else null
    }
}
