package edu.algosteps

import android.net.Uri

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.algosteps.content.Curriculum
import edu.algosteps.content.ImportedLessonStore
import edu.algosteps.content.Lesson
import edu.algosteps.content.LessonPrediction
import edu.algosteps.content.toStudyLesson
import edu.algosteps.data.StudyRepository
import edu.algosteps.data.SubmissionMode
import edu.algosteps.judge.CodeJudge
import edu.algosteps.judge.JudgeException
import edu.algosteps.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException

@Composable
fun AlgoNavigation(repository: StudyRepository) {
    val nav = rememberNavController()
    val dashboard: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(repository))
    val go: (String) -> Unit = { route -> nav.navigate(route) { launchSingleTop = true } }
    val back: () -> Unit = { nav.popBackStack() }
    val lessonArgument = listOf(navArgument("lesson") { type = NavType.StringType })

    Surface(Modifier.fillMaxSize()) {
        NavHost(nav, startDestination = "home", modifier = Modifier.safeDrawingPadding()) {
            composable("home") {
                HomeScreen(dashboard, { go("algorithms") }, { go("coding") }, { go("stats") }, { go("settings") },
                    { query -> go("algorithms?query=${Uri.encode(query)}") })
            }
            composable(
                "algorithms?query={query}",
                arguments = listOf(navArgument("query") { defaultValue = ""; type = NavType.StringType })
            ) { entry ->
                CollectionScreen(
                    false, back, { id -> go("learn/$id") }, { _, _ -> },
                    initialQuery = entry.arguments?.getString("query").orEmpty()
                )
            }
            composable("coding") {
                CollectionScreen(true, back, { _ -> }, { id, exam -> go("solve/$id/$exam") })
            }
            composable("learn/{lesson}", arguments = lessonArgument) {
                val id = requireNotNull(it.arguments?.getString("lesson"))
                val lesson = Curriculum.lessons.find { lesson -> lesson.id == id }
                if (lesson != null) {
                    val backLabel = if (nav.previousBackStackEntry?.destination?.route == "home") {
                        "Back to home"
                    } else {
                        "Back to lessons"
                    }
                    LearnScreen(lesson, { go("solve/$id/false") }, back, { go("reading/$id") }, backLabel = backLabel)
                } else {
                    ImportedLessonScreen(id, back) { imported, prediction ->
                        LearnScreen(imported, { go("solve/$id/false") }, back, null, prediction)
                    }
                }
            }
            composable("reading/{lesson}", arguments = lessonArgument) {
                val id = requireNotNull(it.arguments?.getString("lesson"))
                ReadingScreen(Curriculum.byId(id), repository, back)
            }
            composable(
                "solve/{lesson}/{exam}",
                arguments = lessonArgument + navArgument("exam") { type = NavType.BoolType }
            ) {
                val args = requireNotNull(it.arguments)
                val id = requireNotNull(args.getString("lesson"))
                val lesson = Curriculum.lessons.find { it.id == id }
                if (lesson != null) {
                    CodingScreen(lesson, args.getBoolean("exam"), repository, back)
                } else {
                    ImportedLessonScreen(id, back, checkReference = true) { imported, _ ->
                        CodingScreen(imported, false, repository, back)
                    }
                }
            }
            composable("stats") {
                StatsScreen(dashboard, back) { id, mode -> go("solve/$id/${mode == SubmissionMode.EXAM}") }
            }
            composable("settings") { SettingsScreen(dashboard, back) }
        }
    }
}

@Composable
private fun ImportedLessonScreen(
    id: String,
    onBack: () -> Unit,
    checkReference: Boolean = false,
    content: @Composable (Lesson, LessonPrediction) -> Unit
) {
    val context = LocalContext.current
    val result = produceState<Result<Pair<Lesson, LessonPrediction>?>?>(null, id, checkReference, context) {
        value = try {
            Result.success(withContext(Dispatchers.IO) {
                ImportedLessonStore(context).load().find { it.id == id }?.toStudyLesson()?.also { imported ->
                    if (checkReference) verifyReference(imported.first)
                }
            })
        } catch (error: JSONException) {
            Result.failure(error)
        } catch (error: IllegalArgumentException) {
            Result.failure(error)
        } catch (error: JudgeException) {
            Result.failure(IllegalArgumentException("The reference solution could not run: ${error.message}"))
        }
    }.value
    val imported = result?.getOrNull()
    if (imported != null) {
        content(imported.first, imported.second)
    } else {
        Column(Modifier.padding(20.dp)) {
            TextButton(onClick = onBack) { Text("Back to lessons") }
            val validationMessage = (result?.exceptionOrNull() as? IllegalArgumentException)?.message
            Text(if (result == null) "Loading lesson…" else validationMessage ?: "This imported lesson is unavailable.")
        }
    }
}

private fun verifyReference(lesson: Lesson) {
    val program = CodeJudge.compile(lesson.reference)
    require(program.hasReturnStatement) { "The reference solution must return an integer." }
    for ((index, case) in (lesson.samples + lesson.checks).withIndex()) {
        require(program.run(case.nums, case.target) == case.expected) {
            "The reference solution does not match the expected result for test ${index + 1}."
        }
    }
}
