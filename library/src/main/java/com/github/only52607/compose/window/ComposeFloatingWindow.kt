package com.github.only52607.compose.window

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
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages a floating window that can display Jetpack Compose content overlaying other applications.
 *
 * This class handles the lifecycle, state saving, ViewModel scope, and WindowManager interactions
 * necessary for a floating Compose UI. It implements [AutoCloseable], allowing it to be used
 * with Kotlin's `use` function for automatic resource cleanup.
 *
 * Example usage with `use`:
 * ```kotlin
 * val floatingWindow = ComposeFloatingWindow(context)
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
 *                     by [ComposeFloatingWindow.defaultLayoutParams].
 */
class ComposeFloatingWindow(
    private val context: Context,
    val windowParams: WindowManager.LayoutParams = defaultLayoutParams(context),
) : SavedStateRegistryOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    AutoCloseable {

    companion object {
        /**
         * Creates default [WindowManager.LayoutParams] suitable for a basic floating window.
         * Sets WRAP_CONTENT dimensions, translucency, top-start gravity, default animations,
         * and flags for non-modal, non-focusable interaction.
         * Also sets the appropriate window type based on SDK version and context type.
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
        Log.e(TAG, "Coroutine Exception: ${throwable.localizedMessage}", throwable)
    }
    private val coroutineContext = AndroidUiDispatcher.CurrentThread
    private val lifecycleCoroutineScope = CoroutineScope(
        SupervisorJob() +
                coroutineContext + coroutineExceptionHandler
    )

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        SavedStateViewModelFactory(
            context.applicationContext as Application,
            this@ComposeFloatingWindow,
            null
        )
    }

    override val defaultViewModelCreationExtras: CreationExtras = MutableCreationExtras().apply {
        val application = context.applicationContext?.takeIf { it is Application }
        if (application != null) {
            set(
                ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY,
                application as Application
            )
        }
        set(SAVED_STATE_REGISTRY_OWNER_KEY, this@ComposeFloatingWindow)
        set(VIEW_MODEL_STORE_OWNER_KEY, this@ComposeFloatingWindow)
    }

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
    val display = DisplayHelper(context, windowManager)
    private var composeView: ComposeView? = null // Hold a direct reference
    private var parentComposition: Recomposer? = null // Hold reference for disposal

    /**
     * The maximum X coordinate for the floating window.
     */
    val maxXCoordinate
        get() = display.metrics.widthPixels - decorView.measuredWidth

    /**
     * The maximum Y coordinate for the floating window.
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
     * @throws IllegalStateException if called after [destroy] or [close] has been invoked.
     */
    fun setContent(content: @Composable () -> Unit) {
        checkDestroyed()
        Log.d(TAG, "Setting content.")

        disposeCompositionIfNeeded()

        val currentComposeView = ComposeView(context).apply {

            setViewTreeLifecycleOwner(this@ComposeFloatingWindow)
            setViewTreeViewModelStoreOwner(this@ComposeFloatingWindow)
            setViewTreeSavedStateRegistryOwner(this@ComposeFloatingWindow)

            // Create a Recomposer tied to the window's lifecycle scope
            val recomposer = Recomposer(coroutineContext)
            compositionContext = recomposer
            parentComposition = recomposer // Store for later disposal

            // Launch the Recomposer
            lifecycleCoroutineScope.launch {
                try {
                    recomposer.runRecomposeAndApplyChanges()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    Log.d(TAG, "Coroutine scope cancelled normally: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Recomposer error", e)
                } finally {
                    Log.d(TAG, "Recomposer job finished.")
                }
            }

            // Set the actual Composable content
            setContent {
                CompositionLocalProvider(
                    LocalFloatingWindow provides this@ComposeFloatingWindow
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
            Log.w(TAG, "Overlay permission (SYSTEM_ALERT_WINDOW) not granted. Cannot show window.")
            return
        }

        if (_isShowing.value) {
            Log.d(TAG, "Window already showing, updating layout.")
            update() // Ensure layout is up-to-date if show is called again
            return
        }

        Log.d(TAG, "Showing window.")

        try {
            // Ensure the view doesn't have a parent before adding
            if (decorView.parent != null) {
                Log.w(TAG, "DecorView already has a parent. Removing it.")
                (decorView.parent as? ViewGroup)?.removeView(decorView)
            }
            windowManager.addView(decorView, windowParams)
            // Move lifecycle to STARTED only after view is successfully added
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            // Update state last
            _isShowing.update { true }
        } catch (e: Exception) {
            // Catch potential exceptions from WindowManager (e.g., security, bad token)
            Log.e(TAG, "Error showing window: ${e.localizedMessage}", e)
            // Reset state if adding failed
            _isShowing.update { false }
        }
    }

    /**
     * Updates the layout of the floating window using the current [windowParams].
     * Call this after modifying [windowParams] (e.g., position or size) while the window is showing.
     *
     * @throws IllegalStateException if the window is already destroyed ([isDestroyed] is true).
     */
    fun update() {
        checkDestroyed()

        if (!_isShowing.value) {
            Log.w(TAG, "Update called but window is not showing.")
            return
        }
        Log.d(TAG, "Updating window layout.")
        try {
            windowManager.updateViewLayout(decorView, windowParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating window layout: ${e.localizedMessage}", e)
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
            Log.d(TAG, "Hide called but window is already hidden.")
            return
        }
        Log.d(TAG, "Hiding window.")

        _isShowing.update { false }
        try {
            // Check if view is still attached before removing
            if (decorView.parent != null) {
                windowManager.removeViewImmediate(decorView) // Use immediate for synchronous removal
            } else {
                Log.w(TAG, "Hide called but DecorView has no parent.")
            }
        } catch (e: Exception) {
            // Catch potential exceptions (e.g., view not attached)
            Log.e(TAG, "Error hiding window: ${e.localizedMessage}", e)
        } finally {
            // Move lifecycle to STOPPED regardless of removal success,
            // as the intention is to stop interaction.
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    fun isAvailable(): Boolean = Settings.canDrawOverlays(context)

    init {
        // Restore state early in the lifecycle
        savedStateRegistryController.performRestore(null)
        // Mark the lifecycle as CREATED
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        // Enable SavedStateHandles for ViewModels
        enableSavedStateHandles()
        Log.d(TAG, "ComposeFloatingWindow initialized.")
    }

    /** Throws an [IllegalStateException] if the window has been destroyed. */
    private fun checkDestroyed() {
        check(!_isDestroyed.value) {
            "ComposeFloatingWindow has been destroyed and cannot be used."
        }
    }

    /** Disposes the Compose composition and clears the reference. */
    private fun disposeCompositionIfNeeded() {
        composeView?.let {
            Log.d(TAG, "Disposing composition.")
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
     * **Once destroyed, this instance cannot be reused.** Create a new `ComposeFloatingWindow`
     * instance if you need to show a floating window again.
     */
    override fun close() {
        if (_isDestroyed.value) {
            Log.w(TAG, "Destroy called but window is already destroyed.")
            return
        }
        Log.d(TAG, "Destroying window...")

        // Hide the window if showing (ensures view is removed from WindowManager)
        if (_isShowing.value) {
            hide()
        }

        // Mark as destroyed immediately to prevent race conditions
        _isDestroyed.update { true }

        // Dispose the composition
        disposeCompositionIfNeeded()

        // Cancel the custom lifecycle scope and its children (including the Recomposer's job)
        Log.d(TAG, "Cancelling lifecycle coroutine scope.")
        try {// Explicit cancellation
            lifecycleCoroutineScope.cancel(
                kotlinx.coroutines.CancellationException("ComposeFloatingWindow destroyed")
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "Coroutine scope cancelled normally: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Recomposer error", e)
        }

        // Move lifecycle to DESTROYED
        Log.d(TAG, "Setting lifecycle state to DESTROYED.")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        // Clear the ViewModelStore
        Log.d(TAG, "Clearing ViewModelStore.")
        viewModelStore.clear()

        // Clean up references
        // decorView is managed by GC once this instance is gone.
        // composeView reference is cleared in disposeCompositionIfNeeded
        // windowManager is a system service, no need to clear.
        // savedStateRegistryController is tied to the lifecycle/owner, should be handled.

        Log.d(TAG, "ComposeFloatingWindow destroyed successfully.")
    }
}