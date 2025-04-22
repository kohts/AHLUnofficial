package com.unofficial.ahl.api

import com.unofficial.ahl.model.HebrewWord
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API interface for the Academy of Hebrew Language API
 */
interface AhlApi {
    /**
     * Search for Hebrew words
     * @param searchString The search query
     * @return A list of Hebrew word results
     */
    @GET("Ac/")
    suspend fun searchWords(
        @Query("SearchString") searchString: String
    ): List<HebrewWord>
} 