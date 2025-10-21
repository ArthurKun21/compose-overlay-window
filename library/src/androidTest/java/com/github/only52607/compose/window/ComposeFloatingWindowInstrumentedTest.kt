package com.github.only52607.compose.window

import android.content.Context
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [ComposeFloatingWindow] class.
 *
 * These tests verify the Compose-specific functionality and integration
 * with ViewModels and lifecycle components.
 */
@RunWith(AndroidJUnit4::class)
class ComposeFloatingWindowInstrumentedTest {

    private lateinit var context: Context
    private lateinit var floatingWindow: ComposeFloatingWindow

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        floatingWindow = ComposeFloatingWindow(context)
    }

    @After
    fun tearDown() {
        if (!floatingWindow.isDestroyed.value) {
            floatingWindow.close()
        }
    }

    @Test
    fun initialization_createsValidComposeWindow() {
        // Assert
        assertNotNull("Floating window should not be null", floatingWindow)
        assertNotNull("DecorView should not be null", floatingWindow.decorView)
        assertNotNull("WindowParams should not be null", floatingWindow.windowParams)
    }

    @Test
    fun implements_HasDefaultViewModelProviderFactory() {
        // Assert
        assertTrue(
            "ComposeFloatingWindow should implement HasDefaultViewModelProviderFactory",
            floatingWindow is HasDefaultViewModelProviderFactory,
        )
    }

    @Test
    fun defaultViewModelProviderFactory_isNotNull() {
        // Act
        val factory = (floatingWindow as HasDefaultViewModelProviderFactory).defaultViewModelProviderFactory

        // Assert
        assertNotNull("Default ViewModelProviderFactory should not be null", factory)
    }

    @Test
    fun initialState_isCorrect() = runTest {
        // Assert
        floatingWindow.isShowing.test {
            assertFalse("Window should not be showing initially", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        floatingWindow.isDestroyed.test {
            assertFalse("Window should not be destroyed initially", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun lifecycle_startsInCreatedState() {
        // Assert
        assertTrue(
            "Lifecycle should be at least CREATED",
            floatingWindow.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED),
        )
    }

    @Test
    fun close_destroysWindow() = runTest {
        // Act
        floatingWindow.close()

        // Assert
        floatingWindow.isDestroyed.test {
            assertTrue("Window should be destroyed after close", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun viewModelStore_isAccessible() {
        // Act
        val viewModelStore = floatingWindow.viewModelStore

        // Assert
        assertNotNull("ViewModelStore should not be null", viewModelStore)
    }

    @Test
    fun savedStateRegistry_isAccessible() {
        // Act
        val savedStateRegistry = floatingWindow.savedStateRegistry

        // Assert
        assertNotNull("SavedStateRegistry should not be null", savedStateRegistry)
    }

    @Test
    fun display_providesValidMetrics() {
        // Act
        val metrics = floatingWindow.display.metrics

        // Assert
        assertNotNull("Display metrics should not be null", metrics)
        assertTrue("Width should be positive", metrics.widthPixels > 0)
        assertTrue("Height should be positive", metrics.heightPixels > 0)
    }

    @Test
    fun checkDestroyed_throwsAfterClose() {
        // Arrange
        floatingWindow.close()

        // Act & Assert
        try {
            floatingWindow.checkDestroyed()
            throw AssertionError("checkDestroyed should have thrown IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(
                "Exception message should mention destroyed",
                e.message?.contains("destroyed") == true,
            )
        }
    }
}
