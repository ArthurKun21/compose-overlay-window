package com.github.only52607.compose.window.app.ui

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager

fun newLayoutParams(context: Context) = WindowManager.LayoutParams().apply {
    height = WindowManager.LayoutParams.WRAP_CONTENT
    width = WindowManager.LayoutParams.WRAP_CONTENT
    format = PixelFormat.TRANSLUCENT
    gravity = Gravity.START or Gravity.TOP
    windowAnimations = android.R.style.Animation_Dialog
    flags = (
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL // Allows touches to pass through
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE // Prevents the window from taking focus (e.g., keyboard)
            )

    // Set window type correctly for overlays
    // Requires SYSTEM_ALERT_WINDOW permission
    if (context !is Activity) {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
    }
    // If context is an Activity, the default window type associated with the activity is used.
}