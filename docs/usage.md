# Usage

This library has two primary entry points:

- `ComposeFloatingWindow` for overlays created and managed by app code such as an activity.
- `ComposeServiceFloatingWindow` for overlays managed by a long-running Android `Service`.

Both classes share the same core behavior: set Compose content, show the overlay, update its layout when needed, hide it, and call `close()` when you are done.

## 1. Add The Dependency

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

dependencies {
    implementation("com.github.ArthurKun21:compose-overlay-window:<tag>")
}
```

## 2. Declare The Permission

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

On Android 6.0 and above, the user must also grant the permission in system settings.

```kotlin
if (!checkOverlayPermission(this)) {
    requestOverlayPermission(this)
}
```

## 3. Create A Floating Window

Use the application context when possible to avoid leaking an activity.

```kotlin
private val floatingWindow by lazy {
    ComposeFloatingWindow(applicationContext).apply {
        setContent {
            FloatingActionButton(
                modifier = Modifier.dragFloatingWindow(),
                onClick = { /* Handle click */ },
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call")
            }
        }
    }
}
```

## 4. Show And Hide It

`show()` requires content to be set first. If the permission is missing, the library will refuse to attach the window.

```kotlin
fun showOverlay() {
    if (floatingWindow.isAvailable()) {
        floatingWindow.show()
    } else {
        requestOverlayPermission(this)
    }
}

fun hideOverlay() {
    floatingWindow.hide()
}
```

## 5. Close It When Finished

`close()` is a terminal operation. It hides the overlay if needed, disposes the Compose composition, clears `ViewModel` state, and marks the instance destroyed.

```kotlin
override fun onDestroy() {
    floatingWindow.close()
    super.onDestroy()
}
```

After `close()`, do not call `show()` or `setContent()` on the same instance again. Create a new floating window instead.

## Configure Position And Layout

You can set the initial coordinates directly through `windowParams` before `show()`.

```kotlin
val overlay = ComposeFloatingWindow(applicationContext).apply {
    windowParams.x = 48
    windowParams.y = 160
    setContent { OverlayContent() }
}
```

If you modify layout params while the overlay is visible, call `update()`.

```kotlin
overlay.windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT
overlay.windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
overlay.update()
```

To move the overlay programmatically, use `updateCoordinate(left, top)`.

```kotlin
overlay.updateCoordinate(left = 120, top = 240)
```

## Dragging

Attach the built-in modifier to the composable that should respond to drag gestures.

```kotlin
FloatingActionButton(
    modifier = Modifier.dragFloatingWindow(),
    onClick = { /* ... */ },
) {
    Icon(Icons.Default.Done, contentDescription = null)
}
```

For service overlays, use `Modifier.dragServiceFloatingWindow()`.

The drag modifiers clamp movement so the overlay stays within the visible display bounds reported by the window instance.

## Access The Window From Compose

Inside `setContent`, the current floating window is available through a composition local.

```kotlin
@Composable
fun OverlayBadge() {
    val overlay = LocalFloatingWindow.current
    Text(text = "x=${overlay.windowParams.x}, y=${overlay.windowParams.y}")
}
```

For service overlays, use `LocalServiceFloatingWindow.current`.

## Text Input And Keyboard Handling

When your overlay contains text fields, pass the provided interaction source to input components so the library can coordinate focus and soft keyboard behavior correctly.

```kotlin
@Composable
fun OverlayForm() {
    val interactionSource = rememberFloatingWindowInteractionSource()

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        interactionSource = interactionSource,
        label = { Text("Type something") },
    )
}
```

For service overlays, use `rememberServiceFloatingWindowInteractionSource()`.

## Dialogs Inside Overlays

The library exposes overlay-safe dialog APIs:

- `SystemAlertDialog` for Material 3 alert dialogs.
- `SystemDialog` for fully custom dialog content.

```kotlin
if (showingDialog) {
    SystemAlertDialog(
        onDismissRequest = { showingDialog = false },
        confirmButton = {
            TextButton(onClick = { showingDialog = false }) {
                Text("OK")
            }
        },
        text = { Text("This dialog is shown above the overlay window") },
    )
}
```

## Activity Vs Service

Use `ComposeFloatingWindow` when the overlay belongs to the app UI lifecycle and can be created from an activity.

Use `ComposeServiceFloatingWindow` when the overlay should outlive an activity and stay attached to a service lifecycle.

The API is intentionally similar between the two so you can keep most of your composable content unchanged.
