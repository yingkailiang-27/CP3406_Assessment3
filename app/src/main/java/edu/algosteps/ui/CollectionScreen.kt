package edu.algosteps.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import edu.algosteps.R
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import edu.algosteps.content.Curriculum
import edu.algosteps.content.ImportedLesson
import edu.algosteps.content.ImportedLessonStore
import edu.algosteps.content.readLessonPackage
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException

@Composable
fun CollectionScreen(
    coding: Boolean,
    onBack: () -> Unit,
    onLearn: (String) -> Unit,
    onSolve: (String, Boolean) -> Unit,
    initialQuery: String = ""
) {
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    val context = LocalContext.current
    var imported by remember { mutableStateOf(emptyList<ImportedLesson>()) }
    var libraryError by remember { mutableStateOf(false) }
    LaunchedEffect(context, coding) {
        if (!coding) {
            try {
                imported = withContext(Dispatchers.IO) { ImportedLessonStore(context).load() }
            } catch (_: JSONException) {
                libraryError = true
            }
        }
    }
    val searchTerm = query.trim()
    val lessons = if (coding || searchTerm.isEmpty()) Curriculum.lessons else Curriculum.lessons.filter { lesson ->
        lesson.title.contains(searchTerm, ignoreCase = true) ||
            lesson.summary.contains(searchTerm, ignoreCase = true)
    }
    val importedMatches = imported.filter {
        it.title.contains(searchTerm, ignoreCase = true) || it.summary.contains(searchTerm, ignoreCase = true)
    }
    val paper = if (!coding && !isSystemInDarkTheme()) Color(0xFFFAF6EC)
        else MaterialTheme.colorScheme.background
    Column(Modifier.fillMaxSize().background(paper).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (!coding) {
            Surface(color = Color(0xFF173C2B), contentColor = Color.White) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back to home")
                    }
                    Text("Algorithms", Modifier.weight(1f).semantics { heading() },
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).widthIn(max = 720.dp).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = if (coding) 12.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (coding) 12.dp else 16.dp)
        ) {
            if (coding) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Icon(painterResource(R.drawable.ic_back), contentDescription = "Back to home")
                            }
                            Text("Coding", Modifier.weight(1f).semantics { heading() },
                                style = MaterialTheme.typography.headlineLarge)
                        }
                        Text("Practice with a walkthrough, or take a single-problem exam independently.",
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else if (searchTerm.isNotEmpty()) {
                item {
                    Text("Results for “$searchTerm”", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { query = "" }) { Text("Clear search") }
                }
            }
            if (libraryError) item { Text("Could not read the saved course library.") }
            if (lessons.isEmpty() && importedMatches.isEmpty()) item {
                Text("No matching lessons", style = MaterialTheme.typography.titleMedium)
            }
            items(importedMatches.asReversed(), key = { it.id }) { lesson ->
                AlgorithmRow(lesson.title, lesson.summary, "Imported lesson") { onLearn(lesson.id) }
            }
            items(lessons, key = { it.id }) { lesson ->
                if (coding) {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(lesson.difficulty, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary)
                            Text(lesson.title, style = MaterialTheme.typography.titleLarge)
                            Text(lesson.summary, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onSolve(lesson.id, false) }) { Text("Practice") }
                                OutlinedButton(onClick = { onSolve(lesson.id, true) }) { Text("Exam") }
                            }
                        }
                    }
                } else {
                    AlgorithmRow(lesson.title, lesson.summary, lesson.difficulty) { onLearn(lesson.id) }
                }
            }
        }
    }
}

@Composable
private fun AlgorithmRow(title: String, summary: String, label: String, onClick: () -> Unit) {
    val dark = isSystemInDarkTheme()
    val colors = MaterialTheme.colorScheme
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (dark) colors.surfaceVariant else Color(0xFFE5ECD9)
        ),
        border = BorderStroke(1.dp, if (dark) colors.outline else Color(0xFFA9BDA1))
    ) {
        Row(
            Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (dark) colors.secondaryContainer else Color(0xFFCBDDB9),
                    contentColor = if (dark) colors.onSecondaryContainer else Color(0xFF173C2B)
                ) {
                    Text(label, Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium)
                }
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(summary, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(painterResource(R.drawable.ic_back), contentDescription = null,
                modifier = Modifier.size(18.dp).rotate(180f))
        }
    }
}

@Composable
internal fun rememberLessonImport(onImported: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val resolver = context.contentResolver
    val store = remember(context) { ImportedLessonStore(context) }
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<List<ImportedLesson>?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            loading = true
            scope.launch {
                try {
                    pending = withContext(Dispatchers.IO) {
                        val ids = Curriculum.lessons.mapTo(mutableSetOf()) { it.id }
                        store.load().forEach { ids.add(it.id) }
                        resolver.openInputStream(uri)?.use { readLessonPackage(it, ids) }
                            ?: throw IOException("Unable to open course package.")
                    }
                } catch (e: IllegalArgumentException) {
                    error = e.message ?: "Invalid course package."
                } catch (_: JSONException) {
                    error = "Choose a valid AlgoSteps course package in JSON format."
                } catch (_: IOException) {
                    error = "Could not read this file. Choose it again."
                } catch (_: SecurityException) {
                    error = "The file is unavailable. Choose it again."
                } finally {
                    loading = false
                }
            }
        }
    }
    val preview = pending
    if (loading || preview != null || error != null) {
        val close = { pending = null; error = null }
        AlertDialog(
            onDismissRequest = { if (!loading) close() },
            title = { Text(if (error == null) "Lesson preview" else "Unable to import lessons") },
            text = {
                when {
                    loading -> Text(if (preview == null) "Reading course package…" else "Saving lessons…")
                    error != null -> Text(error.orEmpty())
                    preview != null -> LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(preview) { lesson -> Text(lesson.title) }
                        item { Text("Confirm to save these lessons on this device.") }
                    }
                    else -> Text(error.orEmpty())
                }
            },
            confirmButton = {
                if (!loading && preview != null && error == null) {
                    Button(onClick = {
                        loading = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { store.add(preview) }
                                close()
                                onImported()
                            } catch (e: IllegalArgumentException) {
                                error = e.message ?: "Could not save the course package."
                            } catch (_: IOException) {
                                error = "Could not save the course package. Try again."
                            } catch (_: JSONException) {
                                error = "Could not read the saved course library."
                            } finally {
                                loading = false
                            }
                        }
                    }) { Text("Import lessons") }
                } else if (!loading) {
                    TextButton(onClick = close) { Text("Close") }
                }
            },
            dismissButton = {
                if (!loading && preview != null && error == null) {
                    TextButton(onClick = close) { Text("Cancel") }
                }
            }
        )
    }
    return { picker.launch(arrayOf("*/*")) }
}
