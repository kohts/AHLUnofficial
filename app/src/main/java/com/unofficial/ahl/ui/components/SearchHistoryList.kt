package com.unofficial.ahl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import com.unofficial.ahl.model.SearchHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dropdown list of search history entries
 */
@Composable
fun SearchHistoryList(
    searchHistory: List<SearchHistory>,
    onHistoryItemClick: (SearchHistory) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (searchHistory.isEmpty()) {
        return
    }
    
    Popup(
        alignment = Alignment.TopCenter,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .zIndex(100f),
            elevation = 8.dp,
            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Search History",
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearHistory) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear History"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
                
                Divider()
                
                // History items in a scrollable list (limited height)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp) // Show ~3 items at a time (adjust as needed)
                ) {
                    items(searchHistory) { historyItem ->
                        SearchHistoryItem(
                            historyItem = historyItem,
                            onClick = { onHistoryItemClick(historyItem) }
                        )
                        Divider(startIndent = 48.dp)
                    }
                }
            }
        }
    }
}

/**
 * Individual search history item
 */
@Composable
fun SearchHistoryItem(
    historyItem: SearchHistory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colors.primary.copy(alpha = 0.6f),
            modifier = Modifier.padding(end = 16.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = historyItem.searchTerm,
                style = MaterialTheme.typography.body1.copy(
                    textAlign = TextAlign.Right,
                    textDirection = TextDirection.Rtl
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Format the date
            val formattedDate = formatDate(historyItem.timestamp)
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.caption,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * Format a date for display in the search history
 */
private fun formatDate(date: Date): String {
    val now = Date()
    val diffInMillis = now.time - date.time
    val diffInHours = diffInMillis / (1000 * 60 * 60)
    
    return when {
        diffInHours < 1 -> {
            val diffInMinutes = diffInMillis / (1000 * 60)
            if (diffInMinutes < 1) "Just now" else "$diffInMinutes minutes ago"
        }
        diffInHours < 24 -> "$diffInHours hours ago"
        diffInHours < 48 -> "Yesterday"
        else -> {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            dateFormat.format(date)
        }
    }
} 