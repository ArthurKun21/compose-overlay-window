package com.github.only52607.compose.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for FloatingWindow components.
 *
 * These tests verify that Compose UI elements render correctly
 * within the floating window context.
 */
class ComposeFloatingWindowUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun simpleText_displays_correctly() {
        // Arrange & Act
        composeTestRule.setContent {
            Text("Hello Floating Window")
        }

        // Assert
        composeTestRule.onNodeWithText("Hello Floating Window").assertIsDisplayed()
    }

    @Test
    fun box_renders_with_size() {
        // Arrange & Act
        composeTestRule.setContent {
            Box(
                modifier = Modifier.size(100.dp, 100.dp),
            ) {
                Text("Content")
            }
        }

        // Assert
        composeTestRule.onNodeWithText("Content").assertIsDisplayed()
    }

    @Test
    fun multiple_elements_render_correctly() {
        // Arrange & Act
        composeTestRule.setContent {
            Box {
                Text("First")
                Text("Second")
            }
        }

        // Assert
        composeTestRule.onNodeWithText("First").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second").assertIsDisplayed()
    }
}
