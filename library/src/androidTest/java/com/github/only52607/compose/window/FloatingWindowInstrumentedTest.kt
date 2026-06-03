package com.github.only52607.compose.window

import android.os.ParcelFileDescriptor
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.only52607.compose.service.ComposeServiceFloatingWindow
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FloatingWindowInstrumentedTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val windows = mutableListOf<ComposeServiceFloatingWindow>()

    @Before
    fun allowOverlayWindows() {
        executeShellCommand("cmd appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
    }

    @After
    fun closeWindows() {
        instrumentation.runOnMainSync {
            windows.forEach { window ->
                window.close()
            }
            windows.clear()
        }
    }

    @Test
    fun showMovesLifecycleToStartedImmediately() {
        val window = newWindow()

        instrumentation.runOnMainSync {
            window.setContent {
                Text("Ready")
            }
            window.show()
        }

        assertTrue(
            "Floating window lifecycle should be STARTED immediately after show().",
            window.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED),
        )
    }

    @Test
    fun composeContentRecomposesAfterStateChangeWithoutMovingWindow() {
        val window = newWindow()
        var label by mutableStateOf("Before")
        val initialComposition = CountDownLatch(1)
        val updatedComposition = CountDownLatch(1)

        instrumentation.runOnMainSync {
            window.setContent {
                Text(text = label)
                SideEffect {
                    when (label) {
                        "Before" -> initialComposition.countDown()
                        "After" -> updatedComposition.countDown()
                    }
                }
            }
            window.show()
        }

        assertTrue(
            "Initial floating window composition did not run.",
            initialComposition.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
        )

        instrumentation.runOnMainSync {
            label = "After"
        }

        assertTrue(
            "Floating window did not recompose after state changed.",
            updatedComposition.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
        )
    }

    private fun newWindow(): ComposeServiceFloatingWindow {
        lateinit var window: ComposeServiceFloatingWindow
        instrumentation.runOnMainSync {
            window = ComposeServiceFloatingWindow(context)
            windows += window
            assertTrue(
                "SYSTEM_ALERT_WINDOW app-op was not granted for the test package.",
                window.isAvailable(),
            )
        }
        return window
    }

    private fun executeShellCommand(command: String) {
        val descriptor: ParcelFileDescriptor = instrumentation.uiAutomation.executeShellCommand(command)
        FileInputStream(descriptor.fileDescriptor).bufferedReader().use { it.readText() }
        descriptor.close()
    }

    private companion object {
        const val TIMEOUT_SECONDS = 3L
    }
}
