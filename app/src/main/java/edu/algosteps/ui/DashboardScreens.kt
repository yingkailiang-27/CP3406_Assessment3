package edu.algosteps.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
import androidx.compose.ui.res.painterResource
import edu.algosteps.R
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
    onSettings: () -> Unit,
    onSearch: (String) -> Unit
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val importLesson = rememberLessonImport(onAlgorithms)
    HomeContent(stats, onAlgorithms, onCoding, onStats, onSettings, importedLessonTitles().keys, onSearch, importLesson)
}

@Composable
internal fun HomeContent(
    stats: DashboardStats?,
    onAlgorithms: () -> Unit,
    onCoding: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    importedIds: Set<String> = emptySet(),
    onSearch: (String) -> Unit = {},
    onImport: (() -> Unit)? = null
) {
    var search by rememberSaveable { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val submitSearch = {
        if (search.isNotBlank()) {
            focus.clearFocus()
            onSearch(search.trim())
        }
    }
    val dark = isSystemInDarkTheme()
    val paper = if (dark) MaterialTheme.colorScheme.background else Color(0xFFFAF6EC)
    val forest = Color(0xFF173C2B)
    val cardColor = if (dark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE5ECD9)
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(paper).imePadding(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Surface(color = forest, contentColor = Color.White) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AlgoSteps", Modifier.weight(1f).semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onSettings) {
                        Icon(painterResource(R.drawable.ic_settings), contentDescription = "Settings")
                    }
                }
            }
        }
        item {
            Text(
                text = "Learn algorithms",
                modifier = Modifier.padding(horizontal = 20.dp).semantics { heading() },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(12.dp),
                label = { Text("Search lessons") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                trailingIcon = {
                    TextButton(onClick = submitSearch, enabled = search.isNotBlank()) { Text("Search") }
                }
            )
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Course library", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() })
                TrackCard(false, onAlgorithms, cardColor, forest)
                if (onImport != null) {
                    TextButton(onClick = onImport) { Text("Import a lesson file") }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TrackCard(true, onCoding, cardColor, forest)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                HorizontalDivider()

                Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
private fun TrackCard(coding: Boolean, onClick: () -> Unit, cardColor: Color, forest: Color) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = cardColor,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (coding) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackMark(true, LocalContentColor.current)
                    Text("Coding practice", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                }
                Text("Write code and run sample tests.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("Follow worked examples step by step.", style = MaterialTheme.typography.bodyMedium)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = forest,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center) {
                        Text("Browse algorithms", style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold)
                    }
                }
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
    Column(Modifier.fillMaxSize()) {
        PageTitle("Statistics", onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
}

@Composable
internal fun PageTitle(label: String, onBack: () -> Unit) {
    Surface(color = Color(0xFF173C2B), contentColor = Color.White) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), contentDescription = "Back to home")
            }
            Text(
                label,
                modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
