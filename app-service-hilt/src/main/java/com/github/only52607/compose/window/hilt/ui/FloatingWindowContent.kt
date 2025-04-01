package com.github.only52607.compose.window.hilt.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SystemAlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.only52607.compose.window.LocalFloatingWindow
import com.github.only52607.compose.window.dragFloatingWindow
import com.github.only52607.compose.window.hilt.ui.theme.ComposeFloatingWindowTheme

@Composable
fun FloatingWindowContent(
    model: FloatingWindowViewModel
) {
    val floatingWindow = LocalFloatingWindow.current

    val darkMode by model.darkMode.collectAsStateWithLifecycle(false)

    ComposeFloatingWindowTheme(darkTheme = darkMode) {

        if (model.dialogVisible) {
            SystemAlertDialog(
                onDismissRequest = { model.dismissDialog() },
                confirmButton = {
                    TextButton(onClick = { model.dismissDialog() }) {
                        Text(text = "OK")
                    }
                },
                text = {
                    Text(
                        text = "This is now ${if (darkMode) "Dark" else "Light"} mode",
                    )
                }
            )
        }
        FloatingActionButton(
            modifier = Modifier.dragFloatingWindow(),
            onClick = {
                model.showDialog(!darkMode)
            },
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp
            )
        ) {
            Icon(Icons.Filled.Call, "Call")
        }
    }
}