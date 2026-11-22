package edu.algosteps.ui

import android.content.ActivityNotFoundException
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.algosteps.content.Lesson
import edu.algosteps.data.StudyRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class Load { Loading, Updated, Failed }

private class ReadingViewModel(val id: String, val repo: StudyRepository) : ViewModel() {
    val article = repo.reading(id)
    private val load = MutableStateFlow(Load.Loading)
    val status = load.asStateFlow()
    private var loading = false

    init { refresh() }
    fun refresh() {
        if (loading) return
        loading = true
        load.value = Load.Loading
        viewModelScope.launch {
            try {
                load.value = if (repo.refreshReading(id)) Load.Updated else Load.Failed
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                load.value = Load.Failed
            } finally { loading = false }
        }
    }
}

@Composable
fun ReadingScreen(lesson: Lesson, repository: StudyRepository, onBack: () -> Unit) {
    val vm: ReadingViewModel = viewModel(key = "reading-${lesson.id}", factory = viewModelFactory {
        initializer { ReadingViewModel(lesson.id, repository) }
    })
    val article by vm.article.collectAsStateWithLifecycle(initialValue = null)
    val status by vm.status.collectAsStateWithLifecycle()
    val handler = LocalUriHandler.current
    var linkProblem by remember { mutableStateOf(false) }
    val open: (String) -> Unit = { url ->
        try { handler.openUri(url); linkProblem = false }
        catch (_: ActivityNotFoundException) { linkProblem = true }
        catch (_: IllegalArgumentException) { linkProblem = true }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 720.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack) { Text("Back to lesson") }
            Text("Further reading", style = MaterialTheme.typography.headlineLarge)
            Text(lesson.title, style = MaterialTheme.typography.titleMedium)
            if (status == Load.Loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            val current = article
            val statusText = when (status) {
                Load.Loading -> "Checking Wikipedia for the latest introduction…"
                Load.Updated -> "Reading updated from Wikipedia."
                Load.Failed -> if (current == null) "Could not load this article. Check your connection and retry; the lesson works offline."
                               else "Refresh unavailable. Showing your saved copy."
            }
            Text(statusText, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (current != null) {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(current.title, style = MaterialTheme.typography.titleLarge)
                        Text(current.extract, style = MaterialTheme.typography.bodyLarge)
                        Text("Saved ${DateFormat.getDateTimeInstance().format(Date(current.updatedAt))}",
                            style = MaterialTheme.typography.labelMedium)
                        TextButton(onClick = { open(current.url) }) { Text("Wikipedia contributors · Original article") }
                        TextButton(onClick = { open("https://creativecommons.org/licenses/by-sa/4.0/") }) { Text("License · CC BY-SA 4.0") }
                        if (linkProblem) Text("No app could open that link.")
                    }
                }
            }
            OutlinedButton(onClick = vm::refresh, enabled = status != Load.Loading) { Text("Refresh article") }
        }
    }
}
