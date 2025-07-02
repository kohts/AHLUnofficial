package com.unofficial.ahl.ui.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.unofficial.ahl.ui.components.ErrorBanner
import com.unofficial.ahl.ui.components.ErrorDetailsView
import com.unofficial.ahl.viewmodel.ErrorViewModel
import kotlin.math.abs

/**
 * Main screen layout with error handling components and global zoom gesture detection
 */
@Composable
fun MainScreenLayout(
    errorViewModel: ErrorViewModel,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Collect states from the view model
    val errorMessage by errorViewModel.errorMessage.collectAsState()
    val showErrorDetails by errorViewModel.showErrorDetails.collectAsState()
    
    Box(
        modifier = modifier
            .pointerInput("global_zoom_gestures") {
                awaitEachGesture {
                    // Wait for first finger down - don't require unconsumed
                    awaitFirstDown(requireUnconsumed = false)
                    
                    var isMultiFingerGesture = false
                    
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        
                        // Check if we have multiple fingers
                        if (event.changes.size >= 2) {
                            isMultiFingerGesture = true
                            val zoomChange = event.calculateZoom()
                            
                            // Apply zoom if there's significant change
                            if (abs(zoomChange - 1f) > 0.02f) {
                                onZoomChange(zoomChange)
                            }
                            
                            // Consume events to prevent other gestures from interfering
                            event.changes.forEach { it.consume() }
                        } else if (!isMultiFingerGesture) {
                            // Single finger - don't consume, let other components handle it
                            // Do nothing, let the event pass through
                        }
                        
                    } while (event.changes.any { it.pressed })
                }
            }
    ) {
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