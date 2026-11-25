package edu.algosteps.content

import java.io.InputStream
import org.json.JSONObject

private const val MAX_PACKAGE_BYTES = 256 * 1024

internal fun readLessonTitles(input: InputStream, existingIds: Set<String>): List<String> {
    val bytes = ByteArray(MAX_PACKAGE_BYTES + 1)
    var size = 0
    while (size < bytes.size) {
        val count = input.read(bytes, size, bytes.size - size)
        if (count == -1) break
        size += count
    }
    require(size <= MAX_PACKAGE_BYTES) { "Choose a course package smaller than 256 KB." }

    val coursePackage = JSONObject(String(bytes, 0, size, Charsets.UTF_8))
    require(coursePackage.opt("schemaVersion") == 1) { "This course package version is unsupported." }
    val lessons = coursePackage.getJSONArray("lessons")
    require(lessons.length() in 1..20) { "A package must contain between 1 and 20 lessons." }

    val ids = existingIds.toMutableSet()
    return List(lessons.length()) { index ->
        val lesson = lessons.getJSONObject(index)
        val id = (lesson.opt("id") as? String).orEmpty()
        val title = (lesson.opt("title") as? String).orEmpty().trim()
        require(id.matches(Regex("[a-z0-9]+(-[a-z0-9]+)*")) && id.length <= 80) {
            "Lesson ${index + 1} has an invalid ID."
        }
        require(title.length in 1..120) { "Lesson ${index + 1} needs a title of 1 to 120 characters." }
        require(ids.add(id)) { "Duplicate lesson ID: $id." }
        title
    }
}
