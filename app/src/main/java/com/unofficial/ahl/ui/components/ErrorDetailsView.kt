package com.unofficial.ahl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.unofficial.ahl.api.DetailedSession

/**
 * Composable function to display HTTP error details in a scrollable text field
 */
@Composable
fun ErrorDetailsView(
    session: DetailedSession,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .zIndex(10f),
        elevation = 8.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Error Details",
                    style = MaterialTheme.typography.h6
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close error details"
                    )
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            val scrollState = rememberScrollState()
            
            TextField(
                value = formatSessionDetails(session),
                onValueChange = { /* Read-only */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState),
                readOnly = true,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

/**
 * Formats the DetailedSession into a human-readable string
 */
private fun formatSessionDetails(session: DetailedSession): String {
    return buildString {
        appendLine("URL: ${session.url ?: "Unknown"}")
        appendLine("Method: ${session.method ?: "Unknown"}")
        appendLine()
        
        appendLine("=== REQUEST ===")
        appendLine("Headers:")
        if (session.requestHeaders.isNullOrEmpty()) {
            appendLine("No headers available")
        } else {
            session.requestHeaders.forEach { (key, value) ->
                appendLine("$key: $value")
            }
        }
        
        appendLine()
        appendLine("Body:")
        appendLine(session.requestBody ?: "No body")
        appendLine()
        
        appendLine("=== RESPONSE ===")
        appendLine("Status: ${session.responseCode ?: "Unknown"}")
        
        appendLine("Headers:")
        if (session.responseHeaders.isNullOrEmpty()) {
            appendLine("No headers available")
        } else {
            session.responseHeaders.forEach { (key, value) ->
                appendLine("$key: $value")
            }
        }
        
        appendLine()
        appendLine("Body:")
        appendLine(session.responseBody ?: "No body")
        
        session.errorMessage?.let {
            appendLine()
            appendLine("Error Message: $it")
        }
    }
} 