package com.unofficial.ahl.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * API interface for fetching HTML content from the Hebrew Academy website
 */
interface HtmlApi {
    /**
     * Get HTML content from a specific URL
     */
    @GET
    suspend fun getHtmlContent(@Url url: String): ResponseBody
} 