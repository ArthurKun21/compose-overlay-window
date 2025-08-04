package com.github.only52607.compose.service

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt

/**
 * Adds drag functionality to make a floating window draggable.
 *
 * This modifier enables the user to drag the floating window around the screen by
 * applying touch gestures to the composable it's attached to. The window position
 * is automatically constrained to stay within screen bounds.
 *
 * Example usage:
 * ```kotlin
 * FloatingActionButton(
 *     modifier = Modifier.dragServiceFloatingWindow(),
 *     onClick = { /* handle click */ }
 * ) {
 *     Icon(Icons.Filled.Call, "Call")
 * }
 * ```
 *
 * @param onDragStart Callback invoked when drag gesture starts. Receives the initial touch offset.
 * @param onDragEnd Callback invoked when drag gesture ends normally.
 * @param onDragCancel Callback invoked when drag gesture is cancelled.
 * @param onDrag Optional callback invoked during drag with the current window coordinates (left, top).
 * @return A [Modifier] that enables drag functionality for the floating window.
 */
@Composable
fun Modifier.dragServiceFloatingWindow(
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onDrag: ((Int, Int) -> Unit)? = null,
): Modifier {
    val floatingWindow = LocalServiceFloatingWindow.current

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
            }
        }
}