package com.github.only52607.compose.core

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Base owner for a Compose hierarchy hosted directly by [WindowManager].
 *
 * A window starts in [Lifecycle.State.CREATED]. [show] attaches [decorView], sets [isShowing] to
 * `true`, and advances the lifecycle to [Lifecycle.State.RESUMED] while the screen is interactive.
 * [hide] immediately sets [isShowing] to `false` and returns the lifecycle to
 * [Lifecycle.State.CREATED], then detaches the view after its fade-out animation. Turning the
 * screen off applies the same lifecycle transition to a shown window; turning it on resumes that
 * window.
 *
 * Hiding or temporarily detaching the view does not dispose its composition. The window owns its
 * recomposer across detach/reattach cycles, pauses frame-driven effects while stopped, and honors
 * the system animator-duration scale. Call [close] when the window is no longer needed to detach
 * it immediately, dispose its composition, destroy its lifecycle and ViewModels, and release
 * registered observers and receivers. A closed instance cannot be shown again.
 */
public abstract class CoreFloatingWindow internal constructor(
    private val context: Context,
    private val tag: String = "CoreFloatingWindow",
) : SavedStateRegistryOwner,
    ViewModelStoreOwner,
    AutoCloseable {

    private val applicationContext = context.applicationContext

    /**
     * Layout parameters used when attaching or updating this window.
     *
     * Changes made while the window is visible take effect after calling [update]. Changes made
     * while hidden are applied by the next [show].
     */
    public abstract val windowParams: WindowManager.LayoutParams

    // --- Lifecycle, ViewModel, SavedState ---

    // Use a SupervisorJob so failure of one child doesn't cause others to fail
    // Use a custom scope tied to the window's lifecycle for managing window-specific coroutines
    internal val coroutineContext = AndroidUiDispatcher.Main
    internal val lifecycleCoroutineScope = CoroutineScope(
        SupervisorJob() + coroutineContext,
    )
    private val mainHandler = Handler(Looper.getMainLooper())
    private var coordinateUpdateScheduled = false

    public override val viewModelStore: ViewModelStore = ViewModelStore()

    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    public override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private val savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)
    public override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // --- Window State ---

    private val _isShowing = MutableStateFlow(false)

    /**
     * Whether this window is logically shown.
     *
     * The value becomes `true` when [show] successfully attaches or reuses the window and becomes
     * `false` as soon as [hide] starts. During the fade-out animation, the value is already `false`
     * even though [decorView] may remain attached briefly. This state is independent of screen
     * interactivity; a shown window remains `true` while its lifecycle is stopped for screen-off.
     *
     * This does not indicate whether the instance has been closed. Check [isDestroyed] for that.
     */
    public val isShowing: StateFlow<Boolean>
        get() = _isShowing.asStateFlow()

    private val _isDestroyed = MutableStateFlow(false)

    /**
     * Whether [close] has permanently destroyed this window.
     *
     * Once this becomes `true`, the instance cannot be reused. Create a new window instead.
     */
    public val isDestroyed: StateFlow<Boolean>
        get() = _isDestroyed.asStateFlow()

    /**
     * The root view container for the floating window's content.
     *
     * This view is attached directly to [WindowManager] by [show] and detached by [hide] or
     * [close]. Its Compose content and recomposer survive a normal [hide]/[show] cycle.
     */
    public var decorView: ViewGroup = FloatingWindowDecorView(
        context = context,
        onWindowSizeChanged = ::onDecorViewSizeChanged,
    )
        .apply {
            // Important: Prevent clipping so shadows or elements outside bounds can be drawn
            clipChildren = false
            clipToPadding = false
        }
        private set

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // --- Screen state ---

    // The floating window lifecycle must mirror what an Activity gets for free: when the screen
    // turns off, the UI is no longer visible, so lifecycle-aware work (state collection,
    // animations) must stop. Otherwise the window keeps composing/measuring into an invisible
    // surface while the screen is off, and the accumulated work bursts onto the first frames
    // after the screen turns back on, which is felt as lag.
    private val powerManager =
        applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val isScreenInteractive: Boolean
        get() = powerManager?.isInteractive ?: true

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_SCREEN_OFF,
                -> Unit

                else -> return
            }
            Log.d(tag, "Screen interactive: $isScreenInteractive")
            syncLifecycleWithScreenState()
        }
    }

    /**
     * Moves the lifecycle to RESUMED only while the window is both showing and the screen is
     * interactive; otherwise drops it to CREATED (delivering ON_PAUSE/ON_STOP).
     */
    private fun syncLifecycleWithScreenState() {
        if (_isDestroyed.value) {
            return
        }
        if (_isShowing.value && isScreenInteractive) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        } else if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    /**
     * Helper class providing access to the display metrics for the floating window.
     *
     * Used internally to calculate maximum coordinates and display dimensions
     * for proper window positioning and bounds checking.
     */
    public val display: DisplayHelper = DisplayHelper(context, windowManager)
    internal var composeView: ComposeView? = null // Hold a direct reference
    internal var parentComposition: Recomposer? = null // Hold reference for disposal

    /**
     * The maximum X coordinate for the floating window.
     *
     * This represents the rightmost position where the window can be placed
     * while still remaining fully visible on screen. Calculated as the
     * screen width minus the window's measured width.
     *
     * @return The maximum X coordinate in pixels, or 0 if the window hasn't been measured yet.
     */
    public val maxXCoordinate: Int
        get() = (display.metrics.widthPixels - decorView.measuredWidth).coerceAtLeast(0)

    /**
     * The maximum Y coordinate for the floating window.
     *
     * This represents the bottommost position where the window can be placed
     * while still remaining fully visible on screen. Calculated as the
     * screen height minus the window's measured height.
     *
     * @return The maximum Y coordinate in pixels, or 0 if the window hasn't been measured yet.
     */
    public val maxYCoordinate: Int
        get() = (display.metrics.heightPixels - decorView.measuredHeight).coerceAtLeast(0)

    /**
     * Shows the floating window with a fade-in animation.
     *
     * Attaches [decorView] to [WindowManager] with [windowParams], or reuses it if [hide]'s
     * fade-out is still in progress. When the screen is interactive, the lifecycle advances to
     * [Lifecycle.State.RESUMED]; otherwise it remains [Lifecycle.State.CREATED] until screen-on.
     * Calling this while already shown reapplies [windowParams].
     *
     * This operation requires the `SYSTEM_ALERT_WINDOW` permission. If the permission is absent,
     * the call logs a warning and leaves the window hidden.
     *
     * @throws IllegalStateException if the window is already destroyed ([isDestroyed] is true).
     * @throws IllegalArgumentException if Compose content has not been provided by a subclass.
     */
    public fun show() {
        checkDestroyed()

        require(composeView != null) {
            "Content must be set using setContent() before showing the window."
        }

        if (!isAvailable()) {
            Log.w(
                tag,
                "Overlay permission (SYSTEM_ALERT_WINDOW) not granted. Cannot show window.",
            )
            return
        }

        if (_isShowing.value) {
            Log.d(tag, "Window already showing, updating layout.")
            update() // Ensure layout is up-to-date if show is called again
            return
        }

        Log.d(tag, "Showing window.")

        try {
            val parent = decorView.parent
            val isAttachedToWindowManager = parent != null && parent !is ViewGroup
            if (parent is ViewGroup) {
                Log.w(tag, "DecorView already has a ViewGroup parent. Removing it.")
                parent.removeView(decorView)
            }

            if (isAttachedToWindowManager) {
                // hide() is still fading out. Mark the window as showing before cancelling the
                // animator so its removal callback cannot detach the view we are reusing.
                _isShowing.update { true }
                decorView.animate().cancel()
                updateImmediately()
            } else {
                // A frame callback posted before the previous detach may have been discarded.
                coordinateUpdateScheduled = false
                decorView.alpha = INVISIBLE_ALPHA
                windowParams.disableSystemMoveAnimations()
                windowManager.addView(decorView, windowParams)
                _isShowing.update { true }
            }

            // Move lifecycle to RESUMED as soon as the view is attached (delivers ON_START and
            // ON_RESUME in order). A shown floating window is the overlay equivalent of a resumed
            // Activity: lifecycle-aware collection such as collectAsStateWithLifecycle or
            // repeatOnLifecycle gated on STARTED or RESUMED must be active while it is visible.
            // If the screen is currently off, this stays STOPPED until ACTION_SCREEN_ON.
            syncLifecycleWithScreenState()
            // Animate fade-in
            decorView.animate()
                .alpha(VISIBLE_ALPHA)
                .setDuration(ANIMATION_DURATION)
                .start()
        } catch (e: Exception) {
            // Catch potential exceptions from WindowManager (e.g., security, bad token)
            Log.e(tag, "Error showing window: ${e.message}", e)
            // Reset state if adding failed
            _isShowing.update { false }
        }
    }

    /**
     * Updates the window coordinates to the specified position.
     *
     * Stores the coordinates in [windowParams] and coalesces visible-window updates onto the next
     * animation frame. If the window is hidden, the stored coordinates are used by the next
     * [show].
     *
     * @param left The new X coordinate (left position) for the window.
     * @param top The new Y coordinate (top position) for the window.
     */
    public fun updateCoordinate(left: Int, top: Int) {
        if (windowParams.x == left && windowParams.y == top) {
            return
        }

        windowParams.x = left
        windowParams.y = top

        try {
            scheduleCoordinateUpdate()
        } catch (e: Exception) {
            // Log but don't crash on update failures during drag
            Log.w(tag, "Failed to update window position: ${e.message}")
        }
    }

    private fun scheduleCoordinateUpdate() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            scheduleCoordinateUpdateOnMain()
        } else {
            mainHandler.post {
                scheduleCoordinateUpdateOnMain()
            }
        }
    }

    private fun scheduleCoordinateUpdateOnMain() {
        if (coordinateUpdateScheduled) {
            return
        }

        coordinateUpdateScheduled = true
        decorView.postOnAnimation {
            coordinateUpdateScheduled = false
            if (_isDestroyed.value) {
                return@postOnAnimation
            }
            updateImmediately()
        }
    }

    private fun onDecorViewSizeChanged() {
        if (!_isShowing.value || _isDestroyed.value) {
            return
        }

        // Some Android versions relayout WRAP_CONTENT overlay windows by moving the surface
        // when the content size changes. Re-apply the stored top-start coordinates after the
        // new size is known so expanding/collapsing Compose content does not jump on screen.
        // Coordinates for other gravities are offsets with different semantics and must not be
        // clamped as if they were absolute top-start positions.
        if (windowParams.gravity == (Gravity.START or Gravity.TOP)) {
            windowParams.x = windowParams.x.coerceIn(0, maxXCoordinate)
            windowParams.y = windowParams.y.coerceIn(0, maxYCoordinate)
        }

        try {
            scheduleCoordinateUpdate()
        } catch (e: Exception) {
            Log.w(tag, "Failed to update window after size change: ${e.message}")
        }
    }

    /**
     * Updates the layout of the floating window using the current [windowParams].
     *
     * Call this after modifying [windowParams] (for example, its position or size) while the
     * window is showing. Calls made while hidden do not attach or update the window; the parameters
     * are applied by the next [show]. Calls from a background thread are posted to the main thread.
     *
     * @throws IllegalStateException if the window is already destroyed ([isDestroyed] is true).
     */
    public fun update() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            updateImmediately()
        } else {
            checkDestroyed()
            mainHandler.post {
                if (_isDestroyed.value) {
                    return@post
                }
                updateImmediately()
            }
        }
    }

    private fun updateImmediately() {
        checkDestroyed()

        if (!_isShowing.value) {
            Log.w(tag, "Update called but window is not showing.")
            return
        }
        Log.d(tag, "Updating window layout.")
        try {
            windowParams.disableSystemMoveAnimations()
            windowManager.updateViewLayout(decorView, windowParams)
        } catch (e: Exception) {
            Log.e(tag, "Error updating window layout: ${e.message}", e)
        }
    }

    /**
     * Hides the floating window with fade-out animation.
     *
     * Sets [isShowing] to `false` immediately, dispatches lifecycle pause/stop events so the
     * lifecycle returns to [Lifecycle.State.CREATED], and removes [decorView] from [WindowManager]
     * when the animation finishes. The composition is retained and can be resumed by [show]. If
     * [show] is called before the fade-out finishes, removal is cancelled and the existing view is
     * reused.
     *
     * @throws IllegalStateException if the window is already destroyed ([isDestroyed] is true).
     */
    public fun hide() {
        checkDestroyed()

        if (!_isShowing.value) {
            Log.d(tag, "Hide called but window is already hidden.")
            return
        }
        Log.d(tag, "Hiding window.")

        _isShowing.update { false }
        try {
            // Check if view is still attached before animating removal
            if (decorView.parent != null) {
                // Animate fade-out
                decorView.animate()
                    .alpha(INVISIBLE_ALPHA)
                    .setDuration(ANIMATION_DURATION)
                    .withEndAction {
                        if (_isShowing.value) {
                            return@withEndAction
                        }

                        // Remove view after animation
                        try {
                            if (decorView.parent != null) {
                                windowManager.removeViewImmediate(decorView)
                            }
                            coordinateUpdateScheduled = false
                        } catch (e: Exception) {
                            Log.e(tag, "Error removing window: ${e.message}", e)
                        }
                    }
                    .start()
            } else {
                Log.w(tag, "Hide called but DecorView has no parent.")
            }
        } catch (e: Exception) {
            // Catch potential exceptions (e.g., view not attached)
            Log.e(tag, "Error hiding window: ${e.message}", e)
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
    public fun isAvailable(): Boolean = Settings.canDrawOverlays(context)

    init {
        // Warn if non-application context is used to prevent memory leaks
        if (context !is Application && context.applicationContext != context) {
            Log.w(
                tag,
                "Consider using applicationContext " +
                    "instead of activity context to prevent memory leaks",
            )
        }
        // Restore state early in the lifecycle
        savedStateRegistryController.performRestore(null)
        // Mark the lifecycle as CREATED
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        // Enable SavedStateHandles for ViewModels
        enableSavedStateHandles()
        // Follow the display's interactive state so lifecycle-aware work pauses while the
        // screen is off. ACTION_SCREEN_ON/OFF are protected system broadcasts, so the receiver
        // does not need to be exported.
        ContextCompat.registerReceiver(
            applicationContext,
            screenStateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        Log.d(tag, "FloatingWindow initialized.")
    }

    /** Throws an [IllegalStateException] if the window has been destroyed. */
    internal fun checkDestroyed() {
        check(!_isDestroyed.value) {
            "FloatingWindow has been destroyed and cannot be used."
        }
    }

    /** Creates a recomposer owned by this window rather than by a single view attachment. */
    internal fun createWindowRecomposer(): Recomposer {
        val motionDurationScale = SystemMotionDurationScale(applicationContext)
        val recomposer = Recomposer(coroutineContext + motionDurationScale)

        // Mirror androidx's lifecycle-aware window recomposer: keep the composition frame clock
        // (which drives withFrameNanos-based animations) paused while the window is not STARTED,
        // i.e. while it is hidden or the screen is off. Recomposition from state changes still
        // runs through the parent frame clock, so content stays current across hide()/show().
        recomposer.pauseCompositionFrameClock()
        val frameClockObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> recomposer.resumeCompositionFrameClock()
                Lifecycle.Event.ON_STOP -> recomposer.pauseCompositionFrameClock()
                else -> Unit
            }
        }
        lifecycle.addObserver(frameClockObserver)

        // WindowManager detaches the decor view on every hide(). A lifecycle-aware window
        // recomposer treats that detach as permanent destruction, so own the runner here and
        // cancel it only when content is replaced or this floating window is closed.
        lifecycleCoroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                recomposer.runRecomposeAndApplyChanges()
            } finally {
                lifecycle.removeObserver(frameClockObserver)
                motionDurationScale.close()
            }
        }

        return recomposer
    }

    /** Disposes the Compose composition and clears the reference. */
    internal fun disposeCompositionIfNeeded() {
        composeView?.let {
            Log.d(tag, "Disposing composition.")
            it.disposeComposition() // Dispose the underlying composition
            parentComposition?.cancel() // Cancel the recomposer explicitly if needed
            parentComposition = null
            decorView.removeView(it) // Remove view from hierarchy
            composeView = null // Clear the reference
        }
    }

    /**
     * Implementation of [AutoCloseable].
     * Permanently destroys the floating window and releases its resources.
     *
     * This performs the following actions:
     * 1. Unregisters the screen-state receiver.
     * 2. Cancels any fade animation and detaches [decorView] immediately.
     * 3. Disposes the Compose composition and its window-owned recomposer.
     * 4. Cancels window-owned coroutines and animation-scale observation.
     * 5. Moves the lifecycle to [Lifecycle.State.DESTROYED].
     * 6. Clears the [ViewModelStore], destroying associated ViewModels.
     *
     * Repeated calls are ignored. Once destroyed, this instance cannot be reused.
     */
    public override fun close() {
        if (_isDestroyed.value) {
            Log.w(tag, "Destroy called but window is already destroyed.")
            return
        }
        Log.d(tag, "Destroying window...")

        try {
            applicationContext.unregisterReceiver(screenStateReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(tag, "Screen state receiver was not registered: ${e.message}")
        }

        // Remove the view even when hide() has already marked it hidden but its fade-out has not
        // finished yet.
        val wasShowing = _isShowing.value
        _isShowing.update { false }
        try {
            decorView.animate().cancel()

            if (decorView.parent != null) {
                windowManager.removeViewImmediate(decorView)
            } else if (wasShowing) {
                Log.w(tag, "Destroy called but DecorView has no parent.")
            }
        } catch (e: Exception) {
            Log.e(
                tag,
                "Error hiding window during destruction: ${e.message}",
                e,
            )
        } finally {
            coordinateUpdateScheduled = false
            if (wasShowing) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
        }

        // Mark as destroyed immediately to prevent race conditions
        _isDestroyed.update { true }

        // Dispose the composition
        disposeCompositionIfNeeded()

        // Cancel the custom lifecycle scope and its children (including the Recomposer's job)
        Log.d(tag, "Cancelling lifecycle coroutine scope.")
        try { // Explicit cancellation
            lifecycleCoroutineScope.cancel(
                CancellationException("FloatingWindow destroyed"),
            )
        } catch (e: CancellationException) {
            Log.d(tag, "Coroutine scope cancelled normally: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "Recomposer error", e)
        }

        // Move lifecycle to DESTROYED
        Log.d(tag, "Setting lifecycle state to DESTROYED.")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        // Clear the ViewModelStore
        Log.d(tag, "Clearing ViewModelStore.")
        viewModelStore.clear()

        // Clean up references
        // decorView is managed by GC once this instance is gone.
        // composeView reference is cleared in disposeCompositionIfNeeded
        // windowManager is a system service, no need to clear.
        // savedStateRegistryController is tied to the lifecycle/owner, should be handled.

        Log.d(tag, "FloatingWindow destroyed successfully.")
    }

    private companion object {
        /**
         * Duration in milliseconds for fade in/out animations when showing/hiding the window.
         */
        private const val ANIMATION_DURATION = 300L

        /**
         * Alpha value representing fully invisible state.
         */
        private const val INVISIBLE_ALPHA = 0f

        /**
         * Alpha value representing fully visible state.
         */
        private const val VISIBLE_ALPHA = 1f
    }
}

private class FloatingWindowDecorView(
    context: Context,
    private val onWindowSizeChanged: () -> Unit,
) : FrameLayout(context) {
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)

        if (oldWidth == 0 && oldHeight == 0) {
            return
        }

        onWindowSizeChanged()
    }
}

private class SystemMotionDurationScale(context: Context) : MotionDurationScale, AutoCloseable {
    private val contentResolver = context.contentResolver
    private val animationScaleUri =
        Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)

    private val scaleFactorState = mutableFloatStateOf(readScaleFactor())
    override val scaleFactor: Float
        get() = scaleFactorState.floatValue

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            scaleFactorState.floatValue = readScaleFactor()
        }
    }

    init {
        contentResolver.registerContentObserver(animationScaleUri, false, observer)
    }

    private fun readScaleFactor(): Float = Settings.Global.getFloat(
        contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        DEFAULT_SCALE_FACTOR,
    )

    override fun close() {
        contentResolver.unregisterContentObserver(observer)
    }

    private companion object {
        const val DEFAULT_SCALE_FACTOR = 1f
    }
}
