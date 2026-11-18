package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name matches ChessHub`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("ChessHub", context.getString(R.string.app_name))
    }

    @Test
    fun `continue offline navigates to home and interactive chessboard`() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag("continue_offline_button")).fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodes(hasTestTag("nav_play")).fetchSemanticsNodes().isNotEmpty()
        }

        val offlineNodes = composeRule.onAllNodes(hasTestTag("continue_offline_button"))
            .fetchSemanticsNodes()
        if (offlineNodes.isNotEmpty()) {
            composeRule.onNodeWithTag("continue_offline_button").performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithTag("nav_play").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chess_board").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_learn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chess_board").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_analyze").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("chess_board").assertIsDisplayed()
    }
}
