package io.github.arthurkun.service.hilt.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@AndroidEntryPoint
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

    @Inject
    lateinit var serviceOverlay: ServiceOverlay

    override fun onCreate() {
        super.onCreate()
        _serviceStarted.update { true }
        serviceOverlay.show()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        _serviceStarted.update { false }
        // Call close for cleanup and it will hide it in the process
        serviceOverlay.close()
        super.onDestroy()
    }
}
