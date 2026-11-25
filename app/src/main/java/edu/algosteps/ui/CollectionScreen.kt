package edu.algosteps.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import edu.algosteps.content.Curriculum

@Composable
fun CollectionScreen(
    coding: Boolean,
    onBack: () -> Unit,
    onLearn: (String) -> Unit,
    onSolve: (String, Boolean) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
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
                    Text(if (coding) "Coding" else "Algorithms",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.semantics { heading() })
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
