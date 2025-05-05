package com.github.only52607.compose.window.app.ui

import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.only52607.compose.window.LocalFloatingWindow
import com.github.only52607.compose.window.dragFloatingWindow

@Composable
fun FloatingWindowContent(
    model: FloatingWindowViewModel = viewModel()
) {
    val floatingWindow = LocalFloatingWindow.current
    if (model.dialogVisible) {
        SystemAlertDialog(
            onDismissRequest = { model.dismissDialog() },
            confirmButton = {
                TextButton(onClick = { model.dismissDialog() }) {
                    Text(text = "OK")
                }
            },
            text = {
                Text(text = "This is a system dialog")
            }
        )
    }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val iconSize = if (expanded) 140.dp else 40.dp
    FloatingActionButton(
        modifier = Modifier.dragFloatingWindow(),
        onClick = { expanded = !expanded },
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp
        )
    ) {
        Icon(Icons.Filled.Call, "Call", modifier = Modifier.size(iconSize))
    }
}