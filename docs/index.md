# Compose Floating Window

Compose Floating Window is a global floating window framework based on Jetpack Compose.

![Preview](assets/example.gif)

## Download

[![Release](https://jitpack.io/v/ArthurKun21/compose-overlay-window.svg)](https://jitpack.io/#ArthurKun21/compose-overlay-window)

``` kotlin
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    implementation("com.github.ArthurKun21:compose-floating-window:<TAG>")
}
```

## Permissions

Add to `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

## Acknowledgements

The initial implementation of this library is based on [https://github.com/only52607/compose-floating-window](https://github.com/only52607/compose-floating-window)

## License

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)