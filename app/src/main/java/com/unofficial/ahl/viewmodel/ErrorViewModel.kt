package com.unofficial.ahl.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unofficial.ahl.api.DetailedSession
import com.unofficial.ahl.api.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * ViewModel for managing error states and displaying error details
 */
class ErrorViewModel : ViewModel() {
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
                    _errorMessage.value = session.errorMessage
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
            delay(5000)
            _errorMessage.value = null
        }
    }
    
    /**
     * Shows the error details panel
     */
    fun showErrorDetails() {
        _showErrorDetails.value = true
    }
    
    /**
     * Hides the error details panel
     */
    fun hideErrorDetails() {
        _showErrorDetails.value = false
    }
    
    /**
     * Dismisses the error banner
     */
    fun dismissError() {
        _errorMessage.value = null
        NetworkModule.clearLastErrorSession()
    }
    
    /**
     * Gets the last error session from NetworkModule
     */
    fun getLastErrorSession(): DetailedSession? {
        return NetworkModule.lastErrorSession.value
    }
} 