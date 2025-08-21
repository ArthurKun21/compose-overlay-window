package io.github.arthurkun.fullscreen.dialog.ui.floating

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FloatingViewModel : ViewModel() {
    private var _dialogVisible = MutableStateFlow(false)

    val dialogVisible
        get() = _dialogVisible.asStateFlow()

    fun showDialog() {
        _dialogVisible.update { true }
    }

    fun dismissDialog() {
        _dialogVisible.update { false }
    }
}
