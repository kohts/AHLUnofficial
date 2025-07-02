package com.unofficial.ahl.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A composable that applies zoom transformations to its content
 * Content stays within its original bounds - no panning outside the container
 * @param modifier Modifier to be applied to the container
 * @param currentScale Current scale factor from external state
 * @param onTap Optional callback for tap gestures
 * @param content The content to make zoomable
 */
@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    currentScale: Float = 1f,
    onTap: ((Offset) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RectangleShape)
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
                    scaleX = currentScale,
                    scaleY = currentScale,
                    // No translation - content stays in original position
                    transformOrigin = TransformOrigin(1f, 0f)
                )
        ) {
            content()
        }
    }
} 
