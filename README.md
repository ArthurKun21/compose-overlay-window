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

- If the Gradle version is less than 7.0, add the Jitpack repository in the `build.gradle` of your app.

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

- If the Gradle version is greater than or equal to 7.0, add it in the settings.gradle file.

```groovy
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

- Add `compose-floating-window` Dependency

```groovy
dependencies {
    implementation "com.github.ArthurKun21:compose-overlay-window:<tag>"
}
```

### Grant Floating Window Permission

Add to `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

### Create Floating Window and Show

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

> See [Sample Apps](samples).

## License

Apache 2.0 License