# compose-floating-window

[![Release](https://jitpack.io/v/ArthurKun21/compose-overlay-window.svg)](https://jitpack.io/#ArthurKun21/compose-overlay-window)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Global Floating Window Framework based on Jetpack Compose

## Preview

![Preview](docs/assets/example.gif)

## Features

- Describe the floating window UI with Compose code.
- `ViewModel` support inside the floating window.
- Draggable floating windows that stay on screen.
- Dialog components that show from an application context.

## Requirements

The library requires Android 7.0 (API level 24) or newer.

## Basic Usage

### Import the dependency

Add the JitPack repository to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the dependency:

```kotlin
dependencies {
    implementation("com.github.ArthurKun21:compose-overlay-window:<tag>")
}
```

### Grant the floating window permission

Add to `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

The user must also grant this permission in system settings. Check it with `checkOverlayPermission(context)`, and open the settings screen with `requestOverlayPermission(context)`.

### Activity

Use `ComposeFloatingWindow` to show a floating window from an activity:

```kotlin
val floatingWindow = ComposeFloatingWindow(applicationContext)
floatingWindow.setContent {
    FloatingActionButton(
        modifier = Modifier.dragFloatingWindow(),
        onClick = {
            // Handle tap
        },
    ) {
        Icon(Icons.Filled.Call, "Call")
    }
}

if (checkOverlayPermission(applicationContext)) {
    floatingWindow.show()
} else {
    requestOverlayPermission(this)
}
```

### Service

Use `ComposeServiceFloatingWindow` to show a floating window from a service. The service content needs `dragServiceFloatingWindow` as the drag modifier:

```kotlin
val floatingWindow = ComposeServiceFloatingWindow(applicationContext)
floatingWindow.setContent {
    FloatingActionButton(
        modifier = Modifier.dragServiceFloatingWindow(),
        onClick = {
            // Handle tap
        },
    ) {
        Icon(Icons.Filled.Call, "Call")
    }
}

if (checkOverlayPermission(applicationContext)) {
    floatingWindow.show()
} else {
    requestOverlayPermission(this)
}
```

## Advanced Usage

The [documentation site](https://arthurkun21.github.io/compose-overlay-window) has the full usage guide, examples, and API reference.

The sample apps in this repository show complete setups:

- `samples/app-activity`
- `samples/keyboard-usage`
- `samples/fullscreen-dialog`
- `samples/service-hilt`

## License

Apache License 2.0
