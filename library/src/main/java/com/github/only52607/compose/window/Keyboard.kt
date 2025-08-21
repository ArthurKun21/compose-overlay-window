package com.github.only52607.compose.window

import android.util.Log
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.launch

@Composable
fun rememberFloatingWindowInteractionSource(): MutableInteractionSource {
    val floatingWindow = LocalFloatingWindow.current

    val interactionSource = remember { MutableInteractionSource() }

    var focusIndication: FocusInteraction.Focus? by remember {
        mutableStateOf(null)
    }
    val isFocused by remember {
        derivedStateOf {
            focusIndication != null
        }
    }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            when (it) {
                is FocusInteraction.Focus -> {
                    focusIndication = it
                    Log.d("", "FocusInteraction.Focus: $it")
                }
            }
        }
    }

    val scope = rememberCoroutineScope()

    DisposableEffect(floatingWindow.decorView) {
        ViewCompat.setOnApplyWindowInsetsListener(floatingWindow.decorView) { v, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (!imeVisible && isFocused && floatingWindow.windowParams.flags and FLAG_NOT_FOCUSABLE == 0) {
                Log.d("", "IME closed, restoring FLAG_NOT_FOCUSABLE")
                floatingWindow.windowParams.flags =
                    FLAG_NOT_TOUCH_MODAL or FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_NO_LIMITS
                floatingWindow.update()
                focusIndication?.let {
                    Log.d("", "Unfocusing window: $it")
                    scope.launch {
                        focusManager.clearFocus()
                        focusIndication = null
                    }
                }
            }
            insets
        }
        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(floatingWindow.decorView, null)
        }
    }

    return interactionSource
}
