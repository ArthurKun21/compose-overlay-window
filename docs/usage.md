# Usage

This library has two main window classes:

- `ComposeFloatingWindow` for floating windows created and managed by app code such as an activity.
- `ComposeServiceFloatingWindow` for floating windows managed by a long-running Android `Service`.

Both classes work the same way:

1. Set Compose content with `setContent()`.
2. Show the floating window with `show()`.
3. Update the layout with `update()` when needed.
4. Hide the floating window with `hide()`.
5. Call `close()` when you finish.

## Add the dependency

You can get the library from JitPack.

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

## Declare the permission

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

The user must also grant this permission in system settings.

```kotlin
if (!checkOverlayPermission(this)) {
    requestOverlayPermission(this)
}
```

## Create a floating window

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

## Show and hide it

`show()` throws `IllegalArgumentException` if you did not call `setContent()` first. If the overlay permission is missing, the library logs a warning and does not show the floating window.

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

`hide()` removes the floating window from the screen. The composition and its state stay intact, so you can call `show()` again.

When the user turns the screen off, a shown floating window moves to the stopped state. It returns to the resumed state when the user turns the screen on.

## Close it when finished

Call `close()` when you finish with the floating window. `close()` is permanent. It hides the floating window if needed and disposes the Compose composition. It also clears the `ViewModel` store and marks the instance destroyed.

```kotlin
override fun onDestroy() {
    floatingWindow.close()
    super.onDestroy()
}
```

After `close()`, do not call `show()` or `setContent()` on the same instance again. `show()` on a closed window throws `IllegalStateException`. Create a new floating window instead.

## Configure position and layout

Set the initial coordinates on `windowParams` before `show()`.

```kotlin
val overlay = ComposeFloatingWindow(applicationContext).apply {
    windowParams.x = 48
    windowParams.y = 160
    setContent { OverlayContent() }
}
```

If you change the layout params while the floating window is visible, call `update()`.

```kotlin
overlay.windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT
overlay.windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT
overlay.update()
```

To move the floating window from code, use `updateCoordinate(left, top)`.

```kotlin
overlay.updateCoordinate(left = 120, top = 240)
```

## Dragging

Attach the drag modifier to the composable that must respond to drag gestures.

```kotlin
FloatingActionButton(
    modifier = Modifier.dragFloatingWindow(),
    onClick = { /* ... */ },
) {
    Icon(Icons.Default.Done, contentDescription = null)
}
```

For service floating windows, use `Modifier.dragServiceFloatingWindow()`.

The drag modifiers keep the floating window within the visible screen bounds.

## Access the window from Compose

Inside `setContent`, the current floating window is available through a composition local.

```kotlin
@Composable
fun OverlayBadge() {
    val overlay = LocalFloatingWindow.current
    Text(text = "x=${overlay.windowParams.x}, y=${overlay.windowParams.y}")
}
```

For service floating windows, use `LocalServiceFloatingWindow.current`.

## Text input and keyboard handling

A text field inside a floating window needs the provided interaction source. Pass it to your input components. The library uses it to coordinate focus and the soft keyboard.

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

For service floating windows, use `rememberServiceFloatingWindowInteractionSource()`.

## Dialogs inside overlays

The library provides two dialog APIs that can show above other apps:

- `SystemAlertDialog` for Material 3 alert dialogs.
- `SystemDialog` for fully custom dialog content.

Note the package names. `SystemAlertDialog` is in `androidx.compose.material3`, and `SystemDialog` is in `androidx.compose.ui.window`.

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

## Activity vs service

Use `ComposeFloatingWindow` when the floating window belongs to the app UI lifecycle and can be created from an activity.

Use `ComposeServiceFloatingWindow` when the floating window must outlive an activity and stay attached to a service lifecycle.

The two APIs are similar. In most cases, you can reuse your composable content without changes.
