package edu.algosteps.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import edu.algosteps.content.Curriculum
import edu.algosteps.content.readLessonTitles
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
    onSolve: (String, Boolean) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var importMenuOpen by rememberSaveable { mutableStateOf(false) }
    val openImport = if (!coding) rememberLessonImportPreview() else null
    val searchTerm = query.trim()
    val lessons = if (coding || searchTerm.isEmpty()) Curriculum.lessons else Curriculum.lessons.filter { lesson ->
        lesson.title.contains(searchTerm, ignoreCase = true) ||
            lesson.summary.contains(searchTerm, ignoreCase = true)
    }
    Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onBack) { Text("Back to home") }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (coding) "Coding" else "Algorithms",
                            style = MaterialTheme.typography.headlineLarge,
                            modifier = Modifier.weight(1f).semantics { heading() }
                        )
                        if (!coding) {
                            Box {
                                TextButton(onClick = { importMenuOpen = true }) { Text("More") }
                                DropdownMenu(
                                    expanded = importMenuOpen,
                                    onDismissRequest = { importMenuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Import lessons") },
                                        onClick = {
                                            importMenuOpen = false
                                            openImport?.invoke()
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text(if (coding) "Practice with a walkthrough, or take a single-problem exam independently."
                         else "Choose an algorithm to trace it on two different inputs.",
                        style = MaterialTheme.typography.bodyLarge)
                    if (!coding) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search algorithms") },
                            singleLine = true,
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    TextButton(onClick = { query = "" }) { Text("Clear") }
                                }
                            }
                        )
                    }
                }
            }
            if (lessons.isEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("No matching lessons", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { query = "" }) { Text("Clear search") }
                }
            }
            items(lessons, key = { it.id }) { lesson ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(lesson.difficulty, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Text(lesson.title, style = MaterialTheme.typography.titleLarge)
                        Text(lesson.summary, style = MaterialTheme.typography.bodyMedium)
                        if (coding) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onSolve(lesson.id, false) }) { Text("Practice") }
                                OutlinedButton(onClick = { onSolve(lesson.id, true) }) { Text("Exam") }
                            }
                        } else {
                            TextButton(onClick = { onLearn(lesson.id) }) { Text("Study algorithm") }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun rememberLessonImportPreview(): () -> Unit {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var titles by rememberSaveable { mutableStateOf<ArrayList<String>?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            loading = true
            scope.launch {
                try {
                    titles = ArrayList(withContext(Dispatchers.IO) {
                        val ids = Curriculum.lessons.mapTo(mutableSetOf()) { it.id }
                        resolver.openInputStream(uri)?.use { readLessonTitles(it, ids) }
                            ?: throw IOException("Unable to open course package.")
                    })
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
    val preview = titles
    if (loading || preview != null || error != null) {
        val close = { titles = null; error = null }
        AlertDialog(
            onDismissRequest = { if (!loading) close() },
            title = { Text(if (error == null) "Lesson preview" else "Unable to preview lessons") },
            text = {
                when {
                    loading -> Text("Reading course package…")
                    preview != null -> LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(preview) { title -> Text(title) }
                        item { Text("Preview only. No lessons have been added.") }
                    }
                    else -> Text(error.orEmpty())
                }
            },
            confirmButton = {
                if (!loading) TextButton(onClick = close) { Text("Close") }
            }
        )
    }
    return { picker.launch(arrayOf("*/*")) }
}
