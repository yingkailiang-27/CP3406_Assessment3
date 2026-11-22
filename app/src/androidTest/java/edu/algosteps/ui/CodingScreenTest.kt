package edu.algosteps.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.room.Room
import edu.algosteps.data.Attempt
import edu.algosteps.data.SubmissionMode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.algosteps.content.Curriculum
import edu.algosteps.data.ReadingSource
import edu.algosteps.data.ReminderScheduler
import edu.algosteps.data.StudyDatabase
import edu.algosteps.data.StudyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CodingScreenTest {
    @get:Rule val compose = createComposeRule()
    private val lesson = Curriculum.byId("linear-search")

    @Test fun examKeepsWorkedAnswerClosedUntilSubmissionAndDisablesDuplicateSubmit() {
        val events = mutableListOf<Boolean>()
        compose.setContent {
            var state by remember { mutableStateOf(CodingState(source = lesson.starter, ready = true)) }
            MaterialTheme {
                CodingContent(lesson, true, state, true, { state = state.copy(source = it) },
                    { submit ->
                        events.add(submit)
                        if (submit) state = state.copy(lastAttempt = Attempt(lesson.id, 0, 8, 0, sourceHash = sourceDigest(state.source)))
                    }, {}, {})
            }
        }
        compose.onNodeWithText("Show code walkthrough").assertDoesNotExist()
        compose.onNodeWithText("Run samples").performScrollTo().performClick()
        compose.onNodeWithText("Show code walkthrough").assertDoesNotExist()
        compose.onNodeWithText("Submit solution").performScrollTo().performClick()
        compose.onNodeWithText("Submission recorded").assertIsNotEnabled()
        compose.onNodeWithText("Show code walkthrough").performScrollTo().performClick()
        compose.onNodeWithText("Worked solution").assertExists()
        compose.runOnIdle { assertEquals(listOf(false, true), events) }
    }

    @Test fun practiceAllowsCodeEditingAndExplainsTheSolutionWithoutSubmission() {
        var edited = ""
        compose.setContent {
            var state by remember { mutableStateOf(CodingState(source = lesson.starter, ready = true)) }
            MaterialTheme {
                CodingContent(lesson, false, state, false, {
                    edited = it
                    state = state.copy(source = it)
                }, {}, {}, {})
            }
        }
        val replacement = "def solve(nums, target):\n    return 4"
        compose.onNodeWithTag("code-editor").performScrollTo().performTextReplacement(replacement)
        compose.runOnIdle { assertEquals(replacement, edited) }
        compose.onNodeWithText("Show code walkthrough").performScrollTo().performClick()
        compose.onNodeWithText(lesson.notes.first()).assertExists()
    }

    @Test fun unfinishedExamCannotRevealAnswerButAnIncorrectIntegerStillCounts() = withFreshExam { repository ->
        // The unmodified starter compiles in this teaching language.
        compose.onNodeWithText("Submit solution").performScrollTo().performClick()
        waitForEither("Add a return statement", "Submission recorded")
        assertEquals(0, runBlocking { repository.attempts.first().size })
        compose.onNodeWithText("Add a return statement", substring = true).assertExists()
        compose.onNodeWithText("Show code walkthrough").assertDoesNotExist()

        // Comments are not a return statement, even when they look like code.
        compose.onNodeWithTag("code-editor").performScrollTo().performTextReplacement(
            "def solve(nums, target):\n    # return 12\n    value = target"
        )
        compose.onNodeWithText("Submit solution").performScrollTo().performClick()
        waitForEither("Add a return statement", "Submission recorded")
        assertEquals(0, runBlocking { repository.attempts.first().size })
        compose.onNodeWithText("Show code walkthrough").assertDoesNotExist()

        // A genuine answer earns the outcome it achieved, including a score of zero.
        compose.onNodeWithTag("code-editor").performScrollTo()
            .performTextReplacement("def solve(nums, target):\n    return 54321")
        compose.onNodeWithText("Submit solution").performScrollTo().performClick()
        waitForEither("0 of 8 tests passed", "Submission recorded")
        assertEquals(0, runBlocking { repository.attempts.first().single().passed })
        compose.onNodeWithText("Show code walkthrough").assertExists()
    }

    @Test fun rerunningPublicSamplesKeepsTheSubmittedAdditionalCheckBreakdown() = withFreshExam { repository ->
        // Zero is wrong on both public inputs but correct on two additional cases.
        compose.onNodeWithTag("code-editor").performScrollTo()
            .performTextReplacement("def solve(nums, target):\n    return 0")
        compose.onNodeWithText("Submit solution").performScrollTo().performClick()
        waitForEither("Additional checks:", "Submission recorded")
        compose.onNodeWithText("Additional checks: 2 of 6 passed.").assertExists()
        compose.onNodeWithText("Run samples").performScrollTo().performClick()
        waitForEither("Sample results", "Sample runs do not count")
        compose.onNodeWithText("0 of 2 tests passed").assertExists()
        compose.onNodeWithText("Additional checks: 2 of 6 passed.").assertExists()
        assertEquals(1, runBlocking { repository.attempts.first().size })
        // After a source edit, the old breakdown must not describe the new draft.
        compose.onNodeWithTag("code-editor").performScrollTo()
            .performTextReplacement("def solve(nums, target):\n    return target")
        compose.onNodeWithText("Additional checks:", substring = true).assertDoesNotExist()
    }

    private fun waitForEither(first: String, second: String) {
        compose.waitUntil(5_000) {
            listOf(first, second).any {
                compose.onAllNodesWithText(it, substring = true).fetchSemanticsNodes().isNotEmpty()
            }
        }
    }

    private fun withFreshExam(check: (StudyRepository) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, StudyDatabase::class.java
        ).build()
        val repo = StudyRepository(db.studyDao(), ReadingSource { null }, object : ReminderScheduler {
            override suspend fun update(enabled: Boolean) = Unit
            override fun dismiss() = Unit
        })
        val owner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        try {
            compose.setContent {
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    MaterialTheme { CodingScreen(lesson, true, repo) {} }
                }
            }
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("code-editor").fetchSemanticsNodes().isNotEmpty() }
            check(repo)
        } finally {
            runBlocking { awaitDraftSaves(repo) }
            compose.runOnIdle { owner.viewModelStore.clear() }
            db.close()
        }
    }

    @Test fun sampleAndSubmissionUseRealJudgeAndRecordOnlyOneAttempt() {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, StudyDatabase::class.java
        ).build()
        val repository = StudyRepository(db.studyDao(), ReadingSource { null }, object : ReminderScheduler {
            override suspend fun update(enabled: Boolean) = Unit
            override fun dismiss() = Unit
        })
        val firstOwner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        val reopenedOwner = object : ViewModelStoreOwner { override val viewModelStore = ViewModelStore() }
        var owner by mutableStateOf<ViewModelStoreOwner>(firstOwner)
        try {
            runBlocking { repository.saveDraft(lesson.id, lesson.reference) }
            compose.setContent {
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
                    key(owner) { MaterialTheme { CodingScreen(lesson, true, repository) {} } }
                }
            }
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("code-editor").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("code-editor").assertTextContains("    pass", substring = true)
            compose.onNodeWithTag("code-editor").performScrollTo()
                .performTextReplacement("def solve(nums, target)\n    return 1")
            compose.onNodeWithText("Submit solution").performScrollTo().performClick()
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Fix your code before submitting.", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Show code walkthrough").assertDoesNotExist()
            assertEquals(0, runBlocking { repository.attempts.first().size })
            compose.onNodeWithTag("code-editor").performScrollTo().performTextReplacement(lesson.reference)
            compose.onNodeWithText("Run samples").performScrollTo().performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("2 of 2 tests passed").fetchSemanticsNodes().isNotEmpty() }
            assertEquals(0, runBlocking { repository.attempts.first().size })
            compose.onNodeWithText("Submit solution").performScrollTo().performClick()
            val total = lesson.samples.size + lesson.checks.size
            compose.waitUntil(5_000) { compose.onAllNodesWithText("$total of $total tests passed").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Submission recorded").assertIsNotEnabled()
            val attempt = runBlocking { repository.attempts.first().single() }
            assertEquals(lesson.id, attempt.lessonId)
            assertEquals(total, attempt.passed)
            assertEquals(total, attempt.total)
            assertEquals(SubmissionMode.EXAM.name, attempt.mode)

            compose.runOnIdle {
                firstOwner.viewModelStore.clear()
                owner = reopenedOwner
            }
            compose.waitUntil(5_000) {
                compose.onAllNodesWithText("Submission recorded").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Submission recorded").assertIsNotEnabled()
            compose.onNodeWithText("Show code walkthrough").assertExists()
            assertEquals(1, runBlocking { repository.attempts.first().size })
        } finally {
            compose.runOnIdle { reopenedOwner.viewModelStore.clear() }
            db.close()
        }
    }
}
