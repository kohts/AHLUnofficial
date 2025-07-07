package com.unofficial.ahl.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unofficial.ahl.api.DetailedSession
import com.unofficial.ahl.api.NetworkModule
import com.unofficial.ahl.repository.HebrewWordsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * ViewModel for managing error states and displaying error details
 */
class ErrorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HebrewWordsRepository.getInstance(application.applicationContext)
    // Error message to display in the banner
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Whether to show the error details view
    private val _showErrorDetails = MutableStateFlow(false)
    val showErrorDetails: StateFlow<Boolean> = _showErrorDetails.asStateFlow()
    
    init {
        // Collect error session from NetworkModule
        viewModelScope.launch {
            NetworkModule.lastErrorSession.collect { session ->
                if (session != null) {
                    // Check if we have a more recent DafMila error
                    val dafMilaError = repository.lastFetchDafMilaDetailsError
                    val mostRecentError = getMostRecentError()
                    
                    if (mostRecentError != null) {
                        _errorMessage.value = mostRecentError
                    }
                }
            }
        }
    }
    
    /**
     * Sets the error message to display in the banner
     * This will be auto-dismissed after a delay
     */
    fun setErrorMessage(message: String) {
        _errorMessage.value = message
        
        // Auto-dismiss after a delay
        viewModelScope.launch {
            delay(7000)
            _errorMessage.value = null
        }
    }
    
    /**
     * Updates the error message with the most recent error from any source
     * Should be called when new errors occur
     */
    fun updateErrorFromAllSources() {
        val mostRecentError = getMostRecentError()
        if (mostRecentError != null) {
            _errorMessage.value = mostRecentError
            
            // Auto-dismiss after a delay
            viewModelScope.launch {
                delay(7000)
                _errorMessage.value = null
            }
        }
    }
    
    /**
     * Shows the error details panel
     */
    fun showErrorDetails() {
        _showErrorDetails.value = true
    }
    
    /**
     * Hides the error details panel and clears all errors
     */
    fun hideErrorDetails() {
        _showErrorDetails.value = false
        // Clear both error sources when user closes error details
        NetworkModule.clearLastErrorSession()
        repository.clearDafMilaDetailsError()
    }
    
    /**
     * Dismisses the error banner
     */
    fun dismissError() {
        _errorMessage.value = null
        NetworkModule.clearLastErrorSession()
        repository.clearDafMilaDetailsError()
    }
    
    /**
     * Gets the most recent error message from any source
     */
    private fun getMostRecentError(): String? {
        val networkError = NetworkModule.lastErrorSession.value
        val dafMilaError = repository.lastFetchDafMilaDetailsError
        
        return when {
            networkError == null && dafMilaError == null -> null
            networkError == null -> dafMilaError?.message
            dafMilaError == null -> networkError.errorMessage
            // Both exist - prioritize DafMila error since it has more detailed information
            else -> dafMilaError.message
        }
    }
    
    /**
     * Gets the last error session from NetworkModule (legacy method)
     */
    fun getLastErrorSession(): DetailedSession? {
        return NetworkModule.lastErrorSession.value
    }
    
    /**
     * Gets the most recent error details for display (either network or DafMila error)
     */
    fun getMostRecentErrorDetails(): Any? {
        val networkError = NetworkModule.lastErrorSession.value
        val dafMilaError = repository.lastFetchDafMilaDetailsError
        
        return when {
            networkError == null && dafMilaError == null -> null
            networkError == null -> dafMilaError
            dafMilaError == null -> networkError
            // Both exist - prioritize DafMila error since it has more detailed information
            else -> dafMilaError
        }
    }
} 