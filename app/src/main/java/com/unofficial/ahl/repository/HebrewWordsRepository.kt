package com.unofficial.ahl.repository

import com.unofficial.ahl.api.AhlApi
import com.unofficial.ahl.api.NetworkModule
import com.unofficial.ahl.model.HebrewWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for Hebrew words data operations
 */
class HebrewWordsRepository {
    private val api: AhlApi = NetworkModule.provideAhlApi()
    
    /**
     * Search for Hebrew words matching the query
     * @param query The search query
     * @return A list of Hebrew words
     */
    suspend fun searchWords(query: String): List<HebrewWord> {
        return withContext(Dispatchers.IO) {
            try {
                api.searchWords(query)
            } catch (e: Exception) {
                // Log the error
                e.printStackTrace()
                emptyList()
            }
        }
    }
} 