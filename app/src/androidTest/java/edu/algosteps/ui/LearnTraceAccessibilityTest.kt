package edu.algosteps.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import edu.algosteps.content.Curriculum
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.junit.Rule
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class LearnTraceAccessibilityTest {
    @get:Rule val screen = createComposeRule()

    @Test fun oneLabelPerCellTracksTheShrinkingInterval() {
        val lesson = Curriculum.byId("binary-search")
        screen.setContent { MaterialTheme { LearnScreen(lesson, {}, {}, {}) } }
        screen.onNodeWithContentDescription("Index 2, value 7, middle of the search interval").assertExists()
        // The visual index is included in the cell's description instead of forming a second stop.
        screen.onNodeWithText("[2]").assertDoesNotExist()

        screen.onNodeWithText("Next step").performScrollTo().performClick()
        screen.onNodeWithContentDescription("Index 2, value 7, ruled out").assertExists()
        screen.onNodeWithContentDescription("Index 4, value 16, middle of the search interval").assertExists()
        screen.onNodeWithText("Replay").performScrollTo().performClick()
        screen.onNodeWithContentDescription("Index 2, value 7, middle of the search interval").assertExists()
    }
}
