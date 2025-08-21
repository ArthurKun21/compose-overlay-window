package io.github.arthurkun.service.hilt.ui.floating

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.arthurkun.service.hilt.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FloatingViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val location: Flow<Pair<Int, Int>>
        get() = userPreferencesRepository.locationFlow

    val darkMode: Flow<Boolean>
        get() = userPreferencesRepository.darkModeFlow

    private var _dialogVisible = MutableStateFlow(false)
    val dialogVisible: StateFlow<Boolean>
        get() = _dialogVisible.asStateFlow()

    fun showDialog(value: Boolean) = viewModelScope.launch {
        _dialogVisible.update { true }

        userPreferencesRepository.setDarkMode(value)
    }

    fun dismissDialog() {
        _dialogVisible.update { false }
    }

    fun updateLocation(x: Int, y: Int) = viewModelScope.launch {
        userPreferencesRepository.setLocation(x, y)
    }
}
