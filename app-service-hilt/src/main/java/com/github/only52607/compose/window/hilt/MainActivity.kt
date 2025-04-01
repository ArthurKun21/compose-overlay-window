package com.github.only52607.compose.window.hilt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.only52607.compose.window.hilt.ui.DialogPermission
import com.github.only52607.compose.window.hilt.ui.theme.ComposeFloatingWindowTheme


class MainActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeFloatingWindowTheme {
                val showDialogPermission = remember { mutableStateOf(false) }

                val context = LocalContext.current

                val isShowing by MyService.serviceStarted.collectAsStateWithLifecycle(false)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = {
                                MyService.start(context)
                            },
                            enabled = !isShowing
                        ) {
                            Text("Show")
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                MyService.stop(context)
                            },
                            enabled = isShowing
                        ) {
                            Text("Hide")
                        }
                    }
                    DialogPermission(showDialogState = showDialogPermission)
                }
            }
        }
    }


}