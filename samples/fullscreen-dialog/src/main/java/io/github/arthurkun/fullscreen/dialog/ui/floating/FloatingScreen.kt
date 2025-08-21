package io.github.arthurkun.fullscreen.dialog.ui.floating

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.SystemDialog
import androidx.compose.ui.window.SystemDialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.only52607.compose.window.dragFloatingWindow
import io.github.arthurkun.fullscreen.dialog.ui.theme.ComposeFloatingWindowTheme

@Composable
fun FloatingScreen(
    vm: FloatingViewModel = viewModel(),
) {
    val showing by vm.dialogVisible.collectAsStateWithLifecycle()

    ComposeFloatingWindowTheme {
        if (showing) {
            SystemDialog(
                onDismissRequest = vm::dismissDialog,
                properties = SystemDialogProperties(
                    usePlatformDefaultWidth = false,
                ),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("This is a fullscreen dialog!")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = vm::dismissDialog) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            modifier = Modifier.dragFloatingWindow(),
            onClick = {
                vm.showDialog()
            },
        ) {
            AnimatedContent(showing) { isVisible ->
                if (isVisible) {
                    Icon(Icons.Default.Close, contentDescription = "Hide Dialog")
                } else {
                    Icon(Icons.Default.Done, contentDescription = "Show Dialog")
                }
            }
        }
    }
}
