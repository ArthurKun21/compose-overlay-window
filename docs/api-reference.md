# API reference

This page summarizes the public API of the library module.

## Packages

Import the library classes from these packages:

| Package | Contents |
| --- | --- |
| `com.github.only52607.compose.window` | `ComposeFloatingWindow`, `LocalFloatingWindow`, `Modifier.dragFloatingWindow`, `rememberFloatingWindowInteractionSource` |
| `com.github.only52607.compose.service` | `ComposeServiceFloatingWindow`, `LocalServiceFloatingWindow`, `Modifier.dragServiceFloatingWindow`, `rememberServiceFloatingWindowInteractionSource` |
| `com.github.only52607.compose.core` | `CoreFloatingWindow` (base class), `checkOverlayPermission`, `requestOverlayPermission`, `DisplayHelper` |

The dialog APIs use androidx package names. Import these exact names. The IDE may suggest a different androidx class with a similar name:

- `androidx.compose.material3.SystemAlertDialog`
- `androidx.compose.ui.window.SystemDialog` and `SystemDialogProperties`

## Window types

### `ComposeFloatingWindow`

Creates a floating window backed by app code such as an activity.

```kotlin
val overlay = ComposeFloatingWindow(
    context = applicationContext,
)
```

Public members:

| API | Description |
| --- | --- |
| `setContent(content)` | Sets the Compose UI rendered inside the floating window. Call it before `show()`. |
| `show()` | Attaches the floating window to `WindowManager` when the app has the overlay permission. Throws `IllegalArgumentException` when content is not set. |
| `hide()` | Removes the view with a fade-out animation. The composition and its state stay intact. |
| `close()` | Destroys the instance. Disposes the composition, clears the `ViewModel` store, and removes the window. |
| `updateCoordinate(left, top)` | Moves the floating window to a specific coordinate pair. |
| `update()` | Re-applies the current `windowParams` while the floating window is showing. |
| `isAvailable()` | Returns `true` when the app has the `SYSTEM_ALERT_WINDOW` permission. |
| `windowParams` | Mutable `WindowManager.LayoutParams` for size, flags, gravity, and position. |
| `isShowing` | `StateFlow<Boolean>` that tracks whether the floating window is currently attached. |
| `isDestroyed` | `StateFlow<Boolean>` that becomes `true` after `close()`. |
| `display` | Helper for reading screen metrics. |
| `maxXCoordinate` | Largest x position that keeps the floating window on screen. |
| `maxYCoordinate` | Largest y position that keeps the floating window on screen. |

### `ComposeServiceFloatingWindow`

The service variant of `ComposeFloatingWindow`. The API is parallel to the activity variant. Create and own this window from a `Service`.

```kotlin
val overlay = ComposeServiceFloatingWindow(context = this)
```

This class does not provide a default `ViewModel` factory. Pass an explicit factory when you create `ViewModel` instances inside this window.

## Permission helpers

| API | Description |
| --- | --- |
| `checkOverlayPermission(context)` | Returns `true` when the app can draw overlays. |
| `requestOverlayPermission(context)` | Opens the system settings screen for the overlay permission. |

Typical usage:

```kotlin
if (checkOverlayPermission(this)) {
    overlay.show()
} else {
    requestOverlayPermission(this)
}
```

## Drag and window access helpers

### Composition locals

| API | Description |
| --- | --- |
| `LocalFloatingWindow` | Provides the current `ComposeFloatingWindow` inside `setContent { ... }`. |
| `LocalServiceFloatingWindow` | Provides the current `ComposeServiceFloatingWindow` inside service window content. |

### Drag modifiers

| API | Description |
| --- | --- |
| `Modifier.dragFloatingWindow(...)` | Makes content in a `ComposeFloatingWindow` draggable. |
| `Modifier.dragServiceFloatingWindow(...)` | Makes content in a `ComposeServiceFloatingWindow` draggable. |

Both drag modifiers accept optional callbacks:

| Parameter | Description |
| --- | --- |
| `onDragStart` | Called when dragging starts. |
| `onDragEnd` | Called when dragging ends normally. |
| `onDragCancel` | Called when the user cancels the drag. |
| `onDrag` | Reports the current `(left, top)` coordinates while dragging. |

### Keyboard interaction sources

| API | Description |
| --- | --- |
| `rememberFloatingWindowInteractionSource()` | Use with text inputs inside `ComposeFloatingWindow`. |
| `rememberServiceFloatingWindowInteractionSource()` | Use with text inputs inside `ComposeServiceFloatingWindow`. |

## Dialog APIs

### `SystemAlertDialog`

A Material 3 alert dialog that shows in its own system overlay window.

```kotlin
SystemAlertDialog(
    onDismissRequest = { open = false },
    confirmButton = {
        TextButton(onClick = { open = false }) {
            Text("OK")
        }
    },
    text = { Text("Hello from the overlay") },
)
```

Use this dialog when the standard Material alert layout is enough.

### `SystemDialog`

A dialog API for fully custom content. The content shows in its own system overlay window.

```kotlin
SystemDialog(
    onDismissRequest = { open = false },
    properties = SystemDialogProperties(
        usePlatformDefaultWidth = false,
    ),
) {
    Surface {
        Text("Custom content")
    }
}
```

### `SystemDialogProperties`

| Property | Description |
| --- | --- |
| `dismissOnBackPress` | Whether the back button dismisses the dialog. |
| `dismissOnClickOutside` | Whether a tap outside the dialog dismisses it. |
| `securePolicy` | Controls secure flag inheritance. |
| `usePlatformDefaultWidth` | Uses the platform dialog width when `true`. |
| `decorFitsSystemWindows` | Controls `WindowCompat.setDecorFitsSystemWindows`. |

## Screen metrics

### `DisplayHelper`

`DisplayHelper.metrics` returns `DisplayMetrics` for the screen that the floating window currently uses. The window classes expose an instance through `display`. Use it to compute coordinates relative to the visible screen.

## Default window parameters

A new floating window uses these defaults:

- Size: wrap content for width and height.
- Position: top-start corner of the screen.
- Pixel format: translucent.
- Flags: `FLAG_NOT_TOUCH_MODAL` (touches outside the window pass through), `FLAG_NOT_FOCUSABLE` (the window takes no focus), and `FLAG_HARDWARE_ACCELERATED`.
- Window type: non-activity contexts use `TYPE_APPLICATION_OVERLAY` on Android 8.0 and above, and `TYPE_SYSTEM_ALERT` on older versions. Activity contexts keep their default window type.

The window is not focusable by default. The keyboard interaction sources toggle focus when a text field gains focus.

## Lifecycle notes

- Call `setContent()` before `show()`. `show()` throws `IllegalArgumentException` when content is not set.
- If the overlay permission is missing, `show()` logs a warning and does not attach the floating window.
- Change `windowParams` while the floating window is visible, then call `update()`.
- `hide()` removes the view with a fade-out. The composition and its state stay intact.
- `close()` destroys the instance permanently. `show()` after `close()` throws `IllegalStateException`.
- When the user turns the screen off, a shown floating window moves to the stopped state. It resumes when the user turns the screen on.
- Prefer an application context when you create the floating window to avoid leaks.
