package io.github.arthurkun.service.metro.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import io.github.arthurkun.service.metro.FloatingApplication
import io.github.arthurkun.service.metro.di.ServiceGraph
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

    private var serviceGraph: ServiceGraph? = null
    private val serviceOverlay: ServiceOverlay
        get() = serviceGraph!!.serviceOverlay

    override fun onCreate() {
        super.onCreate()
        serviceGraph = FloatingApplication.instance.appGraph.serviceGraphFactory.create()
        _serviceStarted.update { true }
        serviceOverlay.show()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        _serviceStarted.update { false }
        // Call close for cleanup and it will hide it in the process
        serviceOverlay.close()
        serviceGraph = null
        super.onDestroy()
    }
}
