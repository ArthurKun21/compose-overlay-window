# Compose Floating Window

Compose Floating Window shows Jetpack Compose UI in a system overlay window. It covers the common cases: draggable floating buttons, service-owned windows, text input, and system dialogs rendered from Compose. You do not need to write raw `WindowManager` code.

![Preview](assets/example.gif)

## What it gives you

- `ComposeFloatingWindow` for floating windows created from an activity or another app component.
- `ComposeServiceFloatingWindow` for floating windows that live inside an Android `Service`.
- Drag modifiers that keep the floating window on screen.
- Permission helpers for `SYSTEM_ALERT_WINDOW`.
- Overlay dialog APIs: `SystemAlertDialog` and `SystemDialog`.
- Integration points for `ViewModel`, saved state, and text input.

## Installation

The library requires Android 7.0 (API level 24) or newer. You can get it from JitPack.

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

## Required permission

Add the overlay permission to your manifest:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

Before calling `show()`, check the permission with `checkOverlayPermission(context)` or `floatingWindow.isAvailable()`. If the permission is missing, open the system settings screen with `requestOverlayPermission(context)`.

## Quick start

This activity creates a floating window with a draggable button:

```kotlin
class MainActivity : AppCompatActivity() {

    private val floatingWindow by lazy {
        ComposeFloatingWindow(applicationContext).apply {
            setContent {
                FloatingActionButton(
                    modifier = Modifier.dragFloatingWindow(),
                    onClick = { /* Handle tap */ },
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Open")
                }
            }
        }
    }

    fun showOverlay() {
        if (floatingWindow.isAvailable()) {
            floatingWindow.show()
        } else {
            requestOverlayPermission(this)
        }
    }

    override fun onDestroy() {
        floatingWindow.close()
        super.onDestroy()
    }
}
```

## How to read the docs

- Start with Usage for setup, lifecycle, and window configuration.
- Read Examples for activity windows, service windows, keyboard handling, and dialogs.
- Use API Reference as a compact guide to the public classes and helpers.

## Samples in this repository

The sample apps demonstrate the main library features:

- `samples/app-activity`: an activity-owned floating button and a `SystemAlertDialog`.
- `samples/keyboard-usage`: text input with the keyboard interaction sources.
- `samples/fullscreen-dialog`: a full-screen `SystemDialog`.
- `samples/service-hilt`: a service-owned floating window with dependency injection and a saved position.

## Acknowledgements

only52607 built the upstream project that inspired this library. This repository adapts that implementation.

## License

This library uses the Apache License 2.0.
