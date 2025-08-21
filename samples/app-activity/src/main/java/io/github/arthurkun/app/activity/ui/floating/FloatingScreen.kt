package io.github.arthurkun.app.activity.ui.floating

import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SystemAlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.only52607.compose.window.LocalFloatingWindow
import com.github.only52607.compose.window.dragFloatingWindow

@Composable
fun FloatingScreen(
    vm: FloatingViewModel = viewModel(),
) {
    LocalFloatingWindow.current

    val showing by vm.dialogVisible.collectAsStateWithLifecycle()

    if (showing) {
        SystemAlertDialog(
            onDismissRequest = { vm.dismissDialog() },
            confirmButton = {
                TextButton(onClick = { vm.dismissDialog() }) {
                    Text(text = "OK")
                }
            },
            text = {
                Text(text = "This is a system dialog")
            },
        )
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
