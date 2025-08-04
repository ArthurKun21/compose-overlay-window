package com.github.only52607.compose.window.hilt.inject.service

import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.only52607.compose.service.ComposeServiceFloatingWindow
import com.github.only52607.compose.window.hilt.inject.repository.UserPreferencesRepository
import com.github.only52607.compose.window.hilt.inject.ui.FloatingWindowContent
import com.github.only52607.compose.window.hilt.inject.ui.FloatingWindowViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@ServiceScoped
class ServiceOverlay @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @param:ApplicationContext private val context: Context,
) {

    private var viewModel: FloatingWindowViewModel? = null
    private var floatingWindow: ComposeServiceFloatingWindow? = null


    init {
        floatingWindow = createFloatingWindow()
    }

    private fun createFloatingWindow(): ComposeServiceFloatingWindow {
        // Create ViewModel only when needed
        viewModel = FloatingWindowViewModel(userPreferencesRepository)

        return ComposeServiceFloatingWindow(context).apply {
            setContent {
                var initialization by remember { mutableStateOf(false) }
                viewModel?.let { vm ->
                    LaunchedEffect(Unit) {
                        vm.location.first().let { location ->
                            windowParams.x = location.first
                            windowParams.y = location.second
                            update()
                        }
                        initialization = true
                    }
                    if (initialization) {
                        FloatingWindowContent(vm)
                    }
                }
            }
        }
    }

    fun show() {
        if (floatingWindow == null) {
            floatingWindow = createFloatingWindow()
        }
        floatingWindow?.let { window ->
            if (!window.isShowing.value) {
                window.show()
            }
        }
    }

    fun close() {
        floatingWindow?.let { window ->
            window.hide()
            window.close()
        }
        floatingWindow = null
        viewModel = null
    }
}