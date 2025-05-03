package com.unofficial.ahl.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manager for handling app preferences persistence using DataStore
 */
class PreferencesManager(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")
        
        // Keys for preferences
        private val LAST_SEARCH_QUERY = stringPreferencesKey("last_search_query")
        private val LAST_SEARCH_TIMESTAMP = stringPreferencesKey("last_search_timestamp")
    }
    
    /**
     * Get the last search query stored in preferences
     */
    val lastSearchQuery: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_SEARCH_QUERY] ?: ""
        }
    
    /**
     * Get the timestamp of the last search
     */
    val lastSearchTimestamp: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_SEARCH_TIMESTAMP] ?: ""
        }
    
    /**
     * Save the search query and current timestamp to preferences
     */
    suspend fun saveSearchQuery(query: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SEARCH_QUERY] = query
            preferences[LAST_SEARCH_TIMESTAMP] = System.currentTimeMillis().toString()
        }
    }
} 