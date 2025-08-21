package io.github.arthurkun.keyboard.usage.ui.floating

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FloatingViewModel : ViewModel() {
    private val _state = MutableStateFlow(FloatingState())

    val state = _state.asStateFlow()

    fun showDialog() {
        _state.update {
            it.copy(isDialogVisible = true)
        }
    }

    fun dismissDialog() {
        _state.update {
            it.copy(isDialogVisible = false)
        }
    }

    fun onTextUpdate(newText: String) {
        _state.update {
            it.copy(text = newText)
        }
    }
}
