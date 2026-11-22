package app.spotygram

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class KnowledgeRelease(
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
    val fetchedAt: Long = 0,
)

/** Parses a MusicBrainz release-group search response. */
fun parseReleaseGroup(json: JSONObject): KnowledgeRelease? {
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
    if (date.isBlank() && kind.isBlank() && genres.isBlank()) return null
    return KnowledgeRelease(
        date = date,
        kind = kind,
        url = if (id.isBlank()) "" else "https://musicbrainz.org/release-group/$id",
        genres = genres,
    )
}

/** Parses the first page title of a Wikipedia search response. */
fun parseWikipediaPage(search: JSONObject): String? =
    search.optJSONObject("query")
        ?.optJSONArray("search")
        ?.optJSONObject(0)
        ?.optString("title")
        ?.takeIf(String::isNotBlank)

/** Parses a Wikipedia REST summary, falling back to a generated article URL. */
fun parseWikipediaSummary(
    summary: JSONObject,
    fallbackUrl: String,
): Pair<String, String>? {
    val extract = summary.optString("extract").takeIf(String::isNotBlank) ?: return null
    val url =
        summary.optJSONObject("content_urls")?.optJSONObject("desktop")?.optString("page")
            .orEmpty()
            .ifBlank { fallbackUrl }
    return extract to url
}

/** Parses a MusicBrainz artist search response into dates, type and genres. */
fun parseArtist(json: JSONObject): KnowledgeRelease? {
    val artist = json.optJSONArray("artists")?.optJSONObject(0) ?: return null
    val lifeSpan = artist.optJSONObject("life-span")
    val begin = lifeSpan?.optString("begin").orEmpty().take(10)
    val end = lifeSpan?.optString("end").orEmpty().take(10)
    val date = listOf(begin, end).filter(String::isNotBlank).joinToString("–")
    val kind = artist.optString("type")
    val id = artist.optString("id")
    val genres =
        artist.optJSONArray("tags")
            ?.let { tags ->
                buildList {
                    for (i in 0 until minOf(tags.length(), 4)) {
                        tags.optJSONObject(i)?.optString("name")?.takeIf(String::isNotBlank)
                            ?.let(::add)
                    }
                }.joinToString(", ")
            }
            .orEmpty()
    if (date.isBlank() && kind.isBlank() && genres.isBlank()) return null
    return KnowledgeRelease(
        date = date,
        kind = kind,
        url = if (id.isBlank()) "" else "https://musicbrainz.org/artist/$id",
        genres = genres,
    )
}

object Knowledge {
    private const val MB_SOURCE = "MusicBrainz (CC0)"
    private const val WIKI_SOURCE = "Wikipédia (CC BY-SA)"

    /** Optional on-disk cache; hosts set this once at startup. */
    @Volatile
    var cache: KnowledgeCache? = null

    suspend fun album(artist: String, title: String): KnowledgeInfo? = withContext(Dispatchers.IO) {
        val term = listOf(artist, title).filter(String::isNotBlank).joinToString(" ").trim()
        if (term.isBlank()) return@withContext null
        val key = listOf(artist, title).joinToString("|").trim().lowercase()
        cache?.get(key)?.let { return@withContext it }
        val mb = quietly { musicBrainz(artist, title) }
        val wiki = quietly { wikipedia(term) }
        if (mb == null && wiki == null) return@withContext null
        val info =
            KnowledgeInfo(
                summary = wiki?.first.orEmpty(),
                sourceNames = listOfNotNull(
                    MB_SOURCE.takeIf { mb != null },
                    WIKI_SOURCE.takeIf { wiki != null },
                ).joinToString(" · "),
                sourceUrl = wiki?.second ?: mb?.url.orEmpty(),
                releaseDate = mb?.date.orEmpty(),
                kind = mb?.kind.orEmpty(),
                genres = mb?.genres.orEmpty(),
                fetchedAt = System.currentTimeMillis(),
            )
        cache?.put(key, info)
        info
    }

