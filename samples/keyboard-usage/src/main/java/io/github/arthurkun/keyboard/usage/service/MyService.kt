package io.github.arthurkun.keyboard.usage.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.github.only52607.compose.service.ComposeServiceFloatingWindow
import io.github.arthurkun.keyboard.usage.ui.floating.FloatingServiceScreen

class MyService : Service() {
    companion object {

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

    private fun createFloatingWindow(): ComposeServiceFloatingWindow =
        ComposeServiceFloatingWindow(this).apply {
            setContent {
                FloatingServiceScreen()
            }
        }

    override fun onCreate() {
        super.onCreate()
        ServiceStatus.setServiceStatus(true)
        floatingWindow.show()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        ServiceStatus.setServiceStatus(false)
        // Call close for cleanup and it will hide it in the process
        floatingWindow.close()
        super.onDestroy()
    }
}
