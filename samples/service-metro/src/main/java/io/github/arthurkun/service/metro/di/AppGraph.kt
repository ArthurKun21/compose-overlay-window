package io.github.arthurkun.service.metro.di

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.arthurkun.service.metro.module.dataStore
import io.github.arthurkun.service.metro.repository.UserPreferencesRepository

@DependencyGraph(AppScope::class)
interface AppGraph {

    val userPreferencesRepository: UserPreferencesRepository

    val serviceGraphFactory: ServiceGraph.Factory

    @Provides
    fun provideApplicationContext(application: Application): Context = application

    @SingleIn(AppScope::class)
    @Provides
    fun provideDataStore(context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AppGraph
    }
}
