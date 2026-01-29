# compose-floating-window

[![Release](https://jitpack.io/v/ArthurKun21/compose-overlay-window.svg)](https://jitpack.io/#ArthurKun21/compose-overlay-window)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Global Floating Window Framework based on Jetpack Compose

## Preview

![Preview](/preview/example.gif)

## Features

- Using Compose code to describe the floating window interface.
- ViewModel support.
- Support for draggable floating windows.
- Dialog components based on the Application Context.

## Basic Usage

### Import Dependencies

- Add on settings.gradle.kts

```kotlin
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

- Add `compose-floating-window` Dependency

```kotlin
dependencies {
    implementation("com.github.ArthurKun21:compose-overlay-window:<tag>")
}
```

### Grant Floating Window Permission

Add to `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

### Activity

When you want to show the floating window while on the Activity, you can use the following code:

```kotlin
val floatingWindow = ComposeFloatingWindow(applicationContext)
floatingWindow.setContent {
    FloatingActionButton(
        modifier = Modifier.dragFloatingWindow(),
        onClick = {
            Log.i("")
        }) {
        Icon(Icons.Filled.Call, "Call")
    }
}
floatingWindow.show()
```

### Service

When you want to show the floating window while on the Service, you can use the following code:

```kotlin
val floatingWindow = ComposeServiceFloatingWindow(applicationContext)
floatingWindow.setContent {
    FloatingActionButton(
        modifier = Modifier.dragFloatingWindow(),
        onClick = {
            Log.i("")
        }) {
        Icon(Icons.Filled.Call, "Call")
    }
}
floatingWindow.show()
```

## Advanced Usage

> See [Sample Apps](samples).

## License

Apache 2.0 License