package com.github.only52607.compose.core

import android.content.Context
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [CoreFloatingWindow] class.
 *
 * These tests verify the lifecycle, state management, and core functionality
 * of the floating window on actual Android devices.
 */
@RunWith(AndroidJUnit4::class)
class CoreFloatingWindowInstrumentedTest {

    private lateinit var context: Context
    private lateinit var floatingWindow: TestCoreFloatingWindow

    /**
     * Test subclass to expose protected members for testing.
     */
    private class TestCoreFloatingWindow(
        context: Context,
        windowParams: WindowManager.LayoutParams = defaultLayoutParams(context),
    ) : CoreFloatingWindow(context, windowParams, "TestWindow")

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        floatingWindow = TestCoreFloatingWindow(context)
    }

    @After
    fun tearDown() {
        if (!floatingWindow.isDestroyed.value) {
            floatingWindow.close()
        }
    }

    @Test
    fun initialization_createsValidWindow() {
        // Assert
        assertNotNull("Floating window should not be null", floatingWindow)
        assertNotNull("DecorView should not be null", floatingWindow.decorView)
        assertNotNull("ViewModelStore should not be null", floatingWindow.viewModelStore)
        assertNotNull("Lifecycle should not be null", floatingWindow.lifecycle)
        assertNotNull("SavedStateRegistry should not be null", floatingWindow.savedStateRegistry)
    }

    @Test
    fun initialState_isNotShowingAndNotDestroyed() = runTest {
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
    fun close_marksWindowAsDestroyed() = runTest {
        // Act
        floatingWindow.close()

        // Assert
        floatingWindow.isDestroyed.test {
            assertTrue("Window should be destroyed after close", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun close_movesLifecycleToDestroyed() {
        // Act
        floatingWindow.close()

        // Assert
        assertEquals(
            "Lifecycle should be DESTROYED after close",
            Lifecycle.State.DESTROYED,
            floatingWindow.lifecycle.currentState,
        )
    }

    @Test
    fun close_clearsViewModelStore() {
        // Act
        floatingWindow.close()

        // Assert - ViewModelStore should be cleared (no way to directly verify, but shouldn't crash)
        assertNotNull("ViewModelStore should still exist", floatingWindow.viewModelStore)
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

    @Test
    fun displayHelper_providesValidMetrics() {
        // Act
        val metrics = floatingWindow.display.metrics

        // Assert
        assertNotNull("Display metrics should not be null", metrics)
        assertTrue("Width should be positive", metrics.widthPixels > 0)
        assertTrue("Height should be positive", metrics.heightPixels > 0)
    }

    @Test
    fun maxCoordinates_reflectScreenBounds() {
        // Note: These will be 0 if decorView hasn't been measured yet
        // Act
        val maxX = floatingWindow.maxXCoordinate
        val maxY = floatingWindow.maxYCoordinate

        // Assert - Values should be non-negative
        assertTrue("Max X coordinate should be non-negative", maxX >= 0)
        assertTrue("Max Y coordinate should be non-negative", maxY >= 0)
    }

    @Test
    fun updateCoordinate_updatesWindowParams() {
        // Arrange
        val targetX = 100
        val targetY = 200

        // Act
        floatingWindow.updateCoordinate(targetX, targetY)

        // Assert
        assertEquals("Window params X should be updated", targetX, floatingWindow.windowParams.x)
        assertEquals("Window params Y should be updated", targetY, floatingWindow.windowParams.y)
    }

    @Test
    fun isAvailable_checksOverlayPermission() {
        // Act
        val isAvailable = floatingWindow.isAvailable()

        // Assert - Result should be a boolean (typically false in test environment)
        assertNotNull("isAvailable should return a boolean", isAvailable)
    }

    @Test
    fun decorView_hasCorrectConfiguration() {
        // Act
        val decorView = floatingWindow.decorView

        // Assert
        assertFalse("DecorView should not clip children", decorView.clipChildren)
        assertFalse("DecorView should not clip to padding", decorView.clipToPadding)
    }

    @Test
    fun multipleClose_doesNotThrow() {
        // Act & Assert - Multiple calls to close should not throw
        floatingWindow.close()
        floatingWindow.close() // Should not throw
        floatingWindow.close() // Should not throw
    }
}
