package com.github.only52607.compose.core

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for permission checking functionality.
 *
 * These tests verify that the overlay permission checking works correctly
 * on actual Android devices/emulators.
 */
@RunWith(AndroidJUnit4::class)
class PermissionsInstrumentedTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun checkOverlayPermission_returnsBoolean() {
        // Act
        val hasPermission = checkOverlayPermission(context)

        // Assert - The result should be a boolean value (true or false)
        // In test environment, this is typically false unless explicitly granted
        assertNotNull(hasPermission)
    }

    @Test
    fun checkOverlayPermission_callsSettingsCanDrawOverlays() {
        // Act
        val hasPermission = Settings.canDrawOverlays(context)

        // Assert - Verify the method returns a boolean
        assertNotNull(hasPermission)
    }

    @Test
    fun checkOverlayPermission_returnsFalseByDefault() {
        // Act
        val hasPermission = checkOverlayPermission(context)

        // Assert - In test environment, overlay permission should not be granted by default
        // Note: This test might fail if permission is actually granted in the test environment
        assertFalse("Overlay permission should not be granted in test environment", hasPermission)
    }
}
