# API Reference

This page summarizes the public API exposed by the library module.

## Window Types

### `ComposeFloatingWindow`

Creates an overlay window backed by app code such as an activity.

```kotlin
val overlay = ComposeFloatingWindow(
    context = applicationContext,
)
```

Public entry points:

| API | Description |
| --- | --- |
| `setContent(content)` | Sets the Compose UI rendered inside the overlay. Must be called before `show()`. |
| `show()` | Attaches the overlay to `WindowManager` if permission is available. |
| `hide()` | Removes the overlay view with a fade-out animation. |
| `close()` | Final cleanup. Disposes composition, clears state, and destroys the instance. |
| `updateCoordinate(left, top)` | Moves the overlay to a specific coordinate pair. |
| `update()` | Re-applies the current `windowParams` while the overlay is showing. |
| `isAvailable()` | Returns whether `SYSTEM_ALERT_WINDOW` is granted. |
| `windowParams` | Mutable `WindowManager.LayoutParams` used for size, flags, gravity, and position. |
| `isShowing` | `StateFlow<Boolean>` that tracks whether the overlay is currently attached. |
| `isDestroyed` | `StateFlow<Boolean>` that tracks whether `close()` has already been called. |
| `display` | Helper for reading display metrics. |
| `maxXCoordinate` | Maximum in-bounds horizontal position for the current overlay size. |
| `maxYCoordinate` | Maximum in-bounds vertical position for the current overlay size. |

### `ComposeServiceFloatingWindow`

Service-scoped variant of `ComposeFloatingWindow`. The API is intentionally parallel, but it is designed to be owned by a `Service` lifecycle.

```kotlin
val overlay = ComposeServiceFloatingWindow(context = this)
```

Use it when the overlay should survive beyond an activity and be controlled by a background component.

## Permission Helpers

| API | Description |
| --- | --- |
| `checkOverlayPermission(context)` | Returns `true` when the app can draw overlays. |
| `requestOverlayPermission(context)` | Launches the system settings screen for granting overlay permission. |

Typical usage:

```kotlin
if (checkOverlayPermission(this)) {
    overlay.show()
} else {
    requestOverlayPermission(this)
}
```

## Drag And Window Access Helpers

### Composition locals

| API | Description |
| --- | --- |
| `LocalFloatingWindow` | Provides the current `ComposeFloatingWindow` inside `setContent { ... }`. |
| `LocalServiceFloatingWindow` | Provides the current `ComposeServiceFloatingWindow` inside service overlay content. |

### Drag modifiers

| API | Description |
| --- | --- |
| `Modifier.dragFloatingWindow(...)` | Makes app-owned overlay content draggable. |
| `Modifier.dragServiceFloatingWindow(...)` | Makes service-owned overlay content draggable. |

Both drag modifiers accept optional callbacks:

| Parameter | Description |
| --- | --- |
| `onDragStart` | Called when dragging begins. |
| `onDragEnd` | Called when dragging finishes normally. |
| `onDragCancel` | Called when dragging is cancelled. |
| `onDrag` | Receives the current `(left, top)` coordinates while dragging. |

### Keyboard interaction sources

| API | Description |
| --- | --- |
| `rememberFloatingWindowInteractionSource()` | Use with text inputs inside `ComposeFloatingWindow`. |
| `rememberServiceFloatingWindowInteractionSource()` | Use with text inputs inside `ComposeServiceFloatingWindow`. |

## Dialog APIs

### `SystemAlertDialog`

Material 3 alert dialog rendered as a system overlay dialog.

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

Use this when the standard Material alert layout is enough.

### `SystemDialog`

Low-level dialog API for fully custom content rendered in a system dialog window.

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
| `dismissOnBackPress` | Whether back dismisses the dialog. |
| `dismissOnClickOutside` | Whether tapping outside dismisses the dialog. |
| `securePolicy` | Controls secure flag inheritance. |
| `usePlatformDefaultWidth` | Uses Android's platform dialog width when `true`. |
| `decorFitsSystemWindows` | Controls `WindowCompat.setDecorFitsSystemWindows`. |

## Display Metrics

### `DisplayHelper`

`DisplayHelper.metrics` returns `DisplayMetrics` for the display currently used by the overlay. The window classes expose an instance through `display`, which is useful when computing coordinates relative to the visible screen.

## Lifecycle Notes

- Call `setContent()` before `show()`.
- Mutating `windowParams` while the overlay is visible requires `update()`.
- `hide()` makes the overlay invisible but keeps the instance reusable.
- `close()` destroys the instance permanently.
- Prefer an application context when creating the window to avoid leaks.