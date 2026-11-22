package app.spotygram

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KnowledgeParsingTest {
    @Test
    fun releaseGroupFixtureProducesDateTypeGenresAndLink() {
        val json =
            JSONObject(
                """
                {"release-groups":[{"id":"rg-1","first-release-date":"2001-03-12",
                "primary-type":"Album","tags":[{"name":"french house"},{"name":"disco"}]}]}
                """.trimIndent()
            )
        val release = parseReleaseGroup(json)
        assertEquals("2001-03-12", release?.date)
        assertEquals("Album", release?.kind)
        assertEquals("french house, disco", release?.genres)
        assertEquals("https://musicbrainz.org/release-group/rg-1", release?.url)
    }

    @Test
    fun releaseGroupWithoutTagArrayStillParses() {
        val json =
            JSONObject(
                """{"release-groups":[{"id":"rg-2","first-release-date":"1999","primary-type":"Single"}]}"""
            )
        assertEquals("Single", parseReleaseGroup(json)?.kind)
        assertEquals("", parseReleaseGroup(json)?.genres)
    }

    @Test
    fun emptyReleaseGroupSearchYieldsNull() {
        assertNull(parseReleaseGroup(JSONObject("""{"release-groups":[]}""")))
        assertNull(parseReleaseGroup(JSONObject("""{}""")))
    }

    @Test
    fun releaseGroupWithOnlyUnknownFieldsYieldsNull() {
        assertNull(parseReleaseGroup(JSONObject("""{"release-groups":[{"id":"rg-3"}]}""")))
    }

    @Test
    fun wikipediaSearchPicksFirstTitle() {
        val json =
            JSONObject(
                """{"query":{"search":[{"title":"Discovery (Daft Punk album)"},{"title":"Other"}]}}"""
            )
        assertEquals("Discovery (Daft Punk album)", parseWikipediaPage(json))
        assertNull(parseWikipediaPage(JSONObject("""{"query":{"search":[]}}""")))
    }

    @Test
    fun wikipediaSummaryUsesDesktopUrlWhenPresent() {
        val json =
            JSONObject(
                """
                {"extract":"Discovery is the second studio album by Daft Punk.",
                "content_urls":{"desktop":{"page":"https://en.wikipedia.org/wiki/Discovery"}}}
                """.trimIndent()
            )
        val parsed = parseWikipediaSummary(json, "https://en.wikipedia.org/wiki/Fallback")
        assertEquals("https://en.wikipedia.org/wiki/Discovery", parsed?.second)
        assertEquals("Discovery is the second studio album by Daft Punk.", parsed?.first)
    }

    @Test
    fun wikipediaSummaryFallsBackToGeneratedUrl() {
        val json = JSONObject("""{"extract":"A summary."}""")
        assertEquals(
            "https://en.wikipedia.org/wiki/Discovery",
            parseWikipediaSummary(json, "https://en.wikipedia.org/wiki/Discovery")?.second,
        )
    }

    @Test
    fun wikipediaSummaryWithoutExtractYieldsNull() {
        assertNull(parseWikipediaSummary(JSONObject("""{"content_urls":{}}"""), "fallback"))
    }
}
