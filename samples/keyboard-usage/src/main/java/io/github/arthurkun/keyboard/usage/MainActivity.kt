package io.github.arthurkun.keyboard.usage

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.only52607.compose.core.checkOverlayPermission
import com.github.only52607.compose.window.ComposeFloatingWindow
import io.github.arthurkun.keyboard.usage.service.MyService
import io.github.arthurkun.keyboard.usage.ui.floating.FloatingScreen
import io.github.arthurkun.keyboard.usage.ui.theme.ComposeFloatingWindowTheme

class MainActivity : AppCompatActivity() {

    private val floatingWindow by lazy {
        createFloatingWindow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeFloatingWindowTheme {
                var showDialogPermission by rememberSaveable { mutableStateOf(false) }

                val showing by floatingWindow.isShowing.collectAsStateWithLifecycle()

                var innerText by remember { mutableStateOf("") }

                val focusManager = LocalFocusManager.current

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->
                    Column(
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    focusManager.clearFocus()
                                }
                            },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {

                        OutlinedTextField(
                            value = innerText,
                            onValueChange = { innerText = it },
                            label = { Text("Type something") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            maxLines = 1,
                        )

                        ActivitySample(
                            floatingWindow = floatingWindow,
                            show = ::show,
                            hide = ::hide,
                            showing = showing,
                            askForDialog = { showDialogPermission = true },
                        )

                        ServiceSample(
                            askForDialog = { showDialogPermission = true },
                        )



                    }
                }

                if (showDialogPermission) {
                    DialogPermission(
                        onDismiss = {
                            showDialogPermission = false
                        },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        if (floatingWindow.isShowing.value) {
            floatingWindow.hide()
            floatingWindow.close()
        }
        super.onDestroy()
    }

    private fun createFloatingWindow(): ComposeFloatingWindow =
        ComposeFloatingWindow(applicationContext).apply {
            setContent {
                FloatingScreen()
            }
        }

    private fun show() {
        floatingWindow.show()
    }

    private fun hide() {
        floatingWindow.hide()
    }
}

@Composable
private fun ActivitySample(
    floatingWindow: ComposeFloatingWindow,
    show: () -> Unit,
    hide: () -> Unit,
    showing: Boolean,
    askForDialog: () -> Unit,
) {
    Text(
        "Activity",
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        textAlign = TextAlign.Center,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            16.dp,
            Alignment.CenterHorizontally,
        ),
    ) {
        Button(
            onClick = {
                if (floatingWindow.isAvailable()) {
                    show()
                } else {
                    askForDialog()
                }
            },
            enabled = !showing,
        ) {
            Text("Show")
        }
        Button(
            onClick = hide,
            enabled = showing,
        ) {
            Text("Hide")
        }
    }
}

@Composable
private fun ServiceSample(
    askForDialog: () -> Unit,
) {
    val context = LocalContext.current
    val isServiceRunning by MyService.serviceStarted.collectAsStateWithLifecycle()

    Text(
        "Service",
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        textAlign = TextAlign.Center,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            16.dp,
            Alignment.CenterHorizontally,
        ),
    ) {
        Button(
            onClick = {
                if (checkOverlayPermission(context)) {
                    MyService.start(context)
                } else {
                    askForDialog()
                }
            },
            enabled = !isServiceRunning,
        ) {
            Text("Show")
        }
        Button(
            onClick = {
                MyService.stop(context)
            },
            enabled = isServiceRunning,
        ) {
            Text("Hide")
        }
    }
}
