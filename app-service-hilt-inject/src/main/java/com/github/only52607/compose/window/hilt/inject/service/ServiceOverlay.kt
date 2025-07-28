package com.github.only52607.compose.window.hilt.inject.service

import android.content.Context
import com.github.only52607.compose.service.ComposeServiceFloatingWindow
import com.github.only52607.compose.window.hilt.inject.repository.UserPreferencesRepository
import com.github.only52607.compose.window.hilt.inject.ui.FloatingWindowContent
import com.github.only52607.compose.window.hilt.inject.ui.FloatingWindowViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Inject

@ServiceScoped
class ServiceOverlay @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @param:ApplicationContext private val context: Context,
) {

    private val viewModel by lazy {
        FloatingWindowViewModel(userPreferencesRepository)
    }

    private val floatingWindow by lazy {
        createFloatingWindow()
    }

    private fun createFloatingWindow(): ComposeServiceFloatingWindow =
        ComposeServiceFloatingWindow(context).apply {
            setContent {
                FloatingWindowContent(viewModel)
            }
        }

    fun show() {
        if (!floatingWindow.isShowing.value) {
            floatingWindow.show()
        }
    }

    fun close() {
        floatingWindow.hide()
        floatingWindow.close()
    }
}