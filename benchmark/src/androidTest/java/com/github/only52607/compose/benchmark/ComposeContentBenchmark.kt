package com.github.only52607.compose.benchmark

import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.only52607.compose.window.ComposeFloatingWindow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark tests for Compose content rendering in floating windows.
 *
 * These tests measure the performance of setting and updating
 * Compose content within a floating window.
 */
@RunWith(AndroidJUnit4::class)
class ComposeContentBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private lateinit var window: ComposeFloatingWindow

    @Before
    fun setup() {
        window = ComposeFloatingWindow(context)
    }

    @After
    fun teardown() {
        if (!window.isDestroyed.value) {
            window.close()
        }
    }

    @Test
    fun benchmark_setSimpleContent() {
        benchmarkRule.measureRepeated {
            window.setContent {
                Text("Hello World")
            }
            runWithTimingDisabled {
                // Cleanup composition for next iteration
                window.disposeCompositionIfNeeded()
            }
        }
    }

    @Test
    fun benchmark_setComplexContent() {
        benchmarkRule.measureRepeated {
            window.setContent {
                Box(modifier = Modifier.size(200.dp)) {
                    Text("Title")
                    Text("Subtitle")
                    Text("Body text")
                }
            }
            runWithTimingDisabled {
                window.disposeCompositionIfNeeded()
            }
        }
    }

    @Test
    fun benchmark_updateContent() {
        var counter = 0
        window.setContent {
            Text("Count: $counter")
        }

        benchmarkRule.measureRepeated {
            counter++
            window.setContent {
                Text("Count: $counter")
            }
        }
    }

    @Test
    fun benchmark_multipleContentChanges() {
        benchmarkRule.measureRepeated {
            // Set content multiple times
            repeat(5) { index ->
                window.setContent {
                    Text("Content $index")
                }
            }
            runWithTimingDisabled {
                window.disposeCompositionIfNeeded()
            }
        }
    }
}
