package com.github.only52607.compose.window

import android.graphics.Rect
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

@Composable
fun Modifier.dragFloatingWindow(
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onDrag: ((Int, Int) -> Unit)? = null,
): Modifier {
    val floatingWindow = LocalFloatingWindow.current
    val windowParams = remember { floatingWindow.windowParams }

    var lastUpdateTime = 0.milliseconds
    var targetTime = TimeSource.Monotonic.markNow()
    val interval = 32.milliseconds

    val dragModifier = Modifier
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    lastUpdateTime = 0.milliseconds
                    targetTime = TimeSource.Monotonic.markNow() + interval
                    onDragStart.invoke(offset)
                },
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
            ) { change, dragAmount ->
                change.consume()
                val w = floatingWindow.decorView.width
                val h = floatingWindow.decorView.height
                val f = Rect().also { floatingWindow.decorView.getWindowVisibleDisplayFrame(it) }
                val left = (windowParams.x + dragAmount.x.toInt()).coerceIn(0..(f.width() - w))
                val top = (windowParams.y + dragAmount.y.toInt()).coerceIn(0..(f.height() - h))

                windowParams.x = left
                windowParams.y = top

                if (targetTime.hasPassedNow()) {
                    onDrag?.invoke(left, top)
                    floatingWindow.update()
                    targetTime = TimeSource.Monotonic.markNow() + interval
                }
            }
        }

    return this then dragModifier
}