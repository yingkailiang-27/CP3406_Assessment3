package edu.algosteps.content

import java.io.InputStream
import org.json.JSONArray
import org.json.JSONObject

private const val MAX_PACKAGE_BYTES = 256 * 1024

internal data class ImportedLesson(
    val id: String,
    val title: String,
    val summary: String,
    val json: String
) {
    companion object {
        fun fromJson(lesson: JSONObject) = ImportedLesson(
            id = lesson.getString("id"),
            title = lesson.getString("title").trim(),
            summary = lesson.getString("summary").trim(),
            json = lesson.toString()
        )
    }
}

internal fun readLessonPackage(input: InputStream, existingIds: Set<String>): List<ImportedLesson> {
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
        ImportedLesson.fromJson(lesson)
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
        if (field == "samples") {
            val inputs = cases.objects().map { it.toCase() }.map { it.nums to it.target }
            require(inputs.distinct().size == inputs.size) { "$path.samples must use different inputs." }
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

internal data class LessonPrediction(
    val prompt: String,
    val correctOption: String,
    val incorrectOption: String,
    val explanation: String,
    val hint: String
)

internal fun ImportedLesson.toStudyLesson(): Pair<Lesson, LessonPrediction> {
    val data = JSONObject(json)
    val examples = data.getJSONArray("samples").objects()
    val samples = examples.map { it.toCase() }
    val traces = examples.map { example ->
        example.getJSONArray("trace").objects().map { step ->
            TraceStep(
                title = step.getString("title"),
                detail = step.getString("detail"),
                active = step.getJSONArray("active").integers().toSet()
            )
        }
    }
    val notes = data.getJSONArray("notes")
    val lesson = Lesson(
        id = id,
        title = title,
        summary = summary,
        difficulty = data.getString("difficulty"),
        prompt = data.getString("prompt"),
        starter = data.getString("starter"),
        reference = data.getString("reference"),
        notes = List(notes.length()) { notes.getString(it) },
        samples = samples,
        checks = data.getJSONArray("checks").objects().map { it.toCase() },
        idea = data.getString("idea"),
        invariant = data.getString("invariant"),
        complexity = data.getString("complexity"),
        targetLabel = (data.opt("targetLabel") as? String).orEmpty().trim(),
        trace = { example -> traces[samples.indexOfFirst { it === example }.takeIf { it >= 0 } ?: samples.indexOf(example)] }
    )
    val prediction = data.getJSONObject("prediction")
    return lesson to LessonPrediction(
        prompt = prediction.getString("prompt"),
        correctOption = prediction.getString("correctOption"),
        incorrectOption = prediction.getString("incorrectOption"),
        explanation = prediction.getString("explanation"),
        hint = prediction.getString("hint")
    )
}

private fun JSONArray.objects() = List(length()) { getJSONObject(it) }

private fun JSONArray.integers() = List(length()) { getInt(it) }

private fun JSONObject.toCase() = Case(
    nums = getJSONArray("nums").integers(),
    target = getInt("target"),
    expected = getInt("expected")
)
