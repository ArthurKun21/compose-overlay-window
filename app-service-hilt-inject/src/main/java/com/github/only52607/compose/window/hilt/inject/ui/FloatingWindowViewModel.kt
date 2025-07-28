package com.github.only52607.compose.window.hilt.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.only52607.compose.window.hilt.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

class FloatingWindowViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val darkMode: Flow<Boolean>
        get() = userPreferencesRepository.darkModeFlow

    private var _dialogVisible by mutableStateOf(false)
    val dialogVisible: Boolean get() = _dialogVisible

    fun showDialog(value: Boolean) = viewModelScope.launch {
        _dialogVisible = true

        userPreferencesRepository.setDarkMode(value)
    }

    fun dismissDialog() {
        _dialogVisible = false
    }
}