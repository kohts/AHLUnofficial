package com.unofficial.ahl.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.unofficial.ahl.api.AhlApi
import com.unofficial.ahl.api.NetworkModule
import com.unofficial.ahl.database.AppDatabase
import com.unofficial.ahl.model.HebrewWord
import com.unofficial.ahl.model.SearchCache
import com.unofficial.ahl.model.SearchHistory
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
    private val searchHistoryDao = AppDatabase.getDatabase(context).searchHistoryDao()
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
     * Add an entry to the search history
     * @param searchTerm The search term to log in history
     */
    private suspend fun addToSearchHistory(searchTerm: String) {
        // Only add if the term doesn't already exist or if it's not empty
        if (searchTerm.isNotBlank()) {
            val exists = searchHistoryDao.searchTermExists(searchTerm)
            if (!exists) {
                // Add to search history
                val searchHistory = SearchHistory(
                    searchTerm = searchTerm,
                    timestamp = Date()
                )
                searchHistoryDao.insertSearchHistory(searchHistory)
                
                // Clean up old entries to keep only the most recent ones
                searchHistoryDao.deleteOldSearches(10)
            } else {
                // Just update the timestamp of the existing entry by removing and re-adding
                val searchHistory = SearchHistory(
                    searchTerm = searchTerm,
                    timestamp = Date()
                )
                searchHistoryDao.insertSearchHistory(searchHistory)
            }
        }
    }
    
    /**
     * Get the search history
     * @param limit The maximum number of entries to return
     * @return Flow of search history entries, ordered by timestamp (newest first)
     */
    fun getSearchHistory(limit: Int = 10) = searchHistoryDao.getRecentSearches(limit)
    
    /**
     * Clear the search history
     */
    suspend fun clearSearchHistory() = searchHistoryDao.clearSearchHistory()
    
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
            var apiError: Exception? = null
            var errorMessage: String? = null
            var statusCode: Int? = null
            
            // Always check cache first, regardless of forceRefresh, to have a fallback
            val cachedResult = searchCacheDao.getCachedSearch(normalizedQuery)
            val cachedWords = cachedResult?.let { 
                val words = parseJsonToHebrewWords(it.apiResponse)
                filterValidWords(words)
            } ?: emptyList()
            
            // If we have valid cache data and not forcing refresh, return it immediately
            if (!forceRefresh && cachedWords.isNotEmpty()) {
                // Add to search history when retrieved from cache
                addToSearchHistory(normalizedQuery)
                return@withContext ApiResult.Success(cachedWords)
            }
            
            // If forcing refresh or no valid cache, try the API
            try {
                val apiResult = api.searchWords(normalizedQuery)
                
                // Filter out invalid entries
                val validWords = filterValidWords(apiResult)
                
                // Check if we have any valid results
                if (validWords.isEmpty() && apiResult.isNotEmpty()) {
                    // We have results but none are valid
                    errorMessage = "Invalid data format returned from API"
                    apiError = InvalidDataException(errorMessage)
                    
                    // If we have cached results, use them instead
                    if (cachedWords.isNotEmpty()) {
                        addToSearchHistory(normalizedQuery)
                        return@withContext ApiResult.Success(cachedWords)
                    }
                    
                    return@withContext ApiResult.Error(
                        exception = apiError,
                        message = errorMessage
                    )
                }
                
                // If successful, cache the result
                if (apiResult.isNotEmpty()) {
                    val jsonResponse = gson.toJson(apiResult)
                    val cacheEntry = SearchCache(
                        searchTerm = normalizedQuery,
                        apiResponse = jsonResponse,
                        timestamp = Date()
                    )
                    searchCacheDao.insertCache(cacheEntry)
                    
                    // Add to search history when API call is successful
                    addToSearchHistory(normalizedQuery)
                }
                
                ApiResult.Success(validWords)
            } catch (e: Exception) {
                // Log the error
                e.printStackTrace()
                
                // Extract HTTP status code if available
                statusCode = when (e) {
                    is HttpException -> e.code()
                    else -> null
                }
                
                // Generate a meaningful error message
                errorMessage = when (e) {
                    is HttpException -> "Server error: ${e.message()} (${e.code()})"
                    is IOException -> "Network error: ${e.message}"
                    else -> "Unknown error: ${e.message}"
                }
                
                apiError = e
                
                // Use cached data if available
                if (cachedWords.isNotEmpty()) {
                    // Add to search history when using cached fallback
                    addToSearchHistory(normalizedQuery)
                    return@withContext ApiResult.Success(cachedWords)
                }
                
                ApiResult.Error(apiError, errorMessage, statusCode)
            }
        }
    }
    
    /**
     * Fetch detailed information about a Hebrew word from the Hebrew Academy website
     * @param keyword The keyword from the HebrewWord object
     * @return ApiResult with either the HTML content or error details
     */
    suspend fun fetchWordDetails(keyword: String?): ApiResult<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate the keyword is not null
                if (keyword.isNullOrBlank()) {
                    return@withContext ApiResult.Error(
                        exception = InvalidDataException("Keyword is null or blank"),
                        message = "No valid keyword to fetch details"
                    )
                }
                
                // Construct the URL for the word details page
                val detailsUrl = "https://hebrew-academy.org.il/keyword/$keyword"
                
                // Fetch the HTML content
                val response = api.fetchHtmlContent(detailsUrl)
                val htmlContent = response.string()
                
                // Extract the relevant content using the HTML parser
                val extractedContent = HtmlParser.extractContent(htmlContent)
                
                if (extractedContent.isBlank()) {
                    return@withContext ApiResult.Error(
                        exception = InvalidDataException("No content found"),
                        message = "No relevant content found on the details page"
                    )
                }
                
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
     * Filter out invalid word entries
     * @param words List of words to filter
     * @return List of valid words only
     */
    private fun filterValidWords(words: List<HebrewWord>): List<HebrewWord> {
        return words.filter { it.hasValidContent() }
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
    
    /**
     * Custom exception for invalid data format
     */
    class InvalidDataException(message: String) : Exception(message)
} 