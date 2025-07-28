package com.github.only52607.compose.window.hilt.inject

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.only52607.compose.window.checkOverlayPermission
import com.github.only52607.compose.window.hilt.inject.repository.UserPreferencesRepository
import com.github.only52607.compose.window.hilt.inject.ui.DialogPermission
import com.github.only52607.compose.window.hilt.inject.ui.theme.ComposeFloatingWindowTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeFloatingWindowTheme {
                LaunchedEffect(userPreferencesRepository.darkModeFlow) {
                    userPreferencesRepository
                        .darkModeFlow
                        .distinctUntilChanged()
                        .collect { darkMode ->
                            AppCompatDelegate.setDefaultNightMode(
                                if (darkMode) AppCompatDelegate.MODE_NIGHT_YES
                                else AppCompatDelegate.MODE_NIGHT_NO
                            )
                        }
                }
                var showDialogPermission by rememberSaveable { mutableStateOf(false) }

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
                                val overlayPermission = checkOverlayPermission(context)
                                if (overlayPermission) {
                                    MyService.start(context)
                                } else {
                                    showDialogPermission = true
                                }
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
                }

                if (showDialogPermission) {
                    DialogPermission(
                        onDismiss = {
                            showDialogPermission = false
                        }
                    )
                }
            }
        }
    }


}