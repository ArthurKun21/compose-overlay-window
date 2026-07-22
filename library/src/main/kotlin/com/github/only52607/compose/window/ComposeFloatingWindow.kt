package com.github.only52607.compose.window

import android.app.Application
import android.content.Context
import android.util.Log
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isNotEmpty
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.github.only52607.compose.core.CoreFloatingWindow
import com.github.only52607.compose.core.defaultLayoutParams

/**
 * Hosts Jetpack Compose content in a lifecycle-aware floating overlay window.
 *
 * With the recommended application context, the default [windowParams] create a
 * hardware-accelerated application-overlay window. This class supplies Compose with this window's
 * lifecycle, ViewModel store, saved-state registry, default [ViewModelProvider.Factory], creation
 * extras for `SavedStateHandle` and `AndroidViewModel`, and [LocalFloatingWindow]. The composition
 * and its window-owned recomposer are retained across [hide][CoreFloatingWindow.hide] and
 * [show][CoreFloatingWindow.show], while frame-driven effects pause when the window is hidden or
 * the screen is off.
 *
 * Call [close][CoreFloatingWindow.close] when the window is no longer needed. Closing detaches the
 * view, disposes the composition, destroys scoped ViewModels, and releases registered receivers
 * and observers. The class implements [AutoCloseable] for callers with a naturally bounded
 * lifetime.
 *
 * Example usage with `use`:
 * ```kotlin
 * val floatingWindow = ComposeFloatingWindow(context)
 * floatingWindow.use { window -> // close() is called automatically at the end of this block
 *     window.setContent { /* Your Composable UI */ }
 *     window.show()
 *     // ... interact with the window ...
 * } // Window is hidden and resources are released here
 * ```
 *
 * Remember to declare the `SYSTEM_ALERT_WINDOW` permission in your AndroidManifest.xml and
 * request it at runtime if targeting Android M (API 23) or higher.
 *
 * @param context Context used to create the Compose view and access [WindowManager]. Prefer an
 * application context whose lifetime covers the window.
 * @param windowParams Layout parameters used to attach and update the window. The default requests
 * a hardware-accelerated, non-focusable application overlay with wrap-content dimensions.
 */
public class ComposeFloatingWindow(
    private val context: Context,
    override val windowParams: WindowManager.LayoutParams = defaultLayoutParams(context),
) : CoreFloatingWindow(
    context = context,
    tag = TAG,
),
    HasDefaultViewModelProviderFactory {

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        SavedStateViewModelFactory(
            context.applicationContext as Application,
            this@ComposeFloatingWindow,
            null,
        )
    }

    override val defaultViewModelCreationExtras: CreationExtras = MutableCreationExtras().apply {
        val application = context.applicationContext as? Application
        if (application != null) {
            set(
                ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY,
                application,
            )
        }
        set(SAVED_STATE_REGISTRY_OWNER_KEY, this@ComposeFloatingWindow)
        set(VIEW_MODEL_STORE_OWNER_KEY, this@ComposeFloatingWindow)
    }

    /**
     * Sets or replaces the Compose content owned by this window.
     *
     * The new [ComposeView] receives this window's lifecycle, ViewModel store, saved-state registry,
     * default ViewModel factory and creation extras, and [LocalFloatingWindow]. Its recomposer
     * survives normal WindowManager detach/reattach cycles and follows the window lifecycle for
     * frame-clock pausing. Replacing existing content disposes the previous composition and
     * recomposer. If the window is already showing, its layout is updated after replacement.
     *
     * @param content The composable function defining the UI of the floating window.
     * @throws IllegalStateException if called after [close][CoreFloatingWindow.close].
     */
    public fun setContent(content: @Composable () -> Unit) {
        checkDestroyed()
        Log.d(TAG, "Setting content.")

        disposeCompositionIfNeeded()

        val currentComposeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@ComposeFloatingWindow)
            setViewTreeViewModelStoreOwner(this@ComposeFloatingWindow)
            setViewTreeSavedStateRegistryOwner(this@ComposeFloatingWindow)
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycle),
            )

            // Keep the recomposer alive while WindowManager detaches the view during hide().
            val recomposer = this@ComposeFloatingWindow.createWindowRecomposer()
            setParentCompositionContext(recomposer)
            parentComposition = recomposer // Store for later disposal

            // Set the actual Composable content
            setContent {
                CompositionLocalProvider(
                    LocalFloatingWindow provides this@ComposeFloatingWindow,
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
        if (isShowing.value) {
            update()
        }
    }
}

private const val TAG = "ComposeFloatingWindow"
