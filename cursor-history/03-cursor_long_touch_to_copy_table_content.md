# Long Touch to Copy Table Contents
_Exported on 6/28/2025 at 00:32:27 GMT+3 from Cursor (1.1.5)_

---

**User**

I want to make it possible to copy the contents of each line of the table (filled with API response) by holding it with a finger - long touch. Could you please sketch?

---

**Cursor**

```plaintext
package com.unofficial.ahl

import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.style.TextDirection
import com.unofficial.ahl.model.HebrewWord
import com.unofficial.ahl.viewmodel.MainViewModel
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.widget.Toast

// ... existing code ...

@Composable
fun AhlApp(viewModel: MainViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val apiError by viewModel.apiError.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    // Pre-load string resources
    val apiErrorText = stringResource(R.string.api_error)
    val textCopiedMessage = stringResource(R.string.text_copied)

    // Control visibility of the error banner
    var showErrorBanner by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Update error banner state when API error changes
    LaunchedEffect(apiError) {
        apiError?.let { error ->
            errorMessage = buildString {
                append(apiErrorText)
                error.statusCode?.let { code -> append(" ($code)") }
                append(": ${error.message}")
            }
            showErrorBanner = true
            
            // Auto-dismiss after a delay
            delay(5000)
            showErrorBanner = false
            viewModel.clearApiError()
        }
    }
    
    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Error banner at the top of the screen (visible above keyboard)
                AnimatedVisibility(
                    visible = showErrorBanner,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    ErrorBanner(
                        message = errorMessage,
                        onDismiss = {
                            showErrorBanner = false
                            viewModel.clearApiError()
                        }
                    )
                }
                
                // Main content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // App title
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                    
                    // Search bar
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onSearch = { 
                            viewModel.searchWords()
                        },
                        onRefresh = {
                            viewModel.searchWords(forceRefresh = true)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Content based on state
                    when (uiState) {
                        is MainViewModel.UiState.Initial -> {
                            // Initial state, show nothing or instructions
                        }
                        is MainViewModel.UiState.Loading -> {
                            LoadingIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        is MainViewModel.UiState.Success -> {
                            val words = (uiState as MainViewModel.UiState.Success).words
                            
                            // Hide keyboard only if results list is long
                            LaunchedEffect(words) {
                                if (words.size > 3) {
                                    keyboardController?.hide()
                                }
                            }
                            
                            WordsList(
                                words = words,
                                onCopyText = { text ->
                                    coroutineScope.launch {
                                        scaffoldState.snackbarHostState.showSnackbar(
                                            message = textCopiedMessage,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                }
                            )
                        }
                        is MainViewModel.UiState.NoResults -> {
                            EmptyState(message = stringResource(R.string.no_results))
                        }
                        is MainViewModel.UiState.Error -> {
                            val message = (uiState as MainViewModel.UiState.Error).message
                            ErrorState(message = message)
                        }
                    }
                }
            }
        }
    }
}

// ... existing code ...

@Composable
fun WordsList(
    words: List<HebrewWord>, 
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(words) { word ->
            WordItem(
                word = word,
                onCopyText = onCopyText
            )
            Divider()
        }
    }
}

@Composable
fun WordItem(
    word: HebrewWord, 
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Format all the content that will be copied when long-pressed
    val copyContent = buildString {
        append(word.title)
        append("\n")
        word.definition?.let { 
            append(it)
            append("\n")
        }
        append(word.itemTypeName)
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        // Copy to clipboard
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("hebrew_word", copyContent)
                        clipboardManager.setPrimaryClip(clipData)
                        
                        // Notify through callback
                        onCopyText(copyContent)
                    }
                )
            }
    ) {
        // Title with nikud and without
        Text(
            text = word.title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Definition
        word.definition?.let { definition ->
            Text(
                text = definition,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        // Item type
        Text(
            text = word.itemTypeName,
            fontSize = 12.sp,
            color = MaterialTheme.colors.secondary
        )
    }
}

// ... existing code ...
```

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">AHL Unofficial</string>
    <string name="search_hint">חיפוש מילים בעברית</string>
    <string name="search_button">חפש</string>
    <string name="loading">טוען נתונים...</string>
    <string name="no_results">לא נמצאו תוצאות</string>
    <string name="error_loading">שגיאה בטעינת נתונים</string>
    <string name="refresh">רענן</string>
    <string name="refresh_from_api">רענן מהשרת</string>
    <string name="api_error">שגיאת שרת</string>
    <string name="dismiss">סגור</string>
    <string name="text_copied">הטקסט הועתק ללוח</string>
</resources>
```

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Academy of the Hebrew Language - Unofficial</string>
    <string name="search_hint">Enter search term…</string>
    <string name="search_button">Search</string>
    <string name="loading">Loading...</string>
    <string name="no_results">No results found.</string>
    <string name="error_loading">Error loading data. Please try again.</string>
    <string name="refresh">Refresh</string>
    <string name="refresh_from_api">Refresh from API</string>
    <string name="api_error">API Error</string>
    <string name="dismiss">Dismiss</string>
    <string name="text_copied">Text copied to clipboard</string>
</resources>
```

I've added the long-press to copy functionality to your app. Here's what I implemented:

1. Added gesture detection to the `WordItem` composable using `pointerInput` and `detectTapGestures` to handle long press events.

2. When a user long-presses a word item, it:
   - Copies all the word's information (title, definition, and type) to the clipboard
   - Shows a snackbar notification "Text copied to clipboard"

3. Updated the app layout to use a `Scaffold` which provides the snackbar functionality.

4. Added a new string resource for the copy notification.

The implementation follows Android best practices for clipboard operations and provides clear visual feedback to the user when text is copied. The function neatly formats the clipboard text with each element on its own line for better readability when pasted elsewhere.

---

**User**

When cleanOldCache is called? Is it called on every start of the app or sometime else?

---

**Cursor**

Based on the code, the `cleanOldCache` function is called when the MainViewModel is initialized. In the MainViewModel's `init` block (lines 34-38), it launches a coroutine that calls `repository.cleanOldCache()`:

```kotlin
// When ViewModel is created, clean old cache entries
init {
    viewModelScope.launch {
        repository.cleanOldCache()
    }
}
```

The `cleanOldCache` function itself (from HebrewWordsRepository.kt) removes cache entries that are older than 30 days:

```kotlin
suspend fun cleanOldCache() {
    withContext(Dispatchers.IO) {
        val thirtyDaysAgo = Date().time - TimeUnit.DAYS.toMillis(30)
        searchCacheDao.deleteOldEntries(thirtyDaysAgo)
    }
}
```

Since the MainViewModel is instantiated in the MainActivity using `viewModels()` delegate, the cache cleaning happens every time the app is started and the MainActivity is created, which effectively means it runs on every app launch.

```plaintext
// When ViewModel is created, clean old cache entries
init {
    viewModelScope.launch {
        repository.cleanOldCache()
    }
}
```

```plaintext
suspend fun cleanOldCache() {
    withContext(Dispatchers.IO) {
        val thirtyDaysAgo = Date().time - TimeUnit.DAYS.toMillis(30)
        searchCacheDao.deleteOldEntries(thirtyDaysAgo)
    }
}
```

