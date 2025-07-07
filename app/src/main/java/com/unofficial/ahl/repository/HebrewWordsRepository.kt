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
import com.unofficial.ahl.model.DafMilaCache
import com.unofficial.ahl.model.DafMilaResponse
import com.unofficial.ahl.model.AhlDafMilaAjax
import com.unofficial.ahl.util.HtmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.concurrent.TimeUnit
import retrofit2.HttpException
import java.io.IOException

/**
 * Repository for Hebrew words data operations
 */
class HebrewWordsRepository private constructor(context: Context) {
    private val api: AhlApi = NetworkModule.provideAhlApi()
    private val searchCacheDao = AppDatabase.getDatabase(context).searchCacheDao()
    private val searchHistoryDao = AppDatabase.getDatabase(context).searchHistoryDao()
    private val dafMilaCacheDao = AppDatabase.getDatabase(context).dafMilaCacheDao()
    private val gson = Gson()
    
    /**
     * Last error from fetchDafMilaDetails operation for debugging
     */
    var lastFetchDafMilaDetailsError: DafMilaDetailsError? = null
        private set
    
    companion object {
        @Volatile
        private var INSTANCE: HebrewWordsRepository? = null
        
        fun getInstance(context: Context): HebrewWordsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HebrewWordsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
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
     * Detailed error information for fetchDafMilaDetails operations
     */
    data class DafMilaDetailsError(
        val exception: Exception,
        val message: String,
        val timestamp: Date,
        val keyword: String,
        val statusCode: Int? = null,
        val htmlResponse: String? = null
    )
    
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
                searchHistoryDao.deleteOldSearches(100)
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
     * @return Flow of search history entries, ordered by timestamp (newest first), with consecutive duplicates filtered out
     */
    fun getSearchHistory(limit: Int = 100) = searchHistoryDao.getRecentSearches(limit)
        .map { historyList ->
            filterConsecutiveDuplicates(historyList)
        }
    
    /**
     * Filter out consecutive duplicate search terms, keeping only the earliest occurrence of each consecutive group
     * @param historyList List of search history entries ordered by timestamp DESC (newest first)
     * @return Filtered list with consecutive duplicates removed
     */
    private fun filterConsecutiveDuplicates(historyList: List<SearchHistory>): List<SearchHistory> {
        if (historyList.isEmpty()) return historyList
        
        val filtered = mutableListOf<SearchHistory>()
        var i = 0
        
        while (i < historyList.size) {
            val currentTerm = historyList[i].searchTerm
            var j = i
            
            // Find the end of this consecutive group of same search terms
            while (j < historyList.size && historyList[j].searchTerm == currentTerm) {
                j++
            }
            
            // Add the last item of this group (which has the earliest timestamp 
            // since the list is ordered newest first)
            filtered.add(historyList[j - 1])
            
            // Move to the next group
            i = j
        }
        
        return filtered
    }
    
    /**
     * Clear the search history
     */
    suspend fun clearSearchHistory() = searchHistoryDao.clearSearchHistory()
    
    /**
     * Search for Hebrew words matching the query
     * @param query The search query
     * @param forceRefresh Whether to force a fresh API request
     * @param addToHistory Whether to add this search to the search history
     * @return ApiResult with either the data or error details
     */
    suspend fun searchWords(query: String, forceRefresh: Boolean = false, addToHistory: Boolean = true): ApiResult<List<HebrewWord>> {
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
                // Add to search history when retrieved from cache only if requested
                if (addToHistory) {
                    addToSearchHistory(normalizedQuery)
                }
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
                        if (addToHistory) {
                            addToSearchHistory(normalizedQuery)
                        }
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
                    
                    // Add to search history when API call is successful only if requested
                    if (addToHistory) {
                        addToSearchHistory(normalizedQuery)
                    }
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
                    // Add to search history when using cached fallback only if requested
                    if (addToHistory) {
                        addToSearchHistory(normalizedQuery)
                    }
                    return@withContext ApiResult.Success(cachedWords)
                }
                
                ApiResult.Error(apiError, errorMessage, statusCode)
            }
        }
    }
    
    /**
     * Fetch detailed word data using the Hebrew Academy AJAX API (4-step process)
     * @param keyword The Hebrew word keyword to fetch details for
     * @param forceRefresh Whether to force a fresh API request, bypassing cache
     * @return ApiResult with either DafMilaResponse data or error details
     */
    suspend fun fetchDafMilaDetails(keyword: String, forceRefresh: Boolean = false): ApiResult<DafMilaResponse> {
        return withContext(Dispatchers.IO) {
            var htmlContent: String? = null
            
            try {
                // Validate input
                if (keyword.isBlank()) {
                    val exception = InvalidDataException("Keyword is blank")
                    val errorMessage = "No valid keyword provided"
                    lastFetchDafMilaDetailsError = DafMilaDetailsError(
                        exception = exception,
                        message = errorMessage,
                        timestamp = Date(),
                        keyword = keyword,
                        statusCode = null,
                        htmlResponse = null
                    )
                    return@withContext ApiResult.Error(
                        exception = exception,
                        message = errorMessage
                    )
                }
                
                // Check cache first if not forcing refresh
                if (!forceRefresh) {
                    val cachedResult = dafMilaCacheDao.getCachedWordDetails(keyword)
                    if (cachedResult != null) {
                        val cachedData = parseDafMilaJson(cachedResult.apiResponse)
                        if (cachedData != null) {
                            return@withContext ApiResult.Success(cachedData)
                        }
                    }
                }
                
                // Step 1: Build keyword detail page URL and fetch HTML
                val keywordUrl = HtmlParser.buildKeywordDetailUrl(keyword)
                val htmlResponse = api.fetchHtmlContent(keywordUrl)
                htmlContent = htmlResponse.string()
                
                // Step 2: Extract ahl_daf_mila_ajax JavaScript variable
                val ajaxConfig = HtmlParser.extractAhlDafMilaAjax(htmlContent)
                if (ajaxConfig == null) {
                    val exception = InvalidDataException("AJAX config not found")
                    val errorMessage = "Could not extract AJAX configuration from HTML"
                    lastFetchDafMilaDetailsError = DafMilaDetailsError(
                        exception = exception,
                        message = errorMessage,
                        timestamp = Date(),
                        keyword = keyword,
                        statusCode = null,
                        htmlResponse = htmlContent
                    )
                    return@withContext ApiResult.Error(
                        exception = exception,
                        message = errorMessage
                    )
                }
                
                // Step 3: Build AJAX URL and make request
                val ajaxUrl = HtmlParser.buildAjaxUrl(ajaxConfig, keyword)
                val ajaxResponse = api.fetchDafMilaAjax(ajaxUrl)
                
                // Validate AJAX response
                if (!ajaxResponse.success || ajaxResponse.data.isNullOrBlank()) {
                    val exception = InvalidDataException("AJAX request failed")
                    val errorMessage = "AJAX request returned unsuccessful response or no data"
                    lastFetchDafMilaDetailsError = DafMilaDetailsError(
                        exception = exception,
                        message = errorMessage,
                        timestamp = Date(),
                        keyword = keyword,
                        statusCode = null,
                        htmlResponse = htmlContent
                    )
                    return@withContext ApiResult.Error(
                        exception = exception,
                        message = errorMessage
                    )
                }
                
                // Step 4: Parse the nested JSON data
                val dafMilaData = parseDafMilaJson(ajaxResponse.data)
                if (dafMilaData == null) {
                    val exception = InvalidDataException("Failed to parse response data")
                    val errorMessage = "Could not parse the detailed word data from response"
                    lastFetchDafMilaDetailsError = DafMilaDetailsError(
                        exception = exception,
                        message = errorMessage,
                        timestamp = Date(),
                        keyword = keyword,
                        statusCode = null,
                        htmlResponse = htmlContent
                    )
                    return@withContext ApiResult.Error(
                        exception = exception,
                        message = errorMessage
                    )
                }
                
                // Cache the successful result
                val cacheEntry = DafMilaCache(
                    keyword = keyword,
                    apiResponse = ajaxResponse.data,
                    timestamp = Date()
                )
                dafMilaCacheDao.insertCache(cacheEntry)
                
                ApiResult.Success(dafMilaData)
                
            } catch (e: HttpException) {
                val errorMessage = "HTTP error: ${e.message()} (${e.code()})"
                lastFetchDafMilaDetailsError = DafMilaDetailsError(
                    exception = e,
                    message = errorMessage,
                    timestamp = Date(),
                    keyword = keyword,
                    statusCode = e.code(),
                    htmlResponse = htmlContent
                )
                
                // Try to use cached data as fallback
                val cachedResult = dafMilaCacheDao.getCachedWordDetails(keyword)
                if (cachedResult != null) {
                    val cachedData = parseDafMilaJson(cachedResult.apiResponse)
                    if (cachedData != null) {
                        return@withContext ApiResult.Success(cachedData)
                    }
                }
                
                ApiResult.Error(
                    exception = e,
                    message = errorMessage,
                    statusCode = e.code()
                )
            } catch (e: IOException) {
                val errorMessage = "Network error: ${e.message}"
                lastFetchDafMilaDetailsError = DafMilaDetailsError(
                    exception = e,
                    message = errorMessage,
                    timestamp = Date(),
                    keyword = keyword,
                    statusCode = null,
                    htmlResponse = htmlContent
                )
                
                // Try to use cached data as fallback
                val cachedResult = dafMilaCacheDao.getCachedWordDetails(keyword)
                if (cachedResult != null) {
                    val cachedData = parseDafMilaJson(cachedResult.apiResponse)
                    if (cachedData != null) {
                        return@withContext ApiResult.Success(cachedData)
                    }
                }
                
                ApiResult.Error(
                    exception = e,
                    message = errorMessage
                )
            } catch (e: Exception) {
                e.printStackTrace()
                val errorMessage = "Unexpected error: ${e.message}"
                lastFetchDafMilaDetailsError = DafMilaDetailsError(
                    exception = e,
                    message = errorMessage,
                    timestamp = Date(),
                    keyword = keyword,
                    statusCode = null,
                    htmlResponse = htmlContent
                )
                
                // Try to use cached data as fallback
                val cachedResult = dafMilaCacheDao.getCachedWordDetails(keyword)
                if (cachedResult != null) {
                    val cachedData = parseDafMilaJson(cachedResult.apiResponse)
                    if (cachedData != null) {
                        return@withContext ApiResult.Success(cachedData)
                    }
                }
                
                ApiResult.Error(
                    exception = e,
                    message = errorMessage
                )
            }
        }
    }
    
    /**
     * Clear the last DafMila details error
     */
    fun clearDafMilaDetailsError() {
        lastFetchDafMilaDetailsError = null
    }
    
    /**
     * Clean old DafMila cache entries
     * @param maxAgeInDays Maximum age in days for cache entries
     */
    suspend fun cleanOldDafMilaCache(maxAgeInDays: Int = 30) {
        withContext(Dispatchers.IO) {
            val cutoffTime = Date().time - TimeUnit.DAYS.toMillis(maxAgeInDays.toLong())
            dafMilaCacheDao.deleteOldEntries(cutoffTime)
        }
    }
    
    /**
     * Parse JSON string into DafMilaResponse object
     * @param json The JSON string to parse
     * @return Parsed DafMilaResponse or null if parsing fails
     */
    private fun parseDafMilaJson(json: String): DafMilaResponse? {
        return try {
            gson.fromJson(json, DafMilaResponse::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
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