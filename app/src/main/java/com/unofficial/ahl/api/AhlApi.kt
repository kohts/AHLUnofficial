package com.unofficial.ahl.api

import com.unofficial.ahl.model.HebrewWord
import com.unofficial.ahl.model.DafMilaAjaxResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import okhttp3.ResponseBody

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
    
    /**
     * Fetch HTML content from a full URL
     * @param url The full URL to fetch
     * @return The HTML content
     */
    @GET
    suspend fun fetchHtmlContent(@Url url: String): ResponseBody
    
    /**
     * Fetch detailed word data via AJAX call
     * @param ajaxUrl The complete AJAX URL with query parameters
     * @return The AJAX response containing success flag and nested JSON data
     */
    @GET
    suspend fun fetchDafMilaAjax(@Url ajaxUrl: String): DafMilaAjaxResponse
} 