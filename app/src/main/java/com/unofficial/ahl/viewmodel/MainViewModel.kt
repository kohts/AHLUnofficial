package com.unofficial.ahl.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unofficial.ahl.model.HebrewWord
import com.unofficial.ahl.repository.HebrewWordsRepository
import com.unofficial.ahl.repository.HebrewWordsRepository.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel for the main screen
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HebrewWordsRepository(application.applicationContext)
    
    // UI state
    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // API error tracking
    private val _apiError = MutableStateFlow<ApiError?>(null)
    val apiError: StateFlow<ApiError?> = _apiError.asStateFlow()
    
    // When ViewModel is created, clean old cache entries
    init {
        viewModelScope.launch {
            repository.cleanOldCache()
        }
    }
    
    /**
     * Update the search query
     * @param query The new search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
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
            when (val result = repository.searchWords(query, forceRefresh)) {
                is ApiResult.Success -> {
                    val words = result.data
                    _uiState.value = if (words.isEmpty()) {
                        UiState.NoResults
                    } else {
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
                    
                    _uiState.value = UiState.Error("Failed to load data. Using cached results if available.")
                }
            }
        }
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
} 