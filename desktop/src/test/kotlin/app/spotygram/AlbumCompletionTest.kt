package app.spotygram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlbumCompletionTest {
    @Test
    fun missingIgnoresCaseAndEditionSuffixes() {
        val expected = listOf("One More Time (Remastered 2011)", "Aerodynamic [Live]")
        val existing = listOf("ONE MORE TIME", "aerodynamic")
        assertEquals(emptyList<String>(), AlbumCompletion.missing(expected, existing))
    }

    @Test
    fun missingKeepsTracksTheLibraryDoesNotHave() {
        val expected = listOf("One More Time", "Digital Love", "Aerodynamic")
        val existing = listOf("One More Time")
        assertEquals(
            listOf("Digital Love", "Aerodynamic"),
            AlbumCompletion.missing(expected, existing),
        )
    }

    @Test
    fun missingDropsRepeatedAndBlankTitles() {
        val expected = listOf("Digital Love", "digital love", "  ", "Aerodynamic")
        assertEquals(
            listOf("Digital Love", "Aerodynamic"),
            AlbumCompletion.missing(expected, emptyList()),
        )
    }

    @Test
    fun missingTreatsPunctuationAsEquivalent() {
        val expected = listOf("Don't Stop Me Now")
        assertEquals(
            emptyList<String>(),
            AlbumCompletion.missing(expected, listOf("Dont Stop Me Now")),
        )
    }

    @Test
    fun blankExpectedProducesNoMissingTracks() {
        assertTrue(AlbumCompletion.missing(emptyList(), listOf("Anything")).isEmpty())
    }
}
