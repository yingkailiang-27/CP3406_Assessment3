package edu.algosteps.content

import java.io.InputStream
import org.json.JSONArray
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
        validateLesson(lesson, "Lesson ${index + 1}")
        title
    }
}

private fun validateLesson(lesson: JSONObject, path: String) {
    for (field in listOf("difficulty", "summary", "idea", "invariant", "complexity", "prompt", "starter", "reference")) {
        lesson.requiredText(field, path)
    }
    for (field in listOf("inputLabel", "targetLabel")) {
        if (!lesson.isNull(field)) lesson.requiredText(field, path)
    }

    val notes = lesson.requiredArray("notes", path, 1..20)
    for (index in 0 until notes.length()) {
        require((notes.opt(index) as? String)?.isNotBlank() == true) {
            "$path.notes[${index + 1}] must contain text."
        }
    }

    val prediction = lesson.optJSONObject("prediction")
        ?: throw IllegalArgumentException("$path needs a prediction question.")
    val predictionPath = "$path.prediction"
    for (field in listOf("prompt", "explanation", "hint")) {
        prediction.requiredText(field, predictionPath)
    }
    val correct = prediction.requiredText("correctOption", predictionPath)
    val incorrect = prediction.requiredText("incorrectOption", predictionPath)
    require(!correct.equals(incorrect, ignoreCase = true)) {
        "$predictionPath must have two different options."
    }

    for (field in listOf("samples", "checks")) {
        val limits = if (field == "samples") 2..2 else 1..20
        val cases = lesson.requiredArray(field, path, limits)
        for (index in 0 until cases.length()) {
            val casePath = "$path.$field[${index + 1}]"
            val case = cases.optJSONObject(index)
                ?: throw IllegalArgumentException("$casePath must be an object.")
            validateCase(case, casePath, hasTrace = field == "samples")
        }
    }
}

private fun validateCase(case: JSONObject, path: String, hasTrace: Boolean) {
    val nums = case.requiredArray("nums", path, 0..100)
    require((0 until nums.length()).all { nums.opt(it) is Int }) { "$path.nums must contain integers." }
    for (field in listOf("target", "expected")) {
        require(case.opt(field) is Int) { "$path.$field must be an integer." }
    }
    if (!hasTrace) return

    val steps = case.requiredArray("trace", path, 1..100)
    for (index in 0 until steps.length()) {
        val stepPath = "$path.trace[${index + 1}]"
        val step = steps.optJSONObject(index)
            ?: throw IllegalArgumentException("$stepPath must be an object.")
        step.requiredText("title", stepPath)
        step.requiredText("detail", stepPath)
        val active = step.requiredArray("active", stepPath, 0..nums.length())
        require((0 until active.length()).all { (active.opt(it) as? Int)?.let { value -> value in 0 until nums.length() } == true }) {
            "$stepPath.active contains an index outside the input list."
        }
    }
}

private fun JSONObject.requiredText(field: String, path: String): String {
    val text = (opt(field) as? String).orEmpty().trim()
    require(text.isNotEmpty()) { "$path.$field must contain text." }
    return text
}

private fun JSONObject.requiredArray(field: String, path: String, size: IntRange): JSONArray {
    val array = optJSONArray(field)
        ?: throw IllegalArgumentException("$path.$field must be an array.")
    require(array.length() in size) { "$path.$field must contain ${size.first} to ${size.last} entries." }
    return array
}
