package edu.algosteps.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
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
import edu.algosteps.content.ImportedLessonStore
import edu.algosteps.data.SubmissionMode
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel,
    onAlgorithms: () -> Unit,
    onCoding: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    HomeContent(stats, onAlgorithms, onCoding, onStats, onSettings, importedLessonTitles().keys)
}

@Composable
internal fun HomeContent(
    stats: DashboardStats?,
    onAlgorithms: () -> Unit,
    onCoding: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    importedIds: Set<String> = emptySet()
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
            Text(
                text = "Trace an algorithm, then write and test your code.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your progress", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() })
                    if (stats == null) {
                        Text("Loading progress…")
                    } else {
                        val lessonIds = Curriculum.lessons.map { it.id } + importedIds
                        val solved = lessonIds.count { it in stats.solvedTopics }
                        Text("$solved of ${lessonIds.size} problems solved")
                        LinearProgressIndicator(progress = { solved.toFloat() / lessonIds.size.coerceAtLeast(1) },
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
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (coding) colors.surface else colors.primary,
        contentColor = if (coding) colors.onSurface else colors.onPrimary,
        border = if (coding) BorderStroke(1.dp, colors.outline) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackMark(coding, LocalContentColor.current)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    if (coding) "Coding" else "Algorithms",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (coding) "Write code and run sample tests." else "Follow worked examples step by step.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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
    StatsContent(stats, onBack, onRetry, importedLessonTitles())
}

@Composable
private fun importedLessonTitles(): Map<String, String> {
    val context = LocalContext.current
    val importedTitles by produceState(emptyMap<String, String>(), context) {
        value = try {
            withContext(Dispatchers.IO) { ImportedLessonStore(context).load().associate { it.id to it.title } }
        } catch (_: JSONException) {
            emptyMap()
        }
    }
    return importedTitles
}

@Composable
internal fun StatsContent(
    stats: DashboardStats?,
    onBack: () -> Unit,
    onRetry: (String, SubmissionMode) -> Unit,
    importedTitles: Map<String, String> = emptyMap()
) {
    val curriculum = remember(importedTitles) {
        Curriculum.lessons.associate { it.id to it.title } + importedTitles
    }
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
        if (stats != null) item {
            val total = curriculum.size
            val solved = curriculum.keys.count { it in stats.solvedTopics }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Learning progress", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() })
                    Text("$solved of $total problems solved", style = MaterialTheme.typography.headlineSmall)
                    LinearProgressIndicator(
                        progress = { solved.toFloat() / total.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
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
                        val needsWork = count - modeStats.accepted
                        Text(
                            if (needsWork == 1) "1 submission needs work" else "$needsWork submissions need work",
                            style = MaterialTheme.typography.bodySmall
                        )
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
                val lessonTitle = curriculum[attempt.lessonId]
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(lessonTitle ?: attempt.lessonId, style = MaterialTheme.typography.titleMedium)
                        Text(attempt.mode.label, style = MaterialTheme.typography.labelMedium)
                        val colors = MaterialTheme.colorScheme
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (attempt.accepted) colors.primaryContainer else colors.errorContainer,
                            contentColor = if (attempt.accepted) colors.onPrimaryContainer else colors.onErrorContainer
                        ) {
                            Text(
                                if (attempt.accepted) "Accepted" else "Needs work",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Text("${attempt.passed}/${attempt.total} tests passed · ${dateFormat.format(Date(attempt.submittedAt))}",
                            style = MaterialTheme.typography.bodySmall)
                        if (lessonTitle != null) {
                            val action = if (attempt.mode == SubmissionMode.EXAM) "Open exam" else "Open practice"
                            OutlinedButton(
                                onClick = { onRetry(attempt.lessonId, attempt.mode) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(action) }
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
