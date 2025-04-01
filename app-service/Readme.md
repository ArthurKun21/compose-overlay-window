# Service

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