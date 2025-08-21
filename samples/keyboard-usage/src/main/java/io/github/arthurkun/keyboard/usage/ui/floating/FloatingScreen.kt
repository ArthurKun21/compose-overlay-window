package io.github.arthurkun.keyboard.usage.ui.floating

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SystemAlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.only52607.compose.window.dragFloatingWindow
import io.github.arthurkun.keyboard.usage.ui.theme.ComposeFloatingWindowTheme

@Composable
fun FloatingScreen(
    vm: FloatingViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    ComposeFloatingWindowTheme {
        if (state.isDialogVisible) {
            SystemAlertDialog(
                onDismissRequest = { vm.dismissDialog() },
                confirmButton = {
                    TextButton(onClick = { vm.dismissDialog() }) {
                        Text(text = "OK")
                    }
                },
                text = {
                    Text(
                        text = when (state.text.isBlank()) {
                            true -> "No text entered"
                            else -> "You typed: ${state.text}"
                        },
                    )
                },
            )
        }

        Surface(
            modifier = Modifier
                .dragFloatingWindow()
                .border(
                    Dp.Hairline,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedTextField(
                    value = state.text,
                    onValueChange = vm::onTextUpdate,
                    label = { Text("Type something") },
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = vm::showDialog,
                ) {
                    Text("Submit")
                }
            }
        }
    }
}