    suspend fun artist(name: String): KnowledgeInfo? = withContext(Dispatchers.IO) {
        val term = name.trim()
        if (term.isBlank()) return@withContext null
        val key = "artist|${term.lowercase()}"
        cache?.get(key)?.let { return@withContext it }
        val mb = quietly { musicBrainzArtist(term) }
        val wiki = quietly { wikipedia(term) }
        if (mb == null && wiki == null) return@withContext null
        val info =
            KnowledgeInfo(
                summary = wiki?.first.orEmpty(),
                sourceNames = listOfNotNull(
                    MB_SOURCE.takeIf { mb != null },
                    WIKI_SOURCE.takeIf { wiki != null },
                ).joinToString(" · "),
                sourceUrl = wiki?.second ?: mb?.url.orEmpty(),
                releaseDate = mb?.date.orEmpty(),
                kind = mb?.kind.orEmpty(),
                genres = mb?.genres.orEmpty(),
                fetchedAt = System.currentTimeMillis(),
            )
        cache?.put(key, info)
        info
    }

    private fun musicBrainzArtist(name: String): KnowledgeRelease? {
        val query = "artist:\"" + name.replace("\"", "") + "\""
        val json =
            fetch(
                "https://musicbrainz.org/ws/2/artist/?query=${encode(query)}&fmt=json&limit=1"
            )
        return parseArtist(json)
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
        return parseReleaseGroup(json)
    }

    private fun wikipedia(term: String): Pair<String, String>? {
        val search =
            fetch(
                "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=${encode(term)}&format=json&srlimit=1"
            )
        val page = parseWikipediaPage(search) ?: return null
        val slug = encode(page.replace(' ', '_'))
        val summary = fetch("https://en.wikipedia.org/api/rest_v1/page/summary/$slug")
        return parseWikipediaSummary(summary, "https://en.wikipedia.org/wiki/$slug")
    }
}

/** On-disk cache for album knowledge, keyed by artist and title with a TTL. */
class KnowledgeCache(
    private val file: java.io.File?,
    private val ttlMillis: Long = 30L * 24 * 60 * 60 * 1000,
    private val maxEntries: Int = 300,
) {
    private fun read(): JSONObject =
        try {
            val text = file?.takeIf { it.isFile }?.readText().orEmpty()
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } catch (_: Exception) {
            JSONObject()
        }

    fun get(key: String, now: Long = System.currentTimeMillis()): KnowledgeInfo? {
        if (file == null) return null
        val entry = read().optJSONObject("entries")?.optJSONObject(key) ?: return null
        val at = entry.optLong("at", 0)
        if (at <= 0 || now - at > ttlMillis) return null
        val info = entry.optJSONObject("info") ?: return null
        val summary = info.optString("summary")
        val date = info.optString("date")
        val kind = info.optString("kind")
        if (summary.isBlank() && date.isBlank() && kind.isBlank()) return null
        return KnowledgeInfo(
            summary = summary,
            sourceNames = info.optString("sources"),
            sourceUrl = info.optString("url"),
            releaseDate = date,
            kind = kind,
            genres = info.optString("genres"),
            fetchedAt = info.optLong("fetched", at),
        )
    }

    fun put(key: String, info: KnowledgeInfo, now: Long = System.currentTimeMillis()) {
        val target = file ?: return
        if (info.summary.isBlank() && info.releaseDate.isBlank() && info.kind.isBlank()) return
        val root = read()
        val entries = root.optJSONObject("entries") ?: JSONObject().also { root.put("entries", it) }
        entries.put(
            key,
            JSONObject().put("at", now).put(
                "info",
                JSONObject()
                    .put("summary", info.summary)
                    .put("sources", info.sourceNames)
                    .put("url", info.sourceUrl)
                    .put("date", info.releaseDate)
                    .put("kind", info.kind)
                    .put("genres", info.genres)
                    .put("fetched", info.fetchedAt),
            ),
        )
        while (entries.length() > maxEntries) {
            val oldest =
                entries.keys().asSequence().minByOrNull {
                    entries.optJSONObject(it)?.optLong("at", 0) ?: 0
                } ?: break
            entries.remove(oldest)
        }
        try {
            target.parentFile?.mkdirs()
            target.writeText(root.put("version", 1).toString())
        } catch (_: Exception) {
            // A cache write failure must never break the lookup.
        }
    }
}
