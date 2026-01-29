package io.github.arthurkun.service.metro

import android.app.Application
import dev.zacsweers.metro.createGraphFactory
import io.github.arthurkun.service.metro.di.AppGraph

class FloatingApplication : Application() {

    val appGraph: AppGraph by lazy {
        createGraphFactory<AppGraph.Factory>().create(this)
    }

    companion object {
        lateinit var instance: FloatingApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
