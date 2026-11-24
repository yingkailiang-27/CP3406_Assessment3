package edu.algosteps.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import edu.algosteps.content.Lesson

@Composable
fun LearnScreen(lesson: Lesson, onPractice: () -> Unit, onBack: () -> Unit, onReading: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("Back to lessons") }
        Text(
            lesson.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        Text(lesson.idea, style = MaterialTheme.typography.bodyLarge)
        Walkthrough(lesson)
        when (lesson.id) {
            "linear-search" -> PredictionQuestion(
                prompt = "Search [4, 7, 2, 7] from left to right for the first 7. " +
                    "You find 7 at index 1. What should you do next?",
                correctOption = "Return index 1",
                incorrectOption = "Keep searching",
                explanation = "Correct. Index 1 is the first match because every earlier value has already been checked.",
                hint = "Try again. The task asks for the first match. Would a later 7 change that answer?"
            )
            "count-occurrences" -> PredictionQuestion(
                prompt = "Count the 5s in [5, 2, 5, 5]. After checking the first two values, the count is 1. " +
                    "What should you do when you reach the 5 at index 2?",
                correctOption = "Set count to 2 and keep going",
                incorrectOption = "Keep count at 1 and move on",
                explanation = "Correct. Each matching position counts, so the 5 at index 2 raises the count to 2. " +
                    "Keep checking the list for more matches.",
                hint = "Try again. The 5 at index 2 is a separate occurrence. What should that add to the count?"
            )
            "binary-search" -> PredictionQuestion(
                prompt = "Find 6 in [2, 6, 9, 15, 22]. The middle value is 9. Which side should you search next?",
                correctOption = "Search left",
                incorrectOption = "Search right",
                explanation = "Correct. In a sorted list, every value to the right of 9 is larger than 9. Since 6 is smaller, search to the left.",
                hint = "Try again. Is 6 smaller or larger than the middle value, 9?"
            )
            "window-sum" -> PredictionQuestion(
                prompt = "A window of width 2 starts at [3, 8] in [3, 8, 2, 6]. " +
                    "It moves one place to the right. How should you update the sum?",
                correctOption = "Subtract 3, add 2",
                incorrectOption = "Subtract 8, add 2",
                explanation = "Correct. The window moves from [3, 8] to [8, 2]. " +
                    "Keep 8, subtract 3 and add 2. The sum changes from 11 to 10.",
                hint = "Try again. Which number leaves the window when it moves one place to the right?"
            )
        }
        Button(onClick = onPractice, modifier = Modifier.fillMaxWidth()) { Text("Open coding exercise") }
        LessonText("Why it works", lesson.invariant)
        LessonText("Time and space", lesson.complexity)
        Text(
            text = "Further reading requests a public article from Wikipedia. Your code drafts and scores stay on this device.",
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(onClick = onReading, modifier = Modifier.fillMaxWidth()) { Text("Further reading") }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PredictionQuestion(
    prompt: String,
    correctOption: String,
    incorrectOption: String,
    explanation: String,
    hint: String
) {
    var answer by rememberSaveable { mutableIntStateOf(-1) }
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceVariant,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Predict the next step",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(prompt)
            FilterChip(
                selected = answer == 0,
                onClick = { answer = 0 },
                label = { Text(correctOption) }
            )
            FilterChip(
                selected = answer == 1,
                onClick = { answer = 1 },
                label = { Text(incorrectOption) }
            )
            if (answer >= 0) {
                Text(
                    text = if (answer == 0) explanation else hint,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = if (answer == 0) colors.onSurfaceVariant else colors.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun LessonText(label: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun Walkthrough(lesson: Lesson, sampleIndex: Int? = null) {
    var selectedExample by rememberSaveable(lesson.id) { mutableIntStateOf(0) }
    val example = sampleIndex ?: selectedExample
    var position by rememberSaveable(lesson.id, sampleIndex) { mutableIntStateOf(0) }
    val input = lesson.samples[example]
    val steps = remember(lesson.id, example) { lesson.trace(input) }
    val frame = steps[position]
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceVariant,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Walk through an input", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            if (sampleIndex == null) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lesson.samples.indices.forEach { index ->
                        FilterChip(
                            selected = example == index,
                            onClick = {
                                selectedExample = index
                                position = 0
                            },
                            label = { Text("Example ${index + 1}") }
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = colors.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    "${lesson.targetLabel} = ${input.target}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                input.nums.forEachIndexed { index, value ->
                    val active = index in frame.active
                    val searchRange = frame.searchRange
                    val ruledOut = searchRange != null && index !in searchRange
                    val state = when {
                        ruledOut -> ", ruled out"
                        active && searchRange != null -> ", middle of the search interval"
                        searchRange != null -> ", in the search interval"
                        active -> ", in this step"
                        else -> ""
                    }
                    Column(
                        modifier = Modifier.clearAndSetSemantics {
                            contentDescription = "Index $index, value $value$state"
                        },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("[$index]", style = MaterialTheme.typography.labelMedium)
                        Surface(
                            color = when {
                                active -> colors.primary
                                ruledOut -> colors.surfaceVariant
                                else -> colors.surface
                            },
                            contentColor = if (active) colors.onPrimary else colors.onSurface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                value.toString(),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                textDecoration = if (ruledOut) TextDecoration.LineThrough else TextDecoration.None,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(min = 38.dp).padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { position = (position - 1).coerceAtLeast(0) }, enabled = position > 0, modifier = Modifier.widthIn(min = 64.dp)) {
                    Text("Previous")
                }
                if (position == steps.lastIndex) {
                    OutlinedButton(onClick = { position = 0 }) { Text("Replay") }
                } else {
                    Button(onClick = { position = (position + 1).coerceAtMost(steps.lastIndex) }) { Text("Next step") }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                color = colors.surface,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Step ${position + 1} of ${steps.size}", color = colors.primary,
                        style = MaterialTheme.typography.labelMedium)
                    Text(frame.title, style = MaterialTheme.typography.titleMedium)
                    Text(frame.detail, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
