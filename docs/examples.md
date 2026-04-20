# Examples

These examples mirror the sample apps in this repository and focus on the patterns you are most likely to reuse.

## Activity-Owned Overlay

This is the smallest useful setup: create the window once, set content once, and toggle visibility from the activity.

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

Use this shape when the overlay is a companion to an activity screen and does not need to survive process-local navigation.

## Service-Owned Overlay

For overlays that should keep running after an activity goes away, create the window in a `Service`.

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

This is the right model for chat heads, quick tools, and utility overlays that need an independent lifecycle.

## Dialog From The Overlay

Use `SystemAlertDialog` when you want a Material-styled prompt above the overlay.

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

## Full-Screen Custom Dialog

Use `SystemDialog` when the alert-dialog slot API is too restrictive.

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

## Text Input Inside An Overlay

If your overlay contains `TextField` or `OutlinedTextField`, use the provided interaction source.

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

For a service-based overlay, switch to `rememberServiceFloatingWindowInteractionSource()`.

## Persist Window Position

The drag modifiers expose the current position through `onDrag`, so persisting coordinates is straightforward.

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

Restore those coordinates before showing the window again:

```kotlin
val overlay = ComposeFloatingWindow(applicationContext).apply {
    windowParams.x = savedPosition.first
    windowParams.y = savedPosition.second
    setContent { OverlayContent() }
}
```

The `samples/service-hilt` module demonstrates the same idea with a repository-backed value.