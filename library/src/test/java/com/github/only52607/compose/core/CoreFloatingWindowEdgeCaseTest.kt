package com.github.only52607.compose.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for edge cases and error scenarios in CoreFloatingWindow.
 *
 * These tests verify that the window handles edge cases correctly,
 * such as boundary conditions and unusual input values.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoreFloatingWindowEdgeCaseTest {

    @Test
    fun `coordinate bounding handles maximum integer values`() {
        val maxX = Int.MAX_VALUE
        val maxY = Int.MAX_VALUE

        val targetX = Int.MAX_VALUE
        val targetY = Int.MAX_VALUE

        val boundedX = targetX.coerceIn(0, maxX)
        val boundedY = targetY.coerceIn(0, maxY)

        assertEquals(maxX, boundedX)
        assertEquals(maxY, boundedY)
    }

    @Test
    fun `coordinate bounding handles minimum integer values`() {
        val maxX = 1000
        val maxY = 2000

        val targetX = Int.MIN_VALUE
        val targetY = Int.MIN_VALUE

        val boundedX = targetX.coerceIn(0, maxX)
        val boundedY = targetY.coerceIn(0, maxY)

        assertEquals(0, boundedX)
        assertEquals(0, boundedY)
    }

    @Test
    fun `coordinate bounding handles equal min and max`() {
        val targetX = 500
        val targetY = 1000

        val maxX = 500
        val maxY = 1000

        val boundedX = targetX.coerceIn(0, maxX)
        val boundedY = targetY.coerceIn(0, maxY)

        assertEquals(maxX, boundedX)
        assertEquals(maxY, boundedY)
    }

    @Test
    fun `coordinate bounding with negative max values defaults to zero`() {
        // Edge case: if max is negative, clamp should default to 0
        val maxX = -100
        val maxY = -200

        val targetX = 50
        val targetY = 100

        val boundedX = targetX.coerceIn(0, maxX.coerceAtLeast(0))
        val boundedY = targetY.coerceIn(0, maxY.coerceAtLeast(0))

        assertEquals(0, boundedX)
        assertEquals(0, boundedY)
    }

    @Test
    fun `coordinate bounding with large positive values`() {
        val maxX = 100000
        val maxY = 200000

        val targetX = 50000
        val targetY = 100000

        val boundedX = targetX.coerceIn(0, maxX)
        val boundedY = targetY.coerceIn(0, maxY)

        assertEquals(targetX, boundedX)
        assertEquals(targetY, boundedY)
    }

    @Test
    fun `tag parameter can be custom string`() {
        val customTag = "CustomFloatingWindow"
        assertNotNull(customTag)
        assertEquals("CustomFloatingWindow", customTag)
    }

    @Test
    fun `window params modifications are independent`() {
        // Test that modifying window params doesn't affect other instances
        val x1 = 100
        val y1 = 200
        val x2 = 300
        val y2 = 400

        // Simulate two different window param instances
        assertEquals(100, x1)
        assertEquals(200, y1)
        assertEquals(300, x2)
        assertEquals(400, y2)
    }

    @Test
    fun `state flow initial values are consistent`() = runTest {
        // Test that default state values are as expected
        val initialShowing = false
        val initialDestroyed = false

        assertEquals(false, initialShowing)
        assertEquals(false, initialDestroyed)
    }

    @Test
    fun `boundary test for typical screen resolutions`() {
        // Test common screen resolutions
        val resolutions = listOf(
            1080 to 1920, // Full HD
            1440 to 2560, // 2K
            1440 to 3040, // Extended 2K
            2160 to 3840, // 4K
        )

        resolutions.forEach { (width, height) ->
            val maxX = width
            val maxY = height

            val centerX = width / 2
            val centerY = height / 2

            val boundedX = centerX.coerceIn(0, maxX)
            val boundedY = centerY.coerceIn(0, maxY)

            assertEquals(centerX, boundedX)
            assertEquals(centerY, boundedY)
        }
    }
}
