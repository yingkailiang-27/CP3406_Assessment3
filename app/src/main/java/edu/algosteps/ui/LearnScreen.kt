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
            modifier = Modifier.semantics { heading() }
        )
        Text(lesson.idea, style = MaterialTheme.typography.bodyLarge)
        Walkthrough(lesson)
        LessonText("Why it works", lesson.invariant)
        LessonText("Time and space", lesson.complexity)
        Button(onClick = onPractice, modifier = Modifier.fillMaxWidth()) { Text("Open coding exercise") }
        OutlinedButton(onClick = onReading, modifier = Modifier.fillMaxWidth()) { Text("Further reading") }
        Spacer(Modifier.height(8.dp))
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
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
            Text("${lesson.targetLabel} = ${input.target}", style = MaterialTheme.typography.titleSmall)
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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            contentColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                value.toString(),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                textDecoration = if (ruledOut) TextDecoration.LineThrough else TextDecoration.None,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                        Text("[$index]", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Step ${position + 1} of ${steps.size}", style = MaterialTheme.typography.labelMedium)
                Text(frame.title, style = MaterialTheme.typography.titleMedium)
                Text(frame.detail, style = MaterialTheme.typography.bodyMedium)
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
        }
    }
}
