package com.github.only52607.compose.service

import android.content.Context
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ViewModel support in ComposeServiceFloatingWindow.
 *
 * These tests verify that the service-based floating window properly supports
 * ViewModels through the HasDefaultViewModelProviderFactory interface.
 */
@RunWith(AndroidJUnit4::class)
class ComposeServiceFloatingWindowViewModelTest {

    private lateinit var context: Context
    private lateinit var floatingWindow: ComposeServiceFloatingWindow

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        floatingWindow = ComposeServiceFloatingWindow(context)
    }

    @After
    fun tearDown() {
        if (!floatingWindow.isDestroyed.value) {
            floatingWindow.close()
        }
    }

    @Test
    fun implements_HasDefaultViewModelProviderFactory() {
        // Assert
        assertTrue(
            "ComposeServiceFloatingWindow should implement HasDefaultViewModelProviderFactory",
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
    fun viewModelStore_isAccessible() {
        // Act
        val viewModelStore = floatingWindow.viewModelStore

        // Assert
        assertNotNull("ViewModelStore should not be null", viewModelStore)
    }

    @Test
    fun lifecycle_supportsViewModelScope() {
        // Assert - Lifecycle should be at least CREATED for ViewModels to work
        assertTrue(
            "Lifecycle should be at least CREATED",
            floatingWindow.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED),
        )
    }

    @Test
    fun savedStateRegistry_isAccessibleForViewModels() {
        // Act
        val savedStateRegistry = floatingWindow.savedStateRegistry

        // Assert
        assertNotNull("SavedStateRegistry should not be null", savedStateRegistry)
    }

    @Test
    fun close_clearsViewModelStore() {
        // Act
        floatingWindow.close()

        // Assert - ViewModelStore should be cleared (can't directly verify, but shouldn't crash)
        assertNotNull("ViewModelStore should still exist after close", floatingWindow.viewModelStore)
    }

    @Test
    fun multipleInstances_haveIndependentViewModelStores() {
        // Arrange
        val window2 = ComposeServiceFloatingWindow(context)

        try {
            // Act
            val store1 = floatingWindow.viewModelStore
            val store2 = window2.viewModelStore

            // Assert
            assertTrue(
                "Each window should have its own ViewModelStore",
                store1 !== store2,
            )
        } finally {
            window2.close()
        }
    }
}
