package com.github.only52607.compose.window

import android.graphics.Rect
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun Modifier.dragFloatingWindow(
    onDrag: ((Int, Int) -> Unit)? = null,
): Modifier {
    val floatingWindow = LocalFloatingWindow.current
    val windowParams = remember { floatingWindow.windowParams }
    val dragModifier = Modifier
        .pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                val w = floatingWindow.decorView.width
                val h = floatingWindow.decorView.height
                val f = Rect().also { floatingWindow.decorView.getWindowVisibleDisplayFrame(it) }
                val left = (windowParams.x + dragAmount.x.toInt()).coerceIn(0..(f.width() - w))
                val top = (windowParams.y + dragAmount.y.toInt()).coerceIn(0..(f.height() - h))

                windowParams.x = left
                windowParams.y = top

                onDrag?.invoke(left, top)

                floatingWindow.update()
            }
        }

    return this then dragModifier
}