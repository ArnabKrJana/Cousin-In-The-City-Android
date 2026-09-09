package com.oneforth.cousininthecity.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val deviceIdKey = stringPreferencesKey("device_id")

    suspend fun getOrCreateDeviceId(): String {
        val existingId = context.dataStore.data.map { preferences ->
            preferences[deviceIdKey]
        }.first()

        if (!existingId.isNullOrBlank()) {
            return existingId
        }

        val newDeviceId = UUID.randomUUID().toString()
        context.dataStore.edit { preferences ->
            preferences[deviceIdKey] = newDeviceId
        }
        return newDeviceId
    }
}
