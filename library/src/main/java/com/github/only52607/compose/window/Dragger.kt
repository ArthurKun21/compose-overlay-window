package com.github.only52607.compose.window

import android.util.Log
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

@Composable
fun Modifier.dragFloatingWindow(
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onDrag: ((Int, Int) -> Unit)? = null,
): Modifier {
    val floatingWindow = LocalFloatingWindow.current
    val windowParams = remember { floatingWindow.windowParams }

    return this
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
            ) { change, dragAmount ->
                change.consume()

                val targetX = floatingWindow.windowParams.x + dragAmount.x.roundToInt()
                val targetY = floatingWindow.windowParams.y + dragAmount.y.roundToInt()

                val left = targetX.coerceIn(0, floatingWindow.maxXCoordinate)
                val top = targetY.coerceIn(0, floatingWindow.maxYCoordinate)

                floatingWindow.updateCoordinate(left, top)

                onDrag?.invoke(left, top)

                try {
                    floatingWindow.update()
                } catch (e: Exception) {
                    // Log but don't crash on update failures during drag
                    Log.w(TAG, "Failed to update window position: ${e.message}")
                }
            }
        }
}