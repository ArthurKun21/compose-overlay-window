package com.github.only52607.compose.window


import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 *
 * This class provides a way to retrieve the display metrics of the current window.
 *
 */
class DisplayHelper(
    private val context: Context,
    private val windowManager: WindowManager,
) {

    val metrics: DisplayMetrics
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val bounds = windowManager.currentWindowMetrics.bounds
            val contextMetrics = context.resources.displayMetrics

            val defaultMetrics = DisplayMetrics().apply {
                widthPixels = bounds.width()
                heightPixels = bounds.height()
                density = contextMetrics.density
                densityDpi = contextMetrics.densityDpi
            }

            defaultMetrics
        } else {
            DisplayMetrics().also {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(it)
            }
        }
}