package edu.algosteps.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.algosteps.content.Lesson
import edu.algosteps.data.StudyPreferences
import edu.algosteps.data.StudyRepository

@Composable
fun CodingScreen(lesson: Lesson, examMode: Boolean, repository: StudyRepository, onBack: () -> Unit) {
    val factory = remember(lesson.id, examMode, repository) {
        viewModelFactory { initializer { CodingViewModel(lesson, examMode, repository) } }
    }
    val vm: CodingViewModel = viewModel(key = "coding/${lesson.id}/$examMode", factory = factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val settings by vm.preferences.collectAsStateWithLifecycle(initialValue = StudyPreferences())
    BackHandler(enabled = state.running) { /* The submission is being graded or saved. */ }
    CodingContent(lesson, examMode, state, settings.showHints, vm::edit, vm::evaluate, vm::retryDraft, onBack)
}

@Composable
internal fun CodingContent(
    lesson: Lesson,
    examMode: Boolean,
    state: CodingState,
    showHints: Boolean,
    onEdit: (String) -> Unit,
    onEvaluate: (Boolean) -> Unit,
    onRetryDraft: () -> Unit,
    onBack: () -> Unit
) {
    var languageGuide by rememberSaveable(lesson.id, examMode) { mutableStateOf(false) }
    var explanation by rememberSaveable(lesson.id, examMode) { mutableStateOf(false) }
    val canSeeSolution = !examMode || state.lastAttempt != null
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack, enabled = !state.running) { Text("Back") }
        Text(if (examMode) "Exam" else "Coding practice", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        Text(lesson.title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
        Text(lesson.prompt, style = MaterialTheme.typography.bodyLarge)
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Examples", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                lesson.samples.forEachIndexed { index, case ->
                    Text("${index + 1}. solve(${case.nums}, ${case.target}) → ${case.expected}",
                        fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (!state.ready) {
            Text("Loading your draft…", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            return@Column
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CodeEditor(lesson.id, examMode, state.source, !state.running, onEdit)
                state.draftMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    if (it.startsWith("Couldn't save")) {
                        TextButton(onClick = onRetryDraft) { Text("Retry saving draft") }
                    }
                }
                TextButton(onClick = { languageGuide = !languageGuide }) {
                    Text(if (languageGuide) "Hide language guide" else "Supported Python syntax")
                }
                if (languageGuide) {
                    Text("Use integers, variables, len(nums), nums[i], arithmetic (+, -, *, //, %), comparisons, if/elif/else, while and for. Iterate over nums or range(...). Use spaces to indent and return one integer. Imports, print and changes to the input list are unavailable.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (!examMode && showHints) {
            Text("Hint · ${lesson.invariant}",
                style = MaterialTheme.typography.bodyMedium)
        }
        if (examMode && !canSeeSolution) {
            Text("Submit to record your score and unlock the code walkthrough.", style = MaterialTheme.typography.bodyMedium)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onEvaluate(false) }, enabled = !state.running && state.source.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) { Text("Run samples") }
            Button(
                onClick = { onEvaluate(true) },
                enabled = !state.running && state.source.isNotBlank() && !state.alreadySubmitted,
                modifier = Modifier.weight(1f)
            ) { Text(if (state.alreadySubmitted) "Submission recorded" else "Submit solution") }
        }
        if (state.running) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Checking your solution…", Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        state.storageMessage?.let { Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = MaterialTheme.colorScheme.error) }
        state.validationMessage?.let {
            Text(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, color = MaterialTheme.colorScheme.error)
        }
        state.report?.let { ResultCard(it, lesson, examMode) }
        if (state.report?.submitted != true) state.lastAttempt?.let { attempt ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Last saved submission: ${attempt.passed} of ${attempt.total} tests passed.")
                state.savedReport?.let { Text(it.additionalCheckSummary()) }
            }
        }

        if (canSeeSolution) {
            OutlinedButton(onClick = { explanation = !explanation }, modifier = Modifier.fillMaxWidth()) {
                Text(if (explanation) "Hide code walkthrough" else "Show code walkthrough")
            }
            if (explanation) {
                Text("Worked solution", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() })
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()) {
                    SelectionContainer {
                        Text(lesson.reference.lines().mapIndexed { index, line -> "${index + 1} │ $line" }.joinToString("\n"),
                            Modifier.horizontalScroll(rememberScrollState()).padding(12.dp),
                            fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, softWrap = false)
                    }
                }
                lesson.notes.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CodeEditor(
    lessonId: String, examMode: Boolean, initialSource: String, enabled: Boolean, onEdit: (String) -> Unit
) {
    var field by remember(lessonId, examMode) { mutableStateOf(TextFieldValue(initialSource)) }
    fun change(value: TextFieldValue) {
        field = value
        onEdit(value.text)
    }
    OutlinedTextField(
        value = field, onValueChange = ::change, readOnly = !enabled,
        label = { Text("Your Python code") },
        modifier = Modifier.fillMaxWidth().testTag("code-editor"),
        minLines = 5, maxLines = 13,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false,
            keyboardType = KeyboardType.Ascii)
    )
    TextButton(enabled = enabled, onClick = {
        val start = field.selection.min
        val updated = field.text.replaceRange(start, field.selection.max, "    ")
        change(TextFieldValue(updated, TextRange(start + 4)))
    }) { Text("Insert 4 spaces") }
}

@Composable
private fun ResultCard(report: Evaluation, lesson: Lesson, examMode: Boolean) {
    var reviewedSample by remember(report) { mutableStateOf<Int?>(null) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (report.submitted) "Submission results" else "Sample results",
                style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text("${report.passed} of ${report.total} tests passed",
                Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = MaterialTheme.typography.titleMedium)
            report.samples.forEachIndexed { index, result ->
                val summary = when {
                    result.passed -> "Passed · returned ${result.actual}"
                    result.error != null -> "Error · ${result.error}"
                    else -> "Failed · expected ${result.example.expected}, got ${result.actual}"
                }
                val colors = MaterialTheme.colorScheme
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = if (result.passed) colors.surfaceVariant else colors.errorContainer,
                    contentColor = if (result.passed) colors.onSurfaceVariant else colors.onErrorContainer
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Example ${index + 1}: $summary")
                        if (!examMode && !result.passed) {
                            val expanded = reviewedSample == index
                            OutlinedButton(
                                onClick = { reviewedSample = if (expanded) null else index },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (expanded) "Hide example ${index + 1} steps" else "Review example ${index + 1} steps")
                            }
                            if (expanded) Walkthrough(lesson, sampleIndex = index)
                        }
                    }
                }
            }
            if (report.submitted) {
                Text(report.additionalCheckSummary())
                if (report.additionalPassed < report.additionalTotal) {
                    Text("Review the input limits in the problem and try a boundary case on paper.")
                }
            } else {
                Text("Sample runs do not count toward your statistics.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun Evaluation.additionalCheckSummary() = "Additional checks: $additionalPassed of $additionalTotal passed."
