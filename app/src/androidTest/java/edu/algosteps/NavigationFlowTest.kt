package edu.algosteps

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import edu.algosteps.data.ReadingSource
import edu.algosteps.data.ReminderScheduler
import edu.algosteps.data.StudyDatabase
import edu.algosteps.data.StudyRepository
import edu.algosteps.data.SubmissionMode
import edu.algosteps.ui.AlgoStepsTheme
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {
    @get:Rule val app = createAndroidComposeRule<MainActivity>()

    @Test
    fun lessonLeadsToMatchingPracticeAndBackRestoresTheLesson() {
        app.onNodeWithText("Browse algorithms").performClick()
        app.onNodeWithText("Linear search").performClick()
        app.onNodeWithText("Example 2").performScrollTo().performClick()
        app.onNodeWithText("Next step").performScrollTo().performClick()
        app.onNodeWithText("Open coding exercise").performScrollTo().performClick()
        app.onNodeWithText("Coding practice").assertIsDisplayed()
        app.onNodeWithText("Linear search").assertIsDisplayed()
        app.onNodeWithText("Back").performClick()
        app.onNodeWithText("Why it works").performScrollTo().assertIsDisplayed()
        app.onNodeWithContentDescription("Back to lessons").performScrollTo().performClick()
        app.onNodeWithText("Algorithms").assertIsDisplayed()
    }
}


@RunWith(AndroidJUnit4::class)
class HistoryNavigationTest {
    @get:Rule val app = createComposeRule()

    @Test
    fun openingExamFromHistoryRestoresExamDraftWhenPracticeDraftAlsoExists() {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), StudyDatabase::class.java
        ).build()
        val repository = StudyRepository(db.studyDao(), ReadingSource { null }, object : ReminderScheduler {
            override suspend fun update(enabled: Boolean) {}
            override fun dismiss() {}
        })
        val examDraft = "def solve(nums, target):\n    return 31"
        try {
            runBlocking {
                repository.saveAttempt("linear-search", 1, 4, SubmissionMode.EXAM)
                repository.saveDraft("linear-search", "def solve(nums, target):\n    return -7")
                repository.saveDraft("linear-search:exam", examDraft)
            }
            app.setContent { AlgoStepsTheme { AlgoNavigation(repository) } }

            app.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("View statistics"))
            app.onNodeWithText("View statistics").performClick()
            app.waitUntil(5_000) { app.onAllNodesWithText("0% accepted").fetchSemanticsNodes().isNotEmpty() }
            app.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Open exam"))
            app.onNodeWithText("Open exam").performClick()
            app.waitUntil(5_000) { app.onAllNodesWithTag("code-editor").fetchSemanticsNodes().isNotEmpty() }
            app.onNodeWithText("Exam").assertIsDisplayed()
            app.onNodeWithTag("code-editor").assertTextContains(examDraft)
        } finally {
            db.close()
        }
    }
}
