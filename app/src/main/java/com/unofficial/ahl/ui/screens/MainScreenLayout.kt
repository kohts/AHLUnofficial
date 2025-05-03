package com.unofficial.ahl.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.unofficial.ahl.ui.components.ErrorBanner
import com.unofficial.ahl.ui.components.ErrorDetailsView
import com.unofficial.ahl.viewmodel.ErrorViewModel

/**
 * Main screen layout with error handling components
 */
@Composable
fun MainScreenLayout(
    errorViewModel: ErrorViewModel,
    content: @Composable () -> Unit
) {
    // Collect states from the view model
    val errorMessage by errorViewModel.errorMessage.collectAsState()
    val showErrorDetails by errorViewModel.showErrorDetails.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Error banner at the top of the screen above everything else
        errorMessage?.let { message ->
            ErrorBanner(
                errorMessage = message,
                onDismiss = { errorViewModel.dismissError() },
                onShowDetails = { errorViewModel.showErrorDetails() },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(10f)
            )
        }
        
        // The main content with scaffold
        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                content()
            }
        }
        
        // Error details overlay - on top of everything
        if (showErrorDetails) {
            val lastErrorSession = errorViewModel.getLastErrorSession()
            lastErrorSession?.let { session ->
                ErrorDetailsView(
                    session = session,
                    onClose = { errorViewModel.hideErrorDetails() },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(20f)
                )
            }
        }
    }
} 