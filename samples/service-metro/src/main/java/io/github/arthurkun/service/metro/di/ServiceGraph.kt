package io.github.arthurkun.service.metro.di

import android.content.Context
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.arthurkun.service.metro.repository.UserPreferencesRepository
import io.github.arthurkun.service.metro.service.ServiceOverlay

@GraphExtension(ServiceScope::class)
@SingleIn(ServiceScope::class)
interface ServiceGraph {

    val serviceOverlay: ServiceOverlay

    @SingleIn(ServiceScope::class)
    @Provides
    fun provideServiceOverlay(
        userPreferencesRepository: UserPreferencesRepository,
        context: Context,
    ): ServiceOverlay = ServiceOverlay(userPreferencesRepository, context)

    @GraphExtension.Factory
    interface Factory {
        fun create(): ServiceGraph
    }
}
