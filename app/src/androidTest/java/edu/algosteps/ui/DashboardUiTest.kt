package edu.algosteps.ui

import androidx.compose.material3.MaterialTheme
import edu.algosteps.data.SubmissionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun aLearnerCanChooseEitherTrackFromHome() {
        val opened = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                HomeContent(DashboardStats(emptyList()), { opened += "learn" }, { opened += "code" }, {}, {})
            }
        }
        compose.onNodeWithText("Browse algorithms").performScrollTo().performClick()
        compose.onNodeWithText("Coding practice").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("learn", "code"), opened) }
    }

    @Test
    fun deletingProgressRequiresAnExplicitConfirmation() {
        var deletions = 0
        compose.setContent {
            MaterialTheme {
                SettingsContent(showHints = true, reminders = false, onHints = {},
                    onReminders = {}, onDeviceSettings = {}, onClear = { deletions++ })
            }
        }
        compose.onNodeWithText("Clear progress").performScrollTo().performClick()
        compose.onNodeWithText("Clear learning progress?").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(0, deletions) }
        compose.onNodeWithText("Clear progress").performScrollTo().performClick()
        compose.onNodeWithText("Delete progress").performClick()
        compose.runOnIdle { assertEquals(1, deletions) }
    }

    @Test
    fun emptyExamHistoryDoesNotReportAZeroPercentResult() {
        val stats = DashboardStats(listOf(SubmissionSummary("linear-search", 0, 4, 1_000L, SubmissionMode.PRACTICE)))
        compose.setContent { MaterialTheme { StatsContent(stats, {}, { _, _ -> }) } }
        compose.onNodeWithText("0% accepted").assertIsDisplayed()
        compose.onNodeWithTag("mode-EXAM").performScrollTo().performClick()
        compose.onNodeWithText("No submissions for this mode.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("0% accepted").assertDoesNotExist()
        compose.onNodeWithText("0 of 0 submissions passed every test.").assertDoesNotExist()
    }

    @Test
    fun statisticsDistinguishPassingCasesFromAnAcceptedSubmission() {
        val stats = DashboardStats(listOf(
            SubmissionSummary("linear-search", 3, 4, 1_000L, SubmissionMode.PRACTICE),
            SubmissionSummary("binary-search", 4, 4, 0L, SubmissionMode.EXAM)
        ))
        compose.setContent { MaterialTheme { StatsContent(stats, {}, { _, _ -> }) } }
        compose.onNodeWithText("50% accepted").assertIsDisplayed()
        compose.onNodeWithTag("mode-EXAM").performScrollTo().performClick()
        compose.onNodeWithText("100% accepted").assertIsDisplayed()
        compose.onNodeWithTag("mode-PRACTICE").performScrollTo().performClick()
        compose.onNodeWithText("0% accepted").assertIsDisplayed()
        compose.onNodeWithText("Needs work only").performScrollTo().performClick()
        compose.onNodeWithText("Needs work").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Accepted").assertDoesNotExist()
    }
}
