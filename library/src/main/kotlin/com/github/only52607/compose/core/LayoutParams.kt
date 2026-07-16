package com.github.only52607.compose.core

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager

/**
 * Creates default [WindowManager.LayoutParams] suitable for a basic floating window.
 *
 * Sets WRAP_CONTENT dimensions, translucency, top-start gravity, default animations,
 * and flags for non-modal, non-focusable interaction.
 * Also sets the appropriate window type based on SDK version and context type.
 *
 * The created parameters include:
 * - WRAP_CONTENT dimensions for both width and height
 * - TRANSLUCENT pixel format for transparency support
 * - START|TOP gravity for positioning
 * - NOT_TOUCH_MODAL and NOT_FOCUSABLE flags
 * - Appropriate window type for overlay permissions
 *
 * @param context The context used to determine the appropriate window type.
 *                Activity contexts use default window type, while non-Activity
 *                contexts use overlay window types that require SYSTEM_ALERT_WINDOW permission.
 * @return [WindowManager.LayoutParams] configured for floating window usage.
 */
internal fun defaultLayoutParams(context: Context) = WindowManager.LayoutParams().apply {
    height = WindowManager.LayoutParams.WRAP_CONTENT
    width = WindowManager.LayoutParams.WRAP_CONTENT
    format = PixelFormat.TRANSLUCENT
    gravity = Gravity.START or Gravity.TOP
    flags =
        (
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL // Allows touches to pass through
                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE // Prevents the window from taking focus (e.g., keyboard)
                or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            )
    // FLAG_HARDWARE_ACCELERATED: windows added directly through WindowManager.addView are
    // software-rendered by default (only Activity windows inherit acceleration from the theme).
    // Modern Compose backs its layers with RenderNodes via GraphicsContext even when the host
    // window is not hardware accelerated, and that combination can leave recomposed content
    // stale on screen until something else forces a window traversal (e.g. updateViewLayout
    // during a drag). Hardware acceleration must be requested before addView; it cannot be
    // enabled later.

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

    disableSystemMoveAnimations()
}

/**
 * Disables platform move animations for floating-window relayouts.
 *
 * Issue https://github.com/ArthurKun21/compose-overlay-window/issues/23 was caused by Android 13/14
 * animating the overlay surface from an earlier position when a moved [WindowManager.LayoutParams.WRAP_CONTENT]
 * window changed size. A drag updates [x] and [y] through [WindowManager.updateViewLayout], then a later Compose
 * resize triggers another relayout. With move animations enabled, the system can visually interpolate the surface
 * from the previous committed position to the current one before applying the new size, which looks like the
 * floating window jumps back and then moves forward.
 *
 * Android 14 exposes this behavior as [WindowManager.LayoutParams.setCanPlayMoveAnimation], documented with the
 * `android:windowNoMoveAnimation` attribute:
 * https://developer.android.com/reference/android/view/WindowManager.LayoutParams#setCanPlayMoveAnimation(boolean)
 *
 * Android 13 has the same underlying private flag but no public API. The fallback below sets
 * `PRIVATE_FLAG_NO_MOVE_ANIMATION` best-effort via reflection, matching the framework source flag used by the public
 * Android 14 API. If reflection is blocked, the library keeps working and only the platform move animation workaround
 * is skipped.
 *
 * This is applied before both [WindowManager.addView] and [WindowManager.updateViewLayout] so it also covers caller
 * supplied layout params, not just [defaultLayoutParams].
 */
internal fun WindowManager.LayoutParams.disableSystemMoveAnimations() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        setCanPlayMoveAnimation(false)
    } else {
        disableSystemMoveAnimationsWithPrivateFlag()
    }
}

/**
 * Best-effort Android 13 compatibility path for `android:windowNoMoveAnimation` before the public setter existed.
 *
 * Source reference for the hidden flag:
 * https://android.googlesource.com/platform/frameworks/base/+/android-13.0.0_r1/core/java/android/view/WindowManager.java
 */
private fun WindowManager.LayoutParams.disableSystemMoveAnimationsWithPrivateFlag() {
    try {
        val layoutParamsClass = WindowManager.LayoutParams::class.java
        val privateFlagsField = layoutParamsClass.getField("privateFlags")
        val noMoveAnimationFlag = layoutParamsClass
            .getField("PRIVATE_FLAG_NO_MOVE_ANIMATION")
            .getInt(null)

        privateFlagsField.setInt(
            this,
            privateFlagsField.getInt(this) or noMoveAnimationFlag,
        )
    } catch (_: ReflectiveOperationException) {
        // The Android 13 fallback is best-effort: if the hidden field or flag is unavailable,
        // keep the public floating-window behavior and only skip the no-move-animation workaround.
    } catch (_: RuntimeException) {
        // Some OEM builds can expose the field but reject hidden/private API access at runtime.
        // Ignore that as well so custom LayoutParams remain usable without crashing the app.
    }
}
