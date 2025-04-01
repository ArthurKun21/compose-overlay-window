package com.github.only52607.compose.window

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ComposeFloatingWindow(
    private val context: Context,
    val windowParams: WindowManager.LayoutParams = defaultLayoutParams(context),
) : SavedStateRegistryOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    AutoCloseable {

    companion object {
        private const val TAG = "ComposeFloatingWindow"

        fun defaultLayoutParams(context: Context) = WindowManager.LayoutParams().apply {
            height = WindowManager.LayoutParams.WRAP_CONTENT
            width = WindowManager.LayoutParams.WRAP_CONTENT
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.START or Gravity.TOP
            windowAnimations = android.R.style.Animation_Dialog
            flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            if (context !is Activity) {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                }
            }
        }
    }

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

    fun setContent(content: @Composable () -> Unit) {
        checkDestroyed()

        setContentView(ComposeView(context).apply {
            setContent {
                CompositionLocalProvider(
                    LocalFloatingWindow provides this@ComposeFloatingWindow
                ) {
                    content()
                }
            }
            setViewTreeLifecycleOwner(this@ComposeFloatingWindow)
            setViewTreeViewModelStoreOwner(this@ComposeFloatingWindow)
            setViewTreeSavedStateRegistryOwner(this@ComposeFloatingWindow)
        })
    }

    private fun setContentView(view: View) {
        if (decorView.isNotEmpty()) {
            decorView.removeAllViews()
        }
        decorView.addView(view)
        update()
    }

    fun show() {
        checkDestroyed()

        if (isAvailable().not()) return
        require(decorView.isNotEmpty()) {
            "Content view cannot be empty"
        }
        if (_isShowing.value) {
            update()
            return
        }
        decorView.getChildAt(0)?.takeIf { it is ComposeView }?.let { composeView ->
            val reComposer = Recomposer(AndroidUiDispatcher.CurrentThread)
            composeView.compositionContext = reComposer
            lifecycleScope.launch(AndroidUiDispatcher.CurrentThread) {
                reComposer.runRecomposeAndApplyChanges()
            }
        }
        if (decorView.parent != null) {
            windowManager.removeViewImmediate(decorView)
        }
        windowManager.addView(decorView, windowParams)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        _isShowing.update { true }
    }

    fun update() {
        checkDestroyed()

        if (!_isShowing.value) {
            Log.w(TAG, "Update called but window is not showing.")
            return
        }
        Log.d(TAG, "Updating window layout.")
        try {
            windowManager.updateViewLayout(decorView, windowParams)
        } catch (e: Exception){
            Log.e(TAG, "Error updating window layout: ${e.localizedMessage}", e)
        }
    }

    fun hide() {
        checkDestroyed()

        if (!_isShowing.value) {
            Log.d(TAG, "Hide called but window is already hidden.")
            return
        }
        Log.d(TAG, "Hiding window.")

        _isShowing.update { false }
        windowManager.removeViewImmediate(decorView)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
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

    override fun close() {
        TODO("Not yet implemented")
    }
}