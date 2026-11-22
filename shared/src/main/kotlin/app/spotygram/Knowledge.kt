package app.spotygram

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class KnowledgeRelease(
    val date: String,
    val kind: String,
    val url: String,
    val genres: String,
)

data class KnowledgeInfo(
    val summary: String,
    val sourceNames: String,
    val sourceUrl: String,
    val releaseDate: String = "",
    val kind: String = "",
    val genres: String = "",
)

object Knowledge {
    suspend fun album(artist: String, title: String): KnowledgeInfo? = withContext(Dispatchers.IO) {
        val term = listOf(artist, title).filter(String::isNotBlank).joinToString(" ").trim()
        if (term.isBlank()) return@withContext null
        val mb = quietly { musicBrainz(artist, title) }
        val wiki = quietly { wikipedia(term) }
        if (mb == null && wiki == null) return@withContext null
        return@withContext KnowledgeInfo(
            summary = wiki?.first.orEmpty(),
            sourceNames = listOfNotNull(
                "MusicBrainz".takeIf { mb != null },
                "Wikipédia".takeIf { wiki != null },
            ).joinToString(" · "),
            sourceUrl = wiki?.second ?: mb?.url.orEmpty(),
            releaseDate = mb?.date.orEmpty(),
            kind = mb?.kind.orEmpty(),
            genres = mb?.genres.orEmpty(),
        )
    }

    private suspend fun <T> quietly(block: suspend () -> T?): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }


    private fun fetch(url: String): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty(
                "User-Agent",
                "MLib/1.0 (https://github.com/ginjeira/MLib)",
            )
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            require(body.length <= 1_000_000) { "Response too large" }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun musicBrainz(
        artist: String,
        title: String,
    ): KnowledgeRelease? {
        val query = buildString {
            append("releasegroup:\"")
            append(title.replace("\"", ""))
            append("\"")
            if (artist.isNotBlank()) {
                append(" AND artist:\"")
                append(artist.replace("\"", ""))
                append("\"")
            }
        }
        val json =
            fetch(
                "https://musicbrainz.org/ws/2/release-group/?query=${encode(query)}&fmt=json&limit=1"
            )
        val group = json.optJSONArray("release-groups")?.optJSONObject(0) ?: return null
        val date = group.optString("first-release-date").take(10)
        val kind = group.optString("primary-type")
        val id = group.optString("id")
        val genres =
            group.optJSONArray("tags")
                ?.let { tags ->
                    buildList {
                        for (i in 0 until minOf(tags.length(), 4)) {
                            tags.optJSONObject(i)?.optString("name")?.takeIf(String::isNotBlank)
                                ?.let(::add)
                        }
                    }.joinToString(", ")
                }
                .orEmpty()
        return KnowledgeRelease(
            date = date,
            kind = kind,
            url = if (id.isBlank()) "" else "https://musicbrainz.org/release-group/$id",
            genres = genres,
        )
    }

    private fun wikipedia(term: String): Pair<String, String>? {
        val search =
            fetch(
                "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${encode(term)}&format=json&srlimit=1"
            )
        val page = search.optJSONObject("query")?.optJSONArray("search")?.optJSONObject(0)
            ?.optString("title").orEmpty()
        if (page.isBlank()) return null
        val summary =
            fetch("https://en.wikipedia.org/api/rest_v1/page/summary/${encode(page.replace(' ', '_'))}")
        val extract = summary.optString("extract")
        if (extract.isBlank()) return null
        val url =
            summary.optJSONObject("content_urls")?.optJSONObject("desktop")?.optString("page")
                .orEmpty()
                .ifBlank { "https://en.wikipedia.org/wiki/${encode(page.replace(' ', '_'))}" }
        return extract to url
    }
}
