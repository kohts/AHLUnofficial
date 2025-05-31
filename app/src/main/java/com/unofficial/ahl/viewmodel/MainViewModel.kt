package com.unofficial.ahl.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unofficial.ahl.model.HebrewWord
import com.unofficial.ahl.model.SearchHistory
import com.unofficial.ahl.repository.HebrewWordsRepository
import com.unofficial.ahl.repository.HebrewWordsRepository.ApiResult
import com.unofficial.ahl.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel for the main screen
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HebrewWordsRepository(application.applicationContext)
    private val preferencesManager = PreferencesManager(application.applicationContext)
    
    // UI state
    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // Search history
    val searchHistory = repository.getSearchHistory()
    
    // Flag to show/hide search history
    private val _showSearchHistory = MutableStateFlow(false)
    val showSearchHistory: StateFlow<Boolean> = _showSearchHistory.asStateFlow()
    
    // API error tracking
    private val _apiError = MutableStateFlow<ApiError?>(null)
    val apiError: StateFlow<ApiError?> = _apiError.asStateFlow()
    
    // Invalid data flag - used to show a snackbar when API returns invalid data
    private val _invalidDataDetected = MutableStateFlow(false)
    val invalidDataDetected: StateFlow<Boolean> = _invalidDataDetected.asStateFlow()
    
    // Selected word details
    private val _wordDetailsState = MutableStateFlow<WordDetailsState>(WordDetailsState.Initial)
    val wordDetailsState: StateFlow<WordDetailsState> = _wordDetailsState.asStateFlow()
    
    // Selected word
    private val _selectedWord = MutableStateFlow<HebrewWord?>(null)
    val selectedWord: StateFlow<HebrewWord?> = _selectedWord.asStateFlow()
    
    // When ViewModel is created, load saved preferences and search results
    init {
        viewModelScope.launch {
            // Load saved search query
            val savedQuery = preferencesManager.lastSearchQuery.first()
            
            if (savedQuery.isNotEmpty()) {
                // Update the search query
                _searchQuery.value = savedQuery
                
                // Auto-execute the search to restore last results
                searchWords(forceRefresh = false)
            }
        }
    }
    
    /**
     * Update the search query and save it to preferences
     * @param query The new search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        
        // Save the query to preferences if not empty
        if (query.isNotEmpty()) {
            viewModelScope.launch {
                preferencesManager.saveSearchQuery(query)
            }
        }
    }
    
    /**
     * Search for Hebrew words
     * @param forceRefresh Whether to force a fresh API request
     */
    fun searchWords(forceRefresh: Boolean = false) {
        val query = _searchQuery.value
        if (query.isBlank()) return
        
        _uiState.value = UiState.Loading
        
        viewModelScope.launch {
            // Save the query to preferences
            preferencesManager.saveSearchQuery(query)
            
            when (val result = repository.searchWords(query, forceRefresh)) {
                is ApiResult.Success -> {
                    val words = result.data
                    _uiState.value = if (words.isEmpty()) {
                        UiState.NoResults
                    } else {
                        // Check if any words have invalid data (null titles)
                        val containsInvalidData = words.any { it.title.isNullOrBlank() }
                        if (containsInvalidData) {
                            _invalidDataDetected.value = true
                        }
                        
                        UiState.Success(words)
                    }
                }
                is ApiResult.Error -> {
                    // Track the API error
                    _apiError.value = ApiError(
                        message = result.message,
                        timestamp = Date(),
                        searchTerm = query,
                        statusCode = result.statusCode
                    )
                    
                    // Try again but explicitly use cache only
                    tryFallbackToCache(query)
                }
            }
        }
    }
    
    /**
     * Try to fetch results from cache as a fallback
     */
    private fun tryFallbackToCache(query: String) {
        viewModelScope.launch {
            try {
                // This is a more explicit way to try to get data from cache only
                val cachedResult = repository.searchWords(query, forceRefresh = false)
                
                when (cachedResult) {
                    is ApiResult.Success -> {
                        val words = cachedResult.data
                        if (words.isNotEmpty()) {
                            _uiState.value = UiState.Success(words)
                            return@launch
                        }
                    }
                    else -> { /* Cache fallback also failed */ }
                }
                
                // If we get here, even the cache fallback failed
                _uiState.value = UiState.Error("Failed to load data. No cached results available.")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load data. No cached results available.")
            }
        }
    }
    
    /**
     * Set visibility of search history
     * @param show Whether to show search history
     */
    fun setShowSearchHistory(show: Boolean) {
        _showSearchHistory.value = show
    }
    
    /**
     * Clear all search history
     */
    fun clearSearchHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }
    
    /**
     * Use a history item to perform a search
     * @param historyItem The search history item to use
     */
    fun searchWithHistoryItem(historyItem: SearchHistory) {
        // Update the search query
        _searchQuery.value = historyItem.searchTerm
        
        // Perform the search without adding to history (since it's already in history)
        _uiState.value = UiState.Loading
        
        viewModelScope.launch {
            // Save the query to preferences
            preferencesManager.saveSearchQuery(historyItem.searchTerm)
            
            when (val result = repository.searchWords(historyItem.searchTerm, forceRefresh = false, addToHistory = false)) {
                is ApiResult.Success -> {
                    val words = result.data
                    _uiState.value = if (words.isEmpty()) {
                        UiState.NoResults
                    } else {
                        // Check if any words have invalid data (null titles)
                        val containsInvalidData = words.any { it.title.isNullOrBlank() }
                        if (containsInvalidData) {
                            _invalidDataDetected.value = true
                        }
                        
                        UiState.Success(words)
                    }
                }
                is ApiResult.Error -> {
                    // Track the API error
                    _apiError.value = ApiError(
                        message = result.message,
                        timestamp = Date(),
                        searchTerm = historyItem.searchTerm,
                        statusCode = result.statusCode
                    )
                    
                    // Try again but explicitly use cache only (without adding to history)
                    tryFallbackToCacheWithoutHistory(historyItem.searchTerm)
                }
            }
        }
        
        // Hide the search history
        _showSearchHistory.value = false
    }
    
    /**
     * Try to fetch results from cache as a fallback without adding to history
     */
    private fun tryFallbackToCacheWithoutHistory(query: String) {
        viewModelScope.launch {
            try {
                // This is a more explicit way to try to get data from cache only without adding to history
                val cachedResult = repository.searchWords(query, forceRefresh = false, addToHistory = false)
                
                when (cachedResult) {
                    is ApiResult.Success -> {
                        val words = cachedResult.data
                        if (words.isNotEmpty()) {
                            _uiState.value = UiState.Success(words)
                            return@launch
                        }
                    }
                    else -> { /* Cache fallback also failed */ }
                }
                
                // If we get here, even the cache fallback failed
                _uiState.value = UiState.Error("Failed to load data. No cached results available.")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load data. No cached results available.")
            }
        }
    }
    
    /**
     * Clear the invalid data detection flag
     */
    fun clearInvalidDataFlag() {
        _invalidDataDetected.value = false
    }
    
    /**
     * Select a word and fetch its details
     * @param word The selected HebrewWord
     */
    fun selectWord(word: HebrewWord) {
        _selectedWord.value = word
        _wordDetailsState.value = WordDetailsState.Loading
        
        viewModelScope.launch {
            when (val result = repository.fetchWordDetails(word.keyword)) {
                is ApiResult.Success -> {
                    _wordDetailsState.value = WordDetailsState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _wordDetailsState.value = WordDetailsState.Error("Failed to load details: ${result.message}")
                    
                    // Also track as an API error
                    _apiError.value = ApiError(
                        message = result.message,
                        timestamp = Date(),
                        searchTerm = word.keyword ?: "",
                        statusCode = result.statusCode
                    )
                }
            }
        }
    }
    
    /**
     * Clear the current word selection
     */
    fun clearWordSelection() {
        _selectedWord.value = null
        _wordDetailsState.value = WordDetailsState.Initial
    }
    
    /**
     * Clear the current API error
     */
    fun clearApiError() {
        _apiError.value = null
    }
    
    /**
     * Reset the UI state
     */
    fun resetState() {
        _uiState.value = UiState.Initial
    }
    
    /**
     * API Error data class
     */
    data class ApiError(
        val message: String,
        val timestamp: Date,
        val searchTerm: String,
        val statusCode: Int? = null
    )
    
    /**
     * Sealed class representing the UI state
     */
    sealed class UiState {
        data object Initial : UiState()
        data object Loading : UiState()
        data class Success(val words: List<HebrewWord>) : UiState()
        data object NoResults : UiState()
        data class Error(val message: String) : UiState()
    }
    
    /**
     * Sealed class representing the word details state
     */
    sealed class WordDetailsState {
        data object Initial : WordDetailsState()
        data object Loading : WordDetailsState()
        data class Success(val detailsHtml: String) : WordDetailsState()
        data class Error(val message: String) : WordDetailsState()
    }
} 