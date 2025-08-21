package io.github.arthurkun.keyboard.usage

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.only52607.compose.window.ComposeFloatingWindow
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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->
                    Column(
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Button(
                            onClick = {
                                if (floatingWindow.isAvailable()) {
                                    show()
                                } else {
                                    showDialogPermission = true
                                }
                            },
                            enabled = !showing,
                        ) {
                            Text("Show")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = ::hide,
                            enabled = showing,
                        ) {
                            Text("Hide")
                        }
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
