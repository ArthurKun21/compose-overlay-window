package com.github.only52607.compose.window.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.github.only52607.compose.window.ComposeFloatingWindow
import com.github.only52607.compose.window.service.ui.FloatingWindowContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MyService : Service() {
    companion object {
        private var _serviceStarted = MutableStateFlow(false)
        val serviceStarted: StateFlow<Boolean>
            get() = _serviceStarted.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, MyService::class.java)
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MyService::class.java)
            context.stopService(intent)
        }
    }

    private val floatingWindow by lazy {
        createFloatingWindow()
    }

    private fun createFloatingWindow(): ComposeFloatingWindow =
        ComposeFloatingWindow(this).apply {
            setContent {
                FloatingWindowContent()
            }
        }

    override fun onCreate() {
        super.onCreate()
        _serviceStarted.update { true }
        floatingWindow.show()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        _serviceStarted.update { false }
        // Call close for cleanup and it will hide it in the process
        floatingWindow.close()
        super.onDestroy()
    }
}