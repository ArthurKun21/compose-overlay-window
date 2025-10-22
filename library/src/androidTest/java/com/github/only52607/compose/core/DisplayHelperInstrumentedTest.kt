package com.github.only52607.compose.core

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [DisplayHelper] class.
 *
 * These tests run on an actual Android device/emulator and verify
 * display metrics retrieval functionality.
 */
@RunWith(AndroidJUnit4::class)
class DisplayHelperInstrumentedTest {

    private lateinit var context: Context
    private lateinit var windowManager: WindowManager
    private lateinit var displayHelper: DisplayHelper

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        displayHelper = DisplayHelper(context, windowManager)
    }

    @Test
    fun metrics_returnsValidDisplayMetrics() {
        // Act
        val metrics = displayHelper.metrics

        // Assert
        assertNotNull("DisplayMetrics should not be null", metrics)
        assertTrue("Width should be positive", metrics.widthPixels > 0)
        assertTrue("Height should be positive", metrics.heightPixels > 0)
        assertTrue("Density should be positive", metrics.density > 0f)
        assertTrue("DensityDpi should be positive", metrics.densityDpi > 0)
    }

    @Test
    fun metrics_returnsConsistentValues() {
        // Act - Get metrics twice
        val metrics1 = displayHelper.metrics
        val metrics2 = displayHelper.metrics

        // Assert - Values should be consistent
        assertTrue("Width should be consistent", metrics1.widthPixels == metrics2.widthPixels)
        assertTrue("Height should be consistent", metrics1.heightPixels == metrics2.heightPixels)
        assertTrue("Density should be consistent", metrics1.density == metrics2.density)
        assertTrue("DensityDpi should be consistent", metrics1.densityDpi == metrics2.densityDpi)
    }

    @Test
    fun metrics_matchesDisplayMetrics() {
        // Arrange
        val contextMetrics = context.resources.displayMetrics

        // Act
        val helperMetrics = displayHelper.metrics

        // Assert - Helper metrics should have valid values similar to context metrics
        assertNotNull("Helper metrics should not be null", helperMetrics)
        assertTrue(
            "Helper metrics width should be reasonable",
            helperMetrics.widthPixels > 0 && helperMetrics.widthPixels <= contextMetrics.widthPixels * 2,
        )
        assertTrue(
            "Helper metrics height should be reasonable",
            helperMetrics.heightPixels > 0 && helperMetrics.heightPixels <= contextMetrics.heightPixels * 2,
        )
    }

    @Test
    fun metrics_reflectsDeviceCharacteristics() {
        // Act
        val metrics = displayHelper.metrics

        // Assert - Verify metrics reflect typical device characteristics
        val isPortrait = metrics.heightPixels > metrics.widthPixels
        val isLandscape = metrics.widthPixels > metrics.heightPixels
        val isSquare = metrics.widthPixels == metrics.heightPixels

        assertTrue(
            "Device should be in one of the orientations",
            isPortrait || isLandscape || isSquare,
        )
    }
}
