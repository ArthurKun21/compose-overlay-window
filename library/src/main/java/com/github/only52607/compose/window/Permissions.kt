package com.github.only52607.compose.window

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri

/**
 * Request permission to draw over other apps.
 */
fun requestOverlayPermission(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        "package:${context.packageName}".toUri()
    )

    context.startActivity(intent)
}

/**
 * Check if the permission to draw over other apps is granted.
 */
fun checkOverlayPermission(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}