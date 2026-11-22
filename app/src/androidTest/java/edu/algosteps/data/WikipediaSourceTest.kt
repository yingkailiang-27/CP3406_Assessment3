package edu.algosteps.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WikipediaSourceTest {
    private val source = WikipediaSource()

    @Test
    fun versionTwoResponseUsesCanonicalArticleUrlForAttribution() {
        val page = JSONObject()
            .put("title", "Linear search")
            .put("fullurl", "https://en.wikipedia.org/wiki/Linear_search")
            .put("extract", "A first-to-last search.")
        val reading = requireNotNull(source.parseReading("linear-search", response(page)))
        assertEquals("linear-search", reading.lessonId)
        assertEquals("Linear search", reading.title)
        assertEquals("A first-to-last search.", reading.extract)
        assertEquals("https://en.wikipedia.org/wiki/Linear_search", reading.url)
    }

    @Test
    fun malformedMissingOrExternalArticlesAreNotCached() {
        assertNull(source.parseReading("linear-search", "<html>Service busy</html>"))
        assertNull(source.parseReading("linear-search", response(JSONObject().put("missing", true))))
        val external = JSONObject()
            .put("title", "Linear search")
            .put("extract", "Text")
            .put("fullurl", "https://example.org/unrelated")
        assertNull(source.parseReading("linear-search", response(external)))
    }

    private fun response(page: JSONObject): String = JSONObject().put(
        "query", JSONObject().put("pages", JSONArray().put(page))
    ).toString()
}
