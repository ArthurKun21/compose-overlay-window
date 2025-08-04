package com.github.only52607.compose.window.hilt.inject.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.only52607.compose.window.hilt.inject.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FloatingWindowViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val location: Flow<Pair<Int, Int>>
        get() = userPreferencesRepository.locationFlow

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

    fun updateLocation(x: Int, y: Int) = viewModelScope.launch {
        userPreferencesRepository.setLocation(x, y)
    }
}