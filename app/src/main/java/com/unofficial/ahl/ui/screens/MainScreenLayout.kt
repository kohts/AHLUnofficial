package com.unofficial.ahl.ui.screens

import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
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
import androidx.compose.ui.geometry.Offset
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
    onZoomChange: (Float, Offset?, Offset?) -> Unit,
    currentZoomScale: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Collect states from the view model
    val errorMessage by errorViewModel.errorMessage.collectAsState()
    val showErrorDetails by errorViewModel.showErrorDetails.collectAsState()
    
    Box(
        modifier = modifier
            .pointerInput("global_zoom_and_pan_gestures") {
                awaitEachGesture {
                    // Wait for first finger down - don't require unconsumed
                    val firstDown = awaitFirstDown(requireUnconsumed = false)
                    
                    var isMultiFingerGesture = false
                    var lastCentroid: Offset? = null
                    
                    // Initialize lastCentroid with the first touch position
                    // This is crucial for single-finger panning to work
                    lastCentroid = firstDown.position
                    
                    Log.d("ZoomPan", "Gesture started at: ${firstDown.position}, currentZoomScale: $currentZoomScale")
                    
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val fingerCount = event.changes.size
                        
                        Log.d("ZoomPan", "Fingers: $fingerCount, isMultiFinger: $isMultiFingerGesture, zoomScale: $currentZoomScale")
                        
                        // Check if we have multiple fingers for zoom
                        if (event.changes.size >= 2) {
                            isMultiFingerGesture = true
                            val currentCentroid = event.calculateCentroid()
                            val zoomChange = event.calculateZoom()
                            
                            // Calculate pan delta if we have a previous centroid
                            val panDelta = lastCentroid?.let { last ->
                                Offset(
                                    x = currentCentroid.x - last.x,
                                    y = currentCentroid.y - last.y
                                )
                            }
                            
                            Log.d("ZoomPan", "Multi-finger: zoom=$zoomChange, panDelta=$panDelta")
                            
                            // Apply zoom and/or pan if there's significant change
                            if (abs(zoomChange - 1f) > 0.02f || panDelta != null) {
                                onZoomChange(zoomChange, currentCentroid, panDelta)
                            }
                            
                            lastCentroid = currentCentroid
                            
                            // Consume events to prevent other gestures from interfering
                            event.changes.forEach { it.consume() }
                        } else if (event.changes.size == 1) {
                            // Single finger - check if we should pan (when zoomed in)
                            val currentPos = event.changes[0].position
                            
                            Log.d("ZoomPan", "Single finger at: $currentPos, lastCentroid: $lastCentroid")
                            
                            // Allow single finger panning when zoomed in OR after multi-finger gesture
                            if (currentZoomScale > 1f || isMultiFingerGesture) {
                                val panDelta = lastCentroid?.let { last ->
                                    Offset(
                                        x = currentPos.x - last.x,
                                        y = currentPos.y - last.y
                                    )
                                }
                                
                                Log.d("ZoomPan", "Should pan: panDelta=$panDelta, threshold check: ${panDelta?.let { abs(it.x) > 2f || abs(it.y) > 2f }}")
                                
                                if (panDelta != null && (abs(panDelta.x) > 2f || abs(panDelta.y) > 2f)) {
                                    // Only pan if there's significant movement
                                    Log.d("ZoomPan", "PANNING! Delta: $panDelta")
                                    onZoomChange(1f, null, panDelta)
                                }
                                
                                lastCentroid = currentPos
                                
                                // Consume events to prevent scrolling when panning
                                Log.d("ZoomPan", "Consuming single finger events for panning")
                                event.changes.forEach { it.consume() }
                            } else {
                                // Not zoomed in and not multi-finger - let other components handle
                                Log.d("ZoomPan", "Not panning - letting other components handle")
                                // Update lastCentroid even when not panning for smoother transitions
                                lastCentroid = currentPos
                            }
                        }
                        
                    } while (event.changes.any { it.pressed })
                    
                    Log.d("ZoomPan", "Gesture ended")
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