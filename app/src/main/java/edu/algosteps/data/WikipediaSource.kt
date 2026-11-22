package edu.algosteps.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Only a fixed public article title is sent. Source code, scores and settings stay on device. */
internal class WikipediaSource : ReadingSource {
    private val topics = mapOf(
        "linear-search" to "Linear search",
        "count-occurrences" to "Frequency (statistics)",
        "binary-search" to "Binary search",
        "window-sum" to "Moving average"
    )

    override suspend fun fetch(lessonId: String): Reading? = withContext(Dispatchers.IO) {
        val title = topics[lessonId] ?: return@withContext null
        val endpoint = Uri.parse("https://en.wikipedia.org/w/api.php").buildUpon()
            .appendQueryParameter("action", "query")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("formatversion", "2")
            .appendQueryParameter("prop", "extracts|info")
            .appendQueryParameter("inprop", "url")
            .appendQueryParameter("exintro", "1")
            .appendQueryParameter("explaintext", "1")
            .appendQueryParameter("exchars", "700")
            .appendQueryParameter("redirects", "1")
            .appendQueryParameter("titles", title)
            .build()
        val connection = URL(endpoint.toString()).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 7000
            connection.readTimeout = 7000
            connection.setRequestProperty("User-Agent", "AlgoSteps-Android/1.0 (educational app; topic readings)")
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(1024)
                val text = StringBuilder()
                while (true) {
                    val read = reader.read(buffer)
                    if (read == -1) break
                    if (text.length + read > 64_000) throw IOException("Reading response too large")
                    text.append(buffer, 0, read)
                }
                text.toString()
            }
            parseReading(lessonId, response)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseReading(lessonId: String, response: String): Reading? {
        val page = try {
            JSONObject(response).optJSONObject("query")?.optJSONArray("pages")?.optJSONObject(0)
        } catch (_: JSONException) {
            null
        } ?: return null
        if (page.has("missing")) return null
        val title = page.optString("title").trim()
        val extract = page.optString("extract").trim()
        val uri = Uri.parse(page.optString("fullurl"))
        if (title.isBlank() || extract.isBlank() || uri.scheme != "https" || uri.host != "en.wikipedia.org") {
            return null
        }
        return Reading(lessonId, title, extract, uri.toString(), System.currentTimeMillis())
    }
}
