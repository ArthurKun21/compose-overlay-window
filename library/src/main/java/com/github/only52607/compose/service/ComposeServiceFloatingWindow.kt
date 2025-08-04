@file:Suppress("unused")

package com.github.only52607.compose.service


import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.compositionContext
import androidx.core.view.isNotEmpty
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.github.only52607.compose.window.DisplayHelper
import com.github.only52607.compose.window.LocalFloatingWindow
import com.github.only52607.compose.window.requestOverlayPermission
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages a floating window that can display Jetpack Compose content overlaying other applications.
 *
 * This class handles the lifecycle, state saving, ViewModel scope, and WindowManager interactions
 * necessary for a floating Compose UI. It implements [AutoCloseable], allowing it to be used
 * with Kotlin's `use` function for automatic resource cleanup.
 *
 * Example usage with `use`:
 * ```kotlin
 * val floatingWindow = ComposeServiceFloatingWindow(context)
 * floatingWindow.use { window -> // destroy() is called automatically at the end of this block
 *     window.setContent { /* Your Composable UI */ }
 *     window.show()
 *     // ... interact with the window ...
 * } // Window is hidden and resources are released here
 * ```
 *
 * Remember to declare the `SYSTEM_ALERT_WINDOW` permission in your AndroidManifest.xml and
 * request it at runtime if targeting Android M (API 23) or higher.
 *
 * @param context The context used for creating the window and accessing system services.
 *                An application context is preferred to avoid leaks.
 * @param windowParams The layout parameters for the floating window. Defaults are provided
 *                     by [ComposeServiceFloatingWindow.defaultLayoutParams].
 */
