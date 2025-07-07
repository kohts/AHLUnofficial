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
 * A composable that applies zoom and pan transformations to its content
 * Content stays within its original bounds - no panning outside the container
 * @param modifier Modifier to be applied to the container
 * @param currentScale Current scale factor from external state
 * @param transformOrigin Transform origin for zoom operations
 * @param panOffsetX Horizontal pan offset
 * @param panOffsetY Vertical pan offset
 * @param onTap Optional callback for tap gestures
 * @param content The content to make zoomable
 */
@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    currentScale: Float = 1f,
    transformOrigin: TransformOrigin = TransformOrigin(1f, 0f),
    panOffsetX: Float = 0f,
    panOffsetY: Float = 0f,
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
                    translationX = panOffsetX,
                    translationY = panOffsetY,
                    transformOrigin = transformOrigin
                )
        ) {
            content()
        }
    }
} 
