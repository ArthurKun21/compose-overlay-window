package io.github.arthurkun.service.metro.di

import dev.zacsweers.metro.Scope

/**
 * Service-level scope for dependencies that live for the duration of a Service.
 */
@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class ServiceScope
