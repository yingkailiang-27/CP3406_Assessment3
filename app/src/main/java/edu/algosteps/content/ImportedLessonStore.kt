package edu.algosteps.content

import android.content.Context
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

internal class ImportedLessonStore(context: Context) {
    private val preferences = context.getSharedPreferences("imported_lessons", Context.MODE_PRIVATE)

    fun load(): List<ImportedLesson> = synchronized(lock) { readLessons() }

    fun add(lessons: List<ImportedLesson>): List<ImportedLesson> = synchronized(lock) {
        val current = readLessons()
        val ids = Curriculum.lessons.mapTo(mutableSetOf()) { it.id }
        current.forEach { ids.add(it.id) }
        for (lesson in lessons) {
            require(ids.add(lesson.id)) { "Duplicate lesson ID: ${lesson.id}." }
        }

        val updated = current + lessons
        val json = JSONArray().apply {
            updated.forEach { put(JSONObject(it.json)) }
        }.toString()
        require(json.toByteArray(Charsets.UTF_8).size <= 1024 * 1024) {
            "The imported lesson library has reached its storage limit."
        }
        if (!preferences.edit().putString("lessons", json).commit()) {
            throw IOException("Could not save the course package.")
        }
        updated
    }

    private fun readLessons(): List<ImportedLesson> {
        val json = JSONArray(preferences.getString("lessons", "[]"))
        return List(json.length()) { ImportedLesson.fromJson(json.getJSONObject(it)) }
    }

    private companion object {
        val lock = Any()
    }
}
