# Service

## Initial Setup

This is a sample project on how to use `compose-floating-window` on a service for long running operations

1. Create a new service. Like [this](src/main/java/com/github/only52607/compose/window/service/MyService.kt) for example
        
2. Add the permission to the manifest file

    ```xml
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    ```

3. Declare the service in the manifest file

    ```xml
    <service android:name=".MyService" />
    ```
   
4. Follow the `MyService` sample on how to use it on a service.

## Usage

Do not follow the `build.gradle.kts` setup as it was done for the sample project

Note:
- Be sure to `AppCompatActivity` instead of `ComponentActivity` in order to change the theme of the app

This is mostly just a sample project on how to incorporate hilt into viewmodels and services.

![compose-floating-window](https://github.com/user-attachments/assets/2201f599-137d-48ba-8c79-66eb86461fa3)

Note:
- The service is not aware of the app's theme colors. To fix this, you need to pass the default theme, which is ComposeFloatingWindowTheme in this app. You can then decide whether to modify this theme directly or use preferences to allow users to dynamically update the theme.

```kotlin
val darkMode by model.darkMode.collectAsStateWithLifecycle(false)

ComposeFloatingWindowTheme(darkTheme = darkMode) {
```

![Theme floating window](https://github.com/user-attachments/assets/22b446ce-34b5-4ba0-955c-b270f3c30f90)
