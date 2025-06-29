package dev.abdl.tryfirst.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppSettings(private val dataStore: DataStore<Preferences>) {
    private object Keys {
        val FIRST_RUN_COMPLETED = booleanPreferencesKey("first_run_completed")
    }

    val isFirstRunFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.FIRST_RUN_COMPLETED] != true
    }

    suspend fun setFirstRunCompleted() {
        dataStore.edit { settings ->
            settings[Keys.FIRST_RUN_COMPLETED] = true
        }
    }
}