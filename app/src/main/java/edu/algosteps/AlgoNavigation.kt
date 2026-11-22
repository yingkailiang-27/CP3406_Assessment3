package edu.algosteps

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
import edu.algosteps.data.StudyRepository
import edu.algosteps.data.SubmissionMode
import edu.algosteps.ui.*

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
                HomeScreen(dashboard, { go("algorithms") }, { go("coding") }, { go("stats") }, { go("settings") })
            }
            composable("algorithms") {
                CollectionScreen(false, back, { id -> go("learn/$id") }, { _, _ -> })
            }
            composable("coding") {
                CollectionScreen(true, back, { _ -> }, { id, exam -> go("solve/$id/$exam") })
            }
            composable("learn/{lesson}", arguments = lessonArgument) {
                val lesson = Curriculum.byId(requireNotNull(it.arguments?.getString("lesson")))
                LearnScreen(lesson, { go("solve/${lesson.id}/false") }, back, { go("reading/${lesson.id}") })
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
                CodingScreen(Curriculum.byId(requireNotNull(args.getString("lesson"))), args.getBoolean("exam"), repository, back)
            }
            composable("stats") {
                StatsScreen(dashboard, back) { id, mode -> go("solve/$id/${mode == SubmissionMode.EXAM}") }
            }
            composable("settings") { SettingsScreen(dashboard, back) }
        }
    }
}
