# Examples

These examples match the sample apps in this repository. They show the patterns you will reuse most often.

## Activity-owned overlay

This is the smallest useful setup:

1. Create the floating window once.
2. Set the content once.
3. Toggle visibility from the activity.

```kotlin
class MainActivity : AppCompatActivity() {

    private val floatingWindow by lazy {
        ComposeFloatingWindow(applicationContext).apply {
            setContent {
                FloatingActionButton(
                    modifier = Modifier.dragFloatingWindow(),
                    onClick = { /* Open something */ },
                ) {
                    Icon(Icons.Default.Done, contentDescription = "Show dialog")
                }
            }
        }
    }

    private fun showOverlay() {
        if (floatingWindow.isAvailable()) {
            floatingWindow.show()
        } else {
            requestOverlayPermission(this)
        }
    }

    private fun hideOverlay() {
        floatingWindow.hide()
    }

    override fun onDestroy() {
        floatingWindow.close()
        super.onDestroy()
    }
}
```

Use this pattern when the floating window only needs to live as long as the activity. For a floating window that must live longer, see the service pattern below.

## Service-owned overlay

For floating windows that must keep running after an activity goes away, create the floating window in a `Service`.

```kotlin
class MyService : Service() {

    private val floatingWindow by lazy {
        ComposeServiceFloatingWindow(this).apply {
            setContent {
                FloatingActionButton(
                    modifier = Modifier.dragServiceFloatingWindow(),
                    onClick = { /* Service action */ },
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Action")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        floatingWindow.show()
    }

    override fun onDestroy() {
        floatingWindow.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

Use this pattern for chat heads, quick tools, and other floating windows that need an independent lifecycle.

## Dialog from the overlay

Use `SystemAlertDialog` when you want a Material 3 alert dialog above the floating window.

```kotlin
@Composable
fun FloatingScreen(vm: FloatingViewModel = viewModel()) {
    val showing by vm.dialogVisible.collectAsStateWithLifecycle()

    if (showing) {
        SystemAlertDialog(
            onDismissRequest = vm::dismissDialog,
            confirmButton = {
                TextButton(onClick = vm::dismissDialog) {
                    Text("OK")
                }
            },
            text = { Text("This is a system dialog") },
        )
    }

    FloatingActionButton(
        modifier = Modifier.dragFloatingWindow(),
        onClick = vm::showDialog,
    ) {
        Icon(Icons.Default.Done, contentDescription = null)
    }
}
```

## Full-screen custom dialog

Use `SystemDialog` when you need full control over the dialog content.

```kotlin
if (showingDialog) {
    SystemDialog(
        onDismissRequest = { showingDialog = false },
        properties = SystemDialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("This is a full-screen dialog")
                Button(onClick = { showingDialog = false }) {
                    Text("Dismiss")
                }
            }
        }
    }
}
```

## Text input inside an overlay

If your floating window contains `TextField` or `OutlinedTextField`, use the provided interaction source.

```kotlin
@Composable
fun OverlayInput() {
    var text by rememberSaveable { mutableStateOf("") }
    val interactionSource = rememberFloatingWindowInteractionSource()

    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        interactionSource = interactionSource,
        label = { Text("Type something") },
    )
}
```

For a floating window in a service, use `rememberServiceFloatingWindowInteractionSource()`.

## Persist window position

The drag modifiers report the current position through `onDrag`. Save the coordinates in this callback.

```kotlin
FloatingActionButton(
    modifier = Modifier.dragFloatingWindow(
        onDrag = { left, top ->
            savedPosition = left to top
        },
    ),
    onClick = { /* ... */ },
) {
    Icon(Icons.Default.Done, contentDescription = null)
}
```

Restore those coordinates before you show the floating window again:

```kotlin
val overlay = ComposeFloatingWindow(applicationContext).apply {
    windowParams.x = savedPosition.first
    windowParams.y = savedPosition.second
    setContent { OverlayContent() }
}
```

The `samples/service-hilt` module shows the same idea with a persistent data store.
