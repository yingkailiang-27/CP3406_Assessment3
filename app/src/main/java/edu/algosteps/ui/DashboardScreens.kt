package edu.algosteps.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.algosteps.content.Curriculum
import edu.algosteps.data.SubmissionMode
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel,
    onAlgorithms: () -> Unit,
    onCoding: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    HomeContent(stats, onAlgorithms, onCoding, onStats, onSettings)
}

@Composable
internal fun HomeContent(
    stats: DashboardStats?,
    onAlgorithms: () -> Unit,
    onCoding: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AlgoSteps", Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = onSettings) { Text("Settings") }
            }
        }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth >= 580.dp) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TrackCard(false, onAlgorithms, Modifier.weight(1f))
                        TrackCard(true, onCoding, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TrackCard(false, onAlgorithms, Modifier.fillMaxWidth())
                        TrackCard(true, onCoding, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your progress", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() })
                    if (stats == null) {
                        Text("Loading progress…")
                    } else {
                        val solved = Curriculum.lessons.count { it.id in stats.solvedTopics }
                        Text("$solved of ${Curriculum.lessons.size} problems solved")
                        LinearProgressIndicator(progress = { solved.toFloat() / Curriculum.lessons.size.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(onClick = onStats) { Text("View statistics") }
                }
            }
        }
    }
}

@Composable
private fun TrackCard(coding: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (coding) colors.tertiaryContainer else colors.primaryContainer,
            contentColor = if (coding) colors.onTertiaryContainer else colors.onPrimaryContainer
        )
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val ink = LocalContentColor.current
            TrackMark(coding, ink)
            Text(if (coding) "Coding" else "Algorithms",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(if (coding) "Read code, run samples and submit a solution."
                else "Trace algorithms step by step on example inputs.",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TrackMark(coding: Boolean, ink: Color) {
    Canvas(Modifier.size(40.dp, 24.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        if (coding) {
            drawLine(ink, Offset(.28f*w, .18f*h), Offset(.08f*w, .52f*h), stroke)
            drawLine(ink, Offset(.08f*w, .52f*h), Offset(.28f*w, .86f*h), stroke)
            drawLine(ink, Offset(.61f*w, .08f*h), Offset(.39f*w, .92f*h), stroke)
            drawLine(ink, Offset(.73f*w, .18f*h), Offset(.93f*w, .52f*h), stroke)
            drawLine(ink, Offset(.93f*w, .52f*h), Offset(.73f*w, .86f*h), stroke)
        } else {
            listOf(.36f, .62f, .86f).forEachIndexed { i, scale ->
                drawRect(ink, Offset(.04f*w+i*.33f*w, (.96f-scale)*h),
                    Size(.21f*w, scale*h), style = Stroke(stroke))
            }
        }
    }
}

@Composable
fun StatsScreen(viewModel: DashboardViewModel, onBack: () -> Unit, onRetry: (String, SubmissionMode) -> Unit) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    StatsContent(stats, onBack, onRetry)
}

@Composable
internal fun StatsContent(stats: DashboardStats?, onBack: () -> Unit, onRetry: (String, SubmissionMode) -> Unit) {
    val curriculum = remember { Curriculum.lessons.associateBy { it.id } }
    val dateFormat = remember(Locale.getDefault()) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    }
    var selectedMode by rememberSaveable { mutableStateOf<SubmissionMode?>(null) }
    var onlyMisses by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PageTitle("Statistics", onBack) }
        if (stats == null) {
            item { Text("Loading submissions…") }
        } else if (stats.submissions.isEmpty()) {
            item {
                Text("No submissions yet", style = MaterialTheme.typography.titleLarge)
                Text("Submit a solution in Coding to record your first result.")
            }
        } else {
            val modeStats = DashboardStats(stats.submissions.filter { selectedMode == null || it.mode == selectedMode })
            val count = modeStats.submissions.size
            if (count > 0) item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val percent = (modeStats.accepted * 100.0 / count).roundToInt()
                        Text("$percent% accepted", style = MaterialTheme.typography.headlineMedium)
                        Text("${modeStats.accepted} of $count submissions passed every test.")
                        Text("${count - modeStats.accepted} submissions need work · ${modeStats.solvedTopics.size} problems solved",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Text("Submission history", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() })
                val modes = listOf(null, SubmissionMode.PRACTICE, SubmissionMode.EXAM) +
                    listOf(SubmissionMode.LEGACY).filter { mode -> stats.submissions.any { it.mode == mode } }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modes.forEach { mode ->
                        FilterChip(selected = selectedMode == mode, onClick = { selectedMode = mode },
                            modifier = Modifier.testTag("mode-${mode?.name ?: "all"}"),
                            label = { Text(mode?.label ?: "All modes") })
                    }
                }
                FilterChip(selected = onlyMisses, onClick = { onlyMisses = !onlyMisses },
                    label = { Text("Needs work only") })
            }
            val visible = if (onlyMisses) modeStats.submissions.filterNot { it.accepted } else modeStats.submissions
            if (visible.isEmpty()) item { Text(if (modeStats.submissions.isEmpty()) "No submissions for this mode." else "Every submission passed.") }
            items(visible) { attempt ->
                val lesson = curriculum[attempt.lessonId]
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(lesson?.title ?: attempt.lessonId, style = MaterialTheme.typography.titleMedium)
                        Text(attempt.mode.label, style = MaterialTheme.typography.labelMedium)
                        Text(if (attempt.accepted) "Accepted" else "Needs work", fontWeight = FontWeight.SemiBold)
                        Text("${attempt.passed}/${attempt.total} tests passed · ${dateFormat.format(Date(attempt.submittedAt))}",
                            style = MaterialTheme.typography.bodySmall)
                        if (lesson != null) {
                            val action = if (attempt.mode == SubmissionMode.EXAM) "Open exam" else "Open practice"
                            TextButton(onClick = { onRetry(lesson.id, attempt.mode) }) { Text(action) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PageTitle(label: String, onBack: () -> Unit) {
    Column {
        TextButton(onClick = onBack) { Text("Back") }
        Text(label, modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    }
}
