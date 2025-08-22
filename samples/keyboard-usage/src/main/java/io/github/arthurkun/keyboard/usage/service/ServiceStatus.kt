package io.github.arthurkun.keyboard.usage.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object ServiceStatus {

    private var _serviceStarted = MutableStateFlow(false)
    val serviceStarted: StateFlow<Boolean>
        get() = _serviceStarted.asStateFlow()

    fun setServiceStatus(status: Boolean) {
        _serviceStarted.update { status }
    }
}
