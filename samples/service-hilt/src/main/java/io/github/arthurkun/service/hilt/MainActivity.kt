package io.github.arthurkun.service.hilt

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.only52607.compose.core.checkOverlayPermission
import dagger.hilt.android.AndroidEntryPoint
import io.github.arthurkun.service.hilt.repository.UserPreferencesRepository
import io.github.arthurkun.service.hilt.service.MyService
import io.github.arthurkun.service.hilt.ui.theme.ComposeFloatingWindowTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                userPreferencesRepository.darkModeFlow.collect { darkMode ->
                    withContext(Dispatchers.Main) {
                        AppCompatDelegate.setDefaultNightMode(
                            if (darkMode) {
                                AppCompatDelegate.MODE_NIGHT_YES
                            } else {
                                AppCompatDelegate.MODE_NIGHT_NO
                            },
                        )
                    }
                }
            }
        }

        setContent {
            ComposeFloatingWindowTheme {
                var showDialogPermission by rememberSaveable { mutableStateOf(false) }
                val isShowing by MyService.serviceStarted.collectAsStateWithLifecycle()
                val context = LocalContext.current

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
                                val overlayPermission = checkOverlayPermission(context)
                                if (overlayPermission) {
                                    MyService.start(context)
                                } else {
                                    showDialogPermission = true
                                }
                            },
                            enabled = !isShowing,
                        ) {
                            Text("Show")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                MyService.stop(context)
                            },
                            enabled = isShowing,
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
}
