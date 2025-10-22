package com.github.only52607.compose.benchmark

import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.only52607.compose.window.ComposeFloatingWindow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark tests for CoreFloatingWindow creation and lifecycle operations.
 *
 * These tests measure the performance of key floating window operations
 * to ensure they meet performance requirements and to detect regressions.
 */
@RunWith(AndroidJUnit4::class)
class FloatingWindowCreationBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun benchmark_windowCreation() {
        benchmarkRule.measureRepeated {
            val window = ComposeFloatingWindow(context)
            runWithTimingDisabled {
                window.close()
            }
        }
    }

    @Test
    fun benchmark_windowCreationAndDestruction() {
        benchmarkRule.measureRepeated {
            val window = ComposeFloatingWindow(context)
            window.close()
        }
    }

    @Test
    fun benchmark_multipleWindowCreation() {
        benchmarkRule.measureRepeated {
            val windows = List(5) { ComposeFloatingWindow(context) }
            runWithTimingDisabled {
                windows.forEach { it.close() }
            }
        }
    }

    @Test
    fun benchmark_windowParameterUpdate() {
        val window = ComposeFloatingWindow(context)

        benchmarkRule.measureRepeated {
            window.updateCoordinate(100, 200)
        }

        window.close()
    }

    @Test
    fun benchmark_coordinateBounding() {
        val maxX = 1000
        val maxY = 2000

        benchmarkRule.measureRepeated {
            val targetX = 1500
            val targetY = 2500

            val boundedX = targetX.coerceIn(0, maxX)
            val boundedY = targetY.coerceIn(0, maxY)

            // Use the values to prevent optimization
            boundedX + boundedY
        }
    }

    @Test
    fun benchmark_stateFlowAccess() {
        val window = ComposeFloatingWindow(context)

        benchmarkRule.measureRepeated {
            val isShowing = window.isShowing.value
            val isDestroyed = window.isDestroyed.value

            // Use the values to prevent optimization
            isShowing || isDestroyed
        }

        window.close()
    }

    @Test
    fun benchmark_lifecycleAccess() {
        val window = ComposeFloatingWindow(context)

        benchmarkRule.measureRepeated {
            val state = window.lifecycle.currentState

            // Use the value to prevent optimization
            state.toString()
        }

        window.close()
    }

    @Test
    fun benchmark_viewModelStoreAccess() {
        val window = ComposeFloatingWindow(context)

        benchmarkRule.measureRepeated {
            val store = window.viewModelStore

            // Use the value to prevent optimization
            store.toString()
        }

        window.close()
    }
}
