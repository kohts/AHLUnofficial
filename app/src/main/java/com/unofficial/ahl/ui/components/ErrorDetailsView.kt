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
import com.unofficial.ahl.repository.HebrewWordsRepository.DafMilaDetailsError
import com.unofficial.ahl.util.HtmlParsingException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Composable function to display error details in a scrollable text field
 * Supports both DetailedSession and DafMilaDetailsError types
 */
@Composable
fun ErrorDetailsView(
    errorDetails: Any,
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
                value = formatErrorDetails(errorDetails),
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
 * Formats error details into a human-readable string
 * Supports both DetailedSession and DafMilaDetailsError types
 */
private fun formatErrorDetails(errorDetails: Any): String {
    return when (errorDetails) {
        is DetailedSession -> formatSessionDetails(errorDetails)
        is DafMilaDetailsError -> formatDafMilaError(errorDetails)
        else -> "Unknown error type: ${errorDetails::class.simpleName}"
    }
}

/**
 * Formats the DetailedSession into a human-readable string
 */
private fun formatSessionDetails(session: DetailedSession): String {
    return buildString {
        appendLine("ERROR TYPE: Network/API Error")
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

/**
 * Formats the DafMilaDetailsError into a human-readable string
 */
private fun formatDafMilaError(error: DafMilaDetailsError): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    return buildString {
        appendLine("ERROR TYPE: DafMila Details Fetch Error")
        appendLine("Keyword: ${error.keyword}")
        appendLine("Timestamp: ${dateFormat.format(error.timestamp)}")
        appendLine("Message: ${error.message}")
        appendLine("Exception: ${error.exception::class.simpleName}")
        error.statusCode?.let { 
            appendLine("Status Code: $it")
        }
        appendLine()
        
        appendLine("=== EXCEPTION DETAILS ===")
        appendLine("Exception Type: ${error.exception::class.qualifiedName}")
        appendLine("Exception Message: ${error.exception.message}")
        
        // Show detailed context for HTML parsing exceptions
        if (error.exception is HtmlParsingException) {
            appendLine()
            appendLine("=== HTML PARSING CONTEXT ===")
            error.exception.jsObject?.let { jsObj ->
                appendLine("Extracted JS Object (this should be valid JSON):")
                appendLine(jsObj)
                appendLine()
            }
        }
        
        error.exception.cause?.let { cause ->
            appendLine("Caused by: ${cause::class.simpleName}: ${cause.message}")
        }
        
        appendLine()
        appendLine("Stack Trace:")
        error.exception.stackTrace.take(10).forEach { element ->
            appendLine("  at $element")
        }
        if (error.exception.stackTrace.size > 10) {
            appendLine("  ... and ${error.exception.stackTrace.size - 10} more")
        }
        
        error.htmlResponse?.let { html ->
            appendLine()
            appendLine("=== HTML RESPONSE ===")
            appendLine("Length: ${html.length} characters")
            appendLine()
            if (html.length > 200000) {
                appendLine(html.take(200000))
                appendLine()
                appendLine("... (truncated, full length: ${html.length} characters)")
            } else {
                appendLine(html)
            }
        } ?: run {
            appendLine()
            appendLine("HTML Response: Not available (error occurred before HTML fetch)")
        }
    }
} 