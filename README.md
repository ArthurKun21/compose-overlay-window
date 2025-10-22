# compose-floating-window

[![Release](https://jitpack.io/v/only52607/compose-floating-window.svg)](https://jitpack.io/#User/Repo)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Global Floating Window Framework based on Jetpack Compose

[简体中文](README_CN.md)

## Preview

![Preview](/preview/example.gif)

## Features

- Using Compose code to describe the floating window interface.
- ViewModel support.
- Support for draggable floating windows.
- Dialog components based on the Application Context.
- Smooth fade in/out animations.
- Automatic resize stability (no jumping when content size changes).
- Material3 theming support.

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
    implementation "com.github.only52607:compose-floating-window:1.0"
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

> See [Sample App](https://github.com/only52607/compose-floating-window/tree/master/app).

## Advanced Usage

### Apply Material3 Theme

To ensure proper styling and colors in your floating window, wrap your content in a MaterialTheme:

```kotlin
val floatingWindow = ComposeFloatingWindow(applicationContext)
floatingWindow.setContent {
    MaterialTheme(
        colorScheme = lightColorScheme()
    ) {
        FloatingActionButton(
            modifier = Modifier.dragFloatingWindow(),
            onClick = { /* action */ }
        ) {
            Icon(Icons.Filled.Call, "Call")
        }
    }
}
floatingWindow.show()
```

> **Important**: Always wrap your floating window content in MaterialTheme for proper theming.
> See [THEMING_GUIDE.md](THEMING_GUIDE.md) for comprehensive theming instructions.

### Make Floating Window Draggable

Use the `Modifier.dragFloatingWindow()` modifier on the component you want to make draggable. Example:

```kotlin
FloatingActionButton(
    modifier = Modifier.dragFloatingWindow()
) {
    Icon(Icons.Filled.Call, "Call")
}
```

### Get the current instance of `ComposeFloatingWindow`

Using LocalComposeFloatingWindow to retrieve, here's an example:

```kotlin
val floatingWindow = LocalComposeFloatingWindow.current
```

### Show Dialog

When the Context of the floating window is set to Application, using AlertDialog and Dialog in the Compose interface of the floating window may result in a 'token is null' exception. In such cases, you can use the SystemAlertDialog or SystemDialog components, which can be used in the same way as the built-in AlertDialog and Dialog components.

Example：
```kotlin
SystemAlertDialog(
    onDismissRequest = { showDialog = false },
    confirmButton = {
        TextButton(onClick = { showDialog = false }) {
            Text(text = "OK")
        }
    },
    text = {
        Text(text = "This is a system dialog")
    }
)
```

### ViewModel

#### Activity-Based Windows

`ComposeFloatingWindow` supports ViewModels through the standard `viewModel()` function:

```kotlin
class MyViewModel : ViewModel() { /*...*/ }

@Composable
fun MyScreen(
    viewModel: MyViewModel = viewModel()
) {
    // use viewModel here
}
```

#### Service-Based Windows

For `ComposeServiceFloatingWindow`, ViewModels must be created manually in your Service and passed as parameters:

```kotlin
class MyService : Service() {
    private var viewModel: MyViewModel? = null
    
    override fun onCreate() {
        viewModel = MyViewModel(repository)
        floatingWindow.setContent {
            viewModel?.let { MyContent(it) }
        }
    }
}
```

**Why manual?** This approach prevents conflicts with Hilt/Dagger and gives you explicit control over ViewModel lifecycle in Service contexts.

See [VIEWMODEL_SERVICE_GUIDE.md](VIEWMODEL_SERVICE_GUIDE.md) for complete examples including Hilt integration.

> See https://developer.android.com/jetpack/compose/libraries#viewmodel

## License

Apache 2.0 License