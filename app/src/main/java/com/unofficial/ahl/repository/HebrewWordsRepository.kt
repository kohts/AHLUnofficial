package com.unofficial.ahl.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.unofficial.ahl.api.AhlApi
import com.unofficial.ahl.api.NetworkModule
import com.unofficial.ahl.database.AppDatabase
import com.unofficial.ahl.model.HebrewWord
import com.unofficial.ahl.model.SearchCache
import com.unofficial.ahl.util.HtmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.concurrent.TimeUnit
import retrofit2.HttpException
import java.io.IOException

/**
 * Repository for Hebrew words data operations
 */
class HebrewWordsRepository(context: Context) {
    private val api: AhlApi = NetworkModule.provideAhlApi()
    private val searchCacheDao = AppDatabase.getDatabase(context).searchCacheDao()
    private val gson = Gson()
    
    /**
     * Result class that wraps successful and error responses
     */
    sealed class ApiResult<out T> {
        data class Success<T>(val data: T) : ApiResult<T>()
        data class Error(
            val exception: Exception,
            val message: String,
            val statusCode: Int? = null
        ) : ApiResult<Nothing>()
    }
    
    /**
     * Search for Hebrew words matching the query
     * @param query The search query
     * @param forceRefresh Whether to force a fresh API request
     * @return ApiResult with either the data or error details
     */
    suspend fun searchWords(query: String, forceRefresh: Boolean = false): ApiResult<List<HebrewWord>> {
        return withContext(Dispatchers.IO) {
            // Normalize the search term
            val normalizedQuery = normalizeSearchTerm(query)
            
            if (!forceRefresh) {
                // Try to get from cache first
                val cachedResult = searchCacheDao.getCachedSearch(normalizedQuery)
                
                if (cachedResult != null) {
                    // Convert the cached JSON back to a list of HebrewWord objects
                    return@withContext ApiResult.Success(parseJsonToHebrewWords(cachedResult.apiResponse))
                }
            }
            
            // If cache miss or force refresh, try the API
            try {
                val apiResult = api.searchWords(normalizedQuery)
                
                // If successful, cache the result
                if (apiResult.isNotEmpty()) {
                    val jsonResponse = gson.toJson(apiResult)
                    val cacheEntry = SearchCache(
                        searchTerm = normalizedQuery,
                        apiResponse = jsonResponse,
                        timestamp = Date()
                    )
                    searchCacheDao.insertCache(cacheEntry)
                }
                
                ApiResult.Success(apiResult)
            } catch (e: Exception) {
                // Log the error
                e.printStackTrace()
                
                // Extract HTTP status code if available
                val statusCode = when (e) {
                    is HttpException -> e.code()
                    else -> null
                }
                
                // Generate a meaningful error message
                val errorMessage = when (e) {
                    is HttpException -> "Server error: ${e.message()} (${e.code()})"
                    is IOException -> "Network error: ${e.message}"
                    else -> "Unknown error: ${e.message}"
                }
                
                // Try one last time to get from cache even if forceRefresh was true
                if (forceRefresh) {
                    val lastResortCache = searchCacheDao.getCachedSearch(normalizedQuery)
                    if (lastResortCache != null) {
                        return@withContext ApiResult.Success(parseJsonToHebrewWords(lastResortCache.apiResponse))
                    }
                }
                
                ApiResult.Error(e, errorMessage, statusCode)
            }
        }
    }
    
    /**
     * Fetch detailed information about a Hebrew word from the Hebrew Academy website
     * @param keyword The keyword from the HebrewWord object
     * @return ApiResult with either the HTML content or error details
     */
    suspend fun fetchWordDetails(keyword: String): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Construct the URL for the word details page
                val detailsUrl = "https://hebrew-academy.org.il/keyword/$keyword"
                
                // Fetch the HTML content
                val response = api.fetchHtmlContent(detailsUrl)
                val htmlContent = response.string()
                
                // Extract the relevant content using the HTML parser
                val extractedContent = HtmlParser.extractContent(htmlContent)
                
                ApiResult.Success(extractedContent)
            } catch (e: Exception) {
                // Log the error
                e.printStackTrace()
                
                // Extract HTTP status code if available
                val statusCode = when (e) {
                    is HttpException -> e.code()
                    else -> null
                }
                
                // Generate a meaningful error message
                val errorMessage = when (e) {
                    is HttpException -> "Server error: ${e.message()} (${e.code()})"
                    is IOException -> "Network error: ${e.message}"
                    else -> "Unknown error: ${e.message}"
                }
                
                ApiResult.Error(e, errorMessage, statusCode)
            }
        }
    }

    
    /**
     * Normalize the search term (trim, lowercase, etc.)
     */
    private fun normalizeSearchTerm(query: String): String {
        return query.trim()
    }
    
    /**
     * Parse a JSON string into a list of HebrewWord objects
     */
    private fun parseJsonToHebrewWords(json: String): List<HebrewWord> {
        val type = object : TypeToken<List<HebrewWord>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
} 