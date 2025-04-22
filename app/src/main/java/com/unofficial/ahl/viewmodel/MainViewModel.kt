package com.unofficial.ahl.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unofficial.ahl.model.HebrewWord
import com.unofficial.ahl.repository.HebrewWordsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the main screen
 */
class MainViewModel : ViewModel() {
    private val repository = HebrewWordsRepository()
    
    // UI state
    private val _uiState = MutableStateFlow<UiState>(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    /**
     * Update the search query
     * @param query The new search query
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Search for Hebrew words
     */
    fun searchWords() {
        val query = _searchQuery.value
        if (query.isBlank()) return
        
        _uiState.value = UiState.Loading
        
        viewModelScope.launch {
            try {
                val results = repository.searchWords(query)
                _uiState.value = if (results.isEmpty()) {
                    UiState.NoResults
                } else {
                    UiState.Success(results)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Reset the UI state
     */
    fun resetState() {
        _uiState.value = UiState.Initial
    }
    
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