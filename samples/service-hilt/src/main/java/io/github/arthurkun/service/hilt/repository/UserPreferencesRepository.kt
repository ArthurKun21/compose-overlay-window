package io.github.arthurkun.service.hilt.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val LOCATION_X = intPreferencesKey("location_x")
        private val LOCATION_Y = intPreferencesKey("location_y")
    }

    val darkModeFlow: Flow<Boolean> = dataStore
        .data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    val locationFlow: Flow<Pair<Int, Int>> = dataStore
        .data
        .map { preferences ->
            val x = preferences[LOCATION_X] ?: 0
            val y = preferences[LOCATION_Y] ?: 0
            Pair(x, y)
        }

    suspend fun setLocation(x: Int, y: Int) {
        dataStore.edit { preferences ->
            preferences[LOCATION_X] = x
            preferences[LOCATION_Y] = y
        }
    }
}
