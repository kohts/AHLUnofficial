package com.unofficial.ahl.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A composable that makes its content zoomable via pinch gestures
 * Content stays within its original bounds - no panning outside the container
 * @param modifier Modifier to be applied to the container
 * @param minScale Minimum scale factor (default 1.0x - original size)
 * @param maxScale Maximum scale factor (default 3.0x)
 * @param onTap Optional callback for tap gestures
 * @param content The content to make zoomable
 */
@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    minScale: Float = 1.0f,
    maxScale: Float = 3.0f,
    onTap: ((Offset) -> Unit)? = null,
    content: @Composable (onZoomChange: (Float) -> Unit, currentScale: Float) -> Unit
) {
    // State for current scale - no offset to keep content in place
    var scale by remember { mutableStateOf(1f) }
    
    // Callback to handle zoom changes from child components
    val handleZoomChange: (Float) -> Unit = { zoomChange ->
        scale = (scale * zoomChange).coerceIn(minScale, maxScale)
    }

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .pointerInput("zoom_gestures") {
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
                                handleZoomChange(zoomChange)
                            }
                            
                            // Consume events to prevent clickable from triggering
                            event.changes.forEach { it.consume() }
                        } else if (!isMultiFingerGesture) {
                            // Single finger - don't consume, let clickable handle it
                            // Do nothing, let the event pass through
                        }
                        
                    } while (event.changes.any { it.pressed })
                }
            }
            .then(
                if (onTap != null) {
                    Modifier.pointerInput("tap_gestures") {
                        detectTapGestures(onTap = onTap)
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    // No translation - content stays in original position
                    transformOrigin = TransformOrigin(1f, 0f)
                )
        ) {
            content(handleZoomChange, scale)
        }
    }
} 