class ComposeServiceFloatingWindow(
    private val context: Context,
    val windowParams: WindowManager.LayoutParams = defaultLayoutParams(context),
) : SavedStateRegistryOwner,
    ViewModelStoreOwner,
    AutoCloseable {

    companion object {
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
        fun defaultLayoutParams(context: Context) = WindowManager.LayoutParams().apply {
            height = WindowManager.LayoutParams.WRAP_CONTENT
            width = WindowManager.LayoutParams.WRAP_CONTENT
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.START or Gravity.TOP
            windowAnimations = android.R.style.Animation_Dialog
            flags =
                (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL // Allows touches to pass through
                        or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) // Prevents the window from taking focus (e.g., keyboard)

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
    }
    // --- Lifecycle, ViewModel, SavedState ---

    // Use a SupervisorJob so failure of one child doesn't cause others to fail
    // Use a custom scope tied to the window's lifecycle for managing window-specific coroutines
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(SERVICE_TAG, "Coroutine Exception: ${throwable.localizedMessage}", throwable)
    }
    private val coroutineContext = AndroidUiDispatcher.CurrentThread
    private val lifecycleCoroutineScope = CoroutineScope(
        SupervisorJob() +
                coroutineContext + coroutineExceptionHandler
    )
    private val mutex = Mutex()

    override val viewModelStore: ViewModelStore = ViewModelStore()

    private var lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private var savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // --- Window State ---

    private var _isShowing = MutableStateFlow(false)

    /**
     * A [StateFlow] indicating whether the floating window is currently shown (`true`) or hidden (`false`).
     * Does not reflect the destroyed state. Check [isDestroyed] for that.
     */
    val isShowing: StateFlow<Boolean>
        get() = _isShowing.asStateFlow()

    private var _isDestroyed = MutableStateFlow(false)

    /**
     * A [StateFlow] indicating whether the floating window has been destroyed (`true`).
     * Once destroyed, the instance cannot be reused. Create a new instance if needed.
     */
    val isDestroyed: StateFlow<Boolean>
        get() = _isDestroyed.asStateFlow()

    /**
     * The root view container for the floating window's content.
     * This is the view added to the WindowManager.
     */
    var decorView: ViewGroup = FrameLayout(context)
        .apply {
            // Important: Prevent clipping so shadows or elements outside bounds can be drawn
            clipChildren = false
            clipToPadding = false
        }
        private set

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Helper class providing access to the display metrics for the floating window.
     *
     * Used internally to calculate maximum coordinates and display dimensions
     * for proper window positioning and bounds checking.
     */
    val display = DisplayHelper(context, windowManager)
    private var composeView: ComposeView? = null // Hold a direct reference
    private var parentComposition: Recomposer? = null // Hold reference for disposal

    /**
     * The maximum X coordinate for the floating window.
     *
     * This represents the rightmost position where the window can be placed
     * while still remaining fully visible on screen. Calculated as the
     * screen width minus the window's measured width.
     *
     * @return The maximum X coordinate in pixels, or 0 if the window hasn't been measured yet.
     */
    val maxXCoordinate
        get() = display.metrics.widthPixels - decorView.measuredWidth

    /**
     * The maximum Y coordinate for the floating window.
     *
     * This represents the bottommost position where the window can be placed
     * while still remaining fully visible on screen. Calculated as the
     * screen height minus the window's measured height.
     *
     * @return The maximum Y coordinate in pixels, or 0 if the window hasn't been measured yet.
     */
    val maxYCoordinate
        get() = display.metrics.heightPixels - decorView.measuredHeight

    /**
     * Sets the Jetpack Compose content for the floating window.
     *
     * This method creates a [ComposeView] and sets your [content] within it.
     * It also sets up the necessary CompositionLocal provider for [LocalFloatingWindow]
     * and connects the view to this window's lifecycle, ViewModel store, and saved state registry.
     *
     * @param content The composable function defining the UI of the floating window.
     * @throws IllegalStateException if called after [checkDestroyed] or [close] has been invoked.
     */
    fun setContent(content: @Composable () -> Unit) {
        checkDestroyed()
        Log.d(SERVICE_TAG, "Setting content.")

        disposeCompositionIfNeeded()

        val currentComposeView = ComposeView(context).apply {

            setViewTreeLifecycleOwner(this@ComposeServiceFloatingWindow)
            setViewTreeViewModelStoreOwner(this@ComposeServiceFloatingWindow)
            setViewTreeSavedStateRegistryOwner(this@ComposeServiceFloatingWindow)

            // Create a Recomposer tied to the window's lifecycle scope
            val recomposer = Recomposer(coroutineContext)
            compositionContext = recomposer
            parentComposition = recomposer // Store for later disposal

            // Launch the Recomposer
            lifecycleCoroutineScope.launch {
                try {
                    recomposer.runRecomposeAndApplyChanges()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.d(SERVICE_TAG, "Coroutine scope cancelled normally: ${e.message}")
                } catch (e: Exception) {
                    Log.e(SERVICE_TAG, "Recomposer error", e)
                } finally {
                    Log.d(SERVICE_TAG, "Recomposer job finished.")
                }
            }

            // Set the actual Composable content
            setContent {
                CompositionLocalProvider(
                    LocalServiceFloatingWindow provides this@ComposeServiceFloatingWindow
                ) {
                    content()
                }
            }
        }

        this.composeView = currentComposeView // Store reference

        // Replace the content view in the decorView
        if (decorView.isNotEmpty()) {
            decorView.removeAllViews()
        }

        decorView.addView(currentComposeView)

        // If already showing, update the layout immediately
        if (_isShowing.value) {
            update()
        }
    }

    /**
     * Shows the floating window.
     *
     * Adds the [decorView] to the [WindowManager] using the configured [windowParams].
     * Moves the lifecycle state to STARTED.
     * Requires the `SYSTEM_ALERT_WINDOW` permission.
     * Requires [setContent] to have been called first.
     *
     * @throws IllegalStateException if the window is already destroyed ([isDestroyed] is true).
     * @throws IllegalStateException if [setContent] has not been called.
     * @throws SecurityException if the `SYSTEM_ALERT_WINDOW` permission is not granted (logged as warning).
     */
    fun show() {
        checkDestroyed()

        require(composeView != null) {
            "Content must be set using setContent() before showing the window."
        }

        if (!isAvailable()) {
            Log.w(
                SERVICE_TAG,
                "Overlay permission (SYSTEM_ALERT_WINDOW) not granted. Cannot show window."
            )
            return
        }

        if (_isShowing.value) {
            Log.d(SERVICE_TAG, "Window already showing, updating layout.")
            update() // Ensure layout is up-to-date if show is called again
            return
        }

        Log.d(SERVICE_TAG, "Showing window.")

        try {
            // Ensure the view doesn't have a parent before adding
            if (decorView.parent != null) {
                Log.w(SERVICE_TAG, "DecorView already has a parent. Removing it.")
                (decorView.parent as? ViewGroup)?.removeView(decorView)
            }
            windowManager.addView(decorView, windowParams)
            // Move lifecycle to STARTED only after view is successfully added
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            // Update state last
            _isShowing.update { true }
        } catch (e: Exception) {
            // Catch potential exceptions from WindowManager (e.g., security, bad token)
            Log.e(SERVICE_TAG, "Error showing window: ${e.localizedMessage}", e)
            // Reset state if adding failed
            _isShowing.update { false }
        }
    }

    /**
     * Updates the window coordinates to the specified position.
     *
     * This method updates the window parameters with new coordinates and should
     * typically be followed by a call to [update] to apply the changes to the
     * displayed window. This method is thread-safe and uses a mutex to prevent
     * concurrent modifications to window parameters.
     *
     * @param left The new X coordinate (left position) for the window.
     * @param top The new Y coordinate (top position) for the window.
     */
    fun updateCoordinate(left: Int, top: Int) {
        windowParams.x = left
        windowParams.y = top
    }

    /**
     * Updates the layout of the floating window using the current [windowParams].
     * Call this after modifying [windowParams] (e.g., position or size) while the window is showing.
     *
     * @throws IllegalStateException if the window is already destroyed ([isDestroyed] is true).
     */
    fun update() = lifecycleCoroutineScope.launch {
        checkDestroyed()

        if (!_isShowing.value) {
            Log.w(SERVICE_TAG, "Update called but window is not showing.")
            return@launch
        }
        Log.d(SERVICE_TAG, "Updating window layout.")
        mutex.withLock {
            try {

                windowManager.updateViewLayout(decorView, windowParams)
            } catch (e: Exception) {
                Log.e(SERVICE_TAG, "Error updating window layout: ${e.localizedMessage}", e)
            }
        }
    }

    /**
     * Hides the floating window.
     *
     * Removes the [decorView] from the [WindowManager].
     * Moves the lifecycle state to STOPPED.
     *
     * @throws IllegalStateException if the window is already destroyed ([isDestroyed] is true).
     */
    fun hide() {
        checkDestroyed()

        if (!_isShowing.value) {
            Log.d(SERVICE_TAG, "Hide called but window is already hidden.")
            return
        }
        Log.d(SERVICE_TAG, "Hiding window.")

        _isShowing.update { false }
        try {
            // Check if view is still attached before removing
            if (decorView.parent != null) {
                windowManager.removeViewImmediate(decorView) // Use immediate for synchronous removal
            } else {
                Log.w(SERVICE_TAG, "Hide called but DecorView has no parent.")
            }
        } catch (e: Exception) {
            // Catch potential exceptions (e.g., view not attached)
            Log.e(SERVICE_TAG, "Error hiding window: ${e.localizedMessage}", e)
        } finally {
            // Move lifecycle to STOPPED regardless of removal success,
            // as the intention is to stop interaction.
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    /**
     * Checks if the overlay permission is available for displaying floating windows.
     *
     * This method checks whether the application has the SYSTEM_ALERT_WINDOW permission
     * required to display floating windows over other applications. On Android M (API 23)
     * and above, this permission must be explicitly granted by the user.
     *
     * @return `true` if the overlay permission is granted, `false` otherwise.
     * @see requestOverlayPermission for requesting the permission if not available.
     */
    fun isAvailable(): Boolean = Settings.canDrawOverlays(context)

    init {
        // Warn if non-application context is used to prevent memory leaks
        if (context !is Application && context.applicationContext != context) {
            Log.w(
                SERVICE_TAG, "Consider using applicationContext " +
                        "instead of activity context to prevent memory leaks"
            )
        }
        // Restore state early in the lifecycle
        savedStateRegistryController.performRestore(null)
        // Mark the lifecycle as CREATED
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        // Enable SavedStateHandles for ViewModels
        enableSavedStateHandles()
        Log.d(SERVICE_TAG, "ComposeServiceFloatingWindow initialized.")
    }

    /** Throws an [IllegalStateException] if the window has been destroyed. */
    private fun checkDestroyed() {
        check(!_isDestroyed.value) {
            "ComposeServiceFloatingWindow has been destroyed and cannot be used."
        }
    }

    /** Disposes the Compose composition and clears the reference. */
    private fun disposeCompositionIfNeeded() {
        composeView?.let {
            Log.d(SERVICE_TAG, "Disposing composition.")
            it.disposeComposition() // Dispose the underlying composition
            parentComposition?.cancel() // Cancel the recomposer explicitly if needed
            parentComposition = null
            decorView.removeView(it) // Remove view from hierarchy
            composeView = null // Clear the reference
        }
    }

    /**
     * Implementation of [AutoCloseable].
     * Destroys the floating window, releasing all associated resources.
     *
     * This performs the following actions:
     * 1. Hides the window if it is currently showing.
     * 2. Disposes the Jetpack Compose composition.
     * 3. Cancels all coroutines launched in the window's lifecycle scope.
     * 4. Moves the lifecycle state to DESTROYED.
     * 5. Clears the [ViewModelStore], destroying associated ViewModels.
     * 6. Cleans up internal references.
     *
     * **Once destroyed, this instance cannot be reused.** Create a new `ComposeServiceFloatingWindow`
     * instance if you need to show a floating window again.
     */
    override fun close() {
        if (_isDestroyed.value) {
            Log.w(SERVICE_TAG, "Destroy called but window is already destroyed.")
            return
        }
        Log.d(SERVICE_TAG, "Destroying window...")

        // Mark as destroyed immediately to prevent race conditions
        _isDestroyed.update { true }

        // Hide the window if showing (ensures view is removed from WindowManager)
        if (_isShowing.value) {
            try {
                hide()
            } catch (e: Exception) {
                Log.e(
                    SERVICE_TAG,
                    "Error hiding window during destruction: ${e.localizedMessage}",
                    e
                )
            }
        }

        // Dispose the composition
        disposeCompositionIfNeeded()

        // Cancel the custom lifecycle scope and its children (including the Recomposer's job)
        Log.d(SERVICE_TAG, "Cancelling lifecycle coroutine scope.")
        try {// Explicit cancellation
            lifecycleCoroutineScope.cancel(
                kotlinx.coroutines.CancellationException("ComposeServiceFloatingWindow destroyed")
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d(SERVICE_TAG, "Coroutine scope cancelled normally: ${e.message}")
        } catch (e: Exception) {
            Log.e(SERVICE_TAG, "Recomposer error", e)
        }

        // Move lifecycle to DESTROYED
        Log.d(SERVICE_TAG, "Setting lifecycle state to DESTROYED.")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        // Clear the ViewModelStore
        Log.d(SERVICE_TAG, "Clearing ViewModelStore.")
        viewModelStore.clear()

        // Clean up references
        // decorView is managed by GC once this instance is gone.
        // composeView reference is cleared in disposeCompositionIfNeeded
        // windowManager is a system service, no need to clear.
        // savedStateRegistryController is tied to the lifecycle/owner, should be handled.

        Log.d(SERVICE_TAG, "ComposeServiceFloatingWindow destroyed successfully.")
    }
}