@file:Suppress("unused")

package com.github.only52607.compose.service

import android.content.Context
import android.util.Log
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isNotEmpty
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.github.only52607.compose.core.CoreFloatingWindow
import com.github.only52607.compose.core.defaultLayoutParams

/**
 * Hosts Jetpack Compose content in a floating window created from a service or application context.
 *
 * The default [windowParams] create a hardware-accelerated application-overlay window. This class
 * supplies Compose with this window's lifecycle, [androidx.lifecycle.ViewModelStore], saved-state
 * registry, and [LocalServiceFloatingWindow]. The composition and its window-owned recomposer are
 * retained across [hide][CoreFloatingWindow.hide] and [show][CoreFloatingWindow.show], while
 * frame-driven effects pause when the window is hidden or the screen is off.
 *
 * Call [close][CoreFloatingWindow.close] when the service no longer needs the window. Closing
 * detaches the view, disposes the composition, destroys scoped ViewModels, and releases registered
 * receivers and observers. The class implements [AutoCloseable] for callers with a naturally
 * bounded lifetime.
 *
 * Example usage with `use`:
 * ```kotlin
 * val floatingWindow = ComposeServiceFloatingWindow(context)
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
 * @param context Context used to create the Compose view and access [WindowManager]. Prefer a
 * service or application context whose lifetime covers the window.
 * @param windowParams Layout parameters used to attach and update the window. The default requests
 * a hardware-accelerated, non-focusable application overlay with wrap-content dimensions.
 */
public class ComposeServiceFloatingWindow(
    private val context: Context,
    override val windowParams: WindowManager.LayoutParams = defaultLayoutParams(context),
) : CoreFloatingWindow(
    context = context,
    tag = SERVICE_TAG,
) {
    /**
     * Sets or replaces the Compose content owned by this window.
     *
     * The new [ComposeView] receives this window's lifecycle, ViewModel store, saved-state registry,
     * and [LocalServiceFloatingWindow]. Its recomposer survives normal WindowManager detach/reattach
     * cycles and follows the window lifecycle for frame-clock pausing. Replacing existing content
     * disposes the previous composition and recomposer. If the window is already showing, its
     * layout is updated after replacement.
     *
     * @param content The composable function defining the UI of the floating window.
     * @throws IllegalStateException if called after [close][CoreFloatingWindow.close].
     */
    public fun setContent(content: @Composable () -> Unit) {
        checkDestroyed()
        Log.d(SERVICE_TAG, "Setting content.")

        disposeCompositionIfNeeded()

        val currentComposeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@ComposeServiceFloatingWindow)
            setViewTreeViewModelStoreOwner(this@ComposeServiceFloatingWindow)
            setViewTreeSavedStateRegistryOwner(this@ComposeServiceFloatingWindow)
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(lifecycle),
            )

            // Keep the recomposer alive while WindowManager detaches the view during hide().
            val recomposer = this@ComposeServiceFloatingWindow.createWindowRecomposer()
            setParentCompositionContext(recomposer)
            parentComposition = recomposer // Store for later disposal

            // Set the actual Composable content
            setContent {
                CompositionLocalProvider(
                    LocalServiceFloatingWindow provides this@ComposeServiceFloatingWindow,
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

private const val SERVICE_TAG = "ComposeServiceFloatingWindow"
