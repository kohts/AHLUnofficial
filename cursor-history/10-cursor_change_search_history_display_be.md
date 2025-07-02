# Change search history display behavior
_Exported on 6/28/2025 at 00:34:19 GMT+3 from Cursor (1.1.5)_

---

**User**

Now we have a Search History drop down which is shown above (on top of) Search Field (so that it is not visible).

I want to change when Search History is shown.

At the moment Search History is shown in two cases:
1) when the application starts up
2) when application returns from the Detailed word screen.

I don't want it to be shown automatically.

Instead I'd like a button with "History" icon shown inside of the main Search Field - on its left side (opposite to the side where "Clear field" icon is shown). Once a user presses on the "History" icon - show the history list.

Could you try adjusting the behavior?


---

**Cursor**

```plaintext
if (isSearchFocused && searchHistory.isNotEmpty()) {
    SearchHistoryList(...)
}
```

I'll help you modify the search history behavior to show it only when the user clicks a History icon button instead of automatically showing when focused.

```plaintext
// ... existing code ...
import androidx.compose.material.icons.filled.History
// ... existing code ...

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onClearClick: () -> Unit,
    searchHistory: List<SearchHistory>,
    onHistoryItemClick: (SearchHistory) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State to track if search field is focused
    var isSearchFocused by remember { mutableStateOf(false) }
    // State to track if history should be shown (controlled by History button)
    var showHistory by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onSearch) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search_button)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            val focusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { 
                        onSearch()
                        isSearchFocused = false
                        showHistory = false // Hide history when search is performed
                    }
                ),
                textStyle = MaterialTheme.typography.body1.copy(
                    textAlign = TextAlign.Right,
                    textDirection = TextDirection.Rtl
                ),
                leadingIcon = {
                    // History icon button - only show if there's search history
                    if (searchHistory.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                showHistory = !showHistory
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = stringResource(R.string.search_history),
                                tint = if (showHistory) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                onQueryChange("")
                                focusRequester.requestFocus()
                                keyboardController?.show()
                                onClearClick()
                                showHistory = false // Hide history when clearing
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = Color.Red
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        isSearchFocused = focusState.isFocused
                        // Hide history when field loses focus
                        if (!focusState.isFocused) {
                            showHistory = false
                        }
                    }
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onRefresh,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.refresh)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.refresh_from_api))
            }
        }
        
        // Show search history only when explicitly requested by user clicking History button
        if (showHistory && searchHistory.isNotEmpty()) {
            SearchHistoryList(
                searchHistory = searchHistory,
                onHistoryItemClick = { 
                    onHistoryItemClick(it)
                    showHistory = false // Hide history after selection
                },
                onClearHistory = onClearHistory,
                onDismiss = { showHistory = false }
            )
        }
    }
}
```

Now I need to check if the `search_history` string resource exists, and if not, we should add it to the string resources.

I need to add the missing `search_history` string resource to the strings.xml file:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Academy of the Hebrew Language - Unofficial</string>
    <string name="search_hint">Search in Hebrew</string>
    <string name="search_button">Search</string>
    <string name="search_history">Search History</string>
    <string name="loading">Loading...</string>
    <string name="no_results">No results found</string>
    <string name="error_loading">Error loading data</string>
    <string name="refresh">Refresh</string>
    <string name="refresh_from_api">Refresh from API</string>
    <string name="clear_search">Clear search</string>
    <string name="dismiss">Dismiss</string>
    <string name="text_copied">Text copied to clipboard</string>
    <string name="no_title_available">No title available</string>
    <string name="api_error">API Error</string>
    <string name="invalid_data_format">Invalid response from API</string>
    <string name="no_details_available">No detailed information available</string>
</resources>
```

Perfect! I've implemented the requested changes to modify the search history behavior. Here's what I changed:

## Key Changes Made:

1. **Added a History icon button** inside the search field on the left side (opposite to the Clear button)
2. **Changed when history is shown** - it no longer appears automatically when focused, only when the History button is clicked
3. **Added proper state management** with a new `showHistory` state variable
4. **Added visual feedback** - the History icon changes color when active vs inactive
5. **Added the missing string resource** for accessibility

## Specific Implementation:

- **History Icon**: Added as `leadingIcon` in the `OutlinedTextField` using `Icons.Default.History`
- **Conditional Display**: The History icon only appears when there are items in search history
- **Toggle Behavior**: Clicking the History icon toggles the history list visibility
- **Auto-Hide Logic**: History automatically hides when:
  - User performs a search
  - Field is cleared
  - Field loses focus  
  - User selects a history item

## Visual Behavior:

- The History icon is muted (gray) when history is hidden
- The History icon is highlighted (primary color) when history is shown
- The icon only appears when there's actual search history to show

Now the search history will only appear when the user explicitly requests it by clicking the History icon, giving them full control over when to access their search history.

---

**User**

Yes, this is exactly what I needed!

Now let's remove "Clear" button from the Search History screen and replace it with a Cancek button, which just hides Search History and focuses into Search Field.

---

**Cursor**

I'll help you modify the Search History screen to replace the "Clear" button with a "Cancel" button that just hides the history and focuses back into the search field.

First, let me find the SearchHistoryList component to see its current implementation:

Now I'll modify the `SearchHistoryList` component to replace the "Clear" button with a "Cancel" button that hides the history and focuses back into the search field.

```plaintext
// ... existing code ...
import androidx.compose.material.icons.filled.Close
// ... existing code ...

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
                    TextButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                }
                
                Divider()
                
                // History items in a scrollable list (limited height)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp) // Show ~3 items at a time (adjust as needed)
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
```

Now I need to update the `SearchBar` component in `MainActivity.kt` to handle the cancel action properly by focusing back into the search field when the history is dismissed:

```plaintext
        // Show search history only when explicitly requested by user clicking History button
        if (showHistory && searchHistory.isNotEmpty()) {
            SearchHistoryList(
                searchHistory = searchHistory,
                onHistoryItemClick = { 
                    onHistoryItemClick(it)
                    showHistory = false // Hide history after selection
                },
                onClearHistory = onClearHistory,
                onDismiss = { 
                    showHistory = false 
                    // Focus back into search field and show keyboard
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            )
        }
```

Perfect! I've successfully updated the search history behavior as requested:

## Changes Made:

### 1. **SearchHistoryList Component** (`SearchHistoryList.kt`):
- **Replaced "Clear" button with "Cancel" button**
- **Changed icon** from `Icons.Default.Delete` to `Icons.Default.Close`
- **Changed action** from calling `onClearHistory` to calling `onDismiss`
- **Updated text** from "Clear" to "Cancel"

### 2. **SearchBar Component** (`MainActivity.kt`):
- **Enhanced the `onDismiss` callback** to:
  - Hide the search history
  - Focus back into the search field
  - Show the keyboard

## New Behavior:

- **No more destructive "Clear" action** - users can't accidentally delete their search history
- **"Cancel" button** simply closes the history dropdown
- **Smooth return to search** - when Cancel is pressed, the focus automatically returns to the search field with the keyboard visible
- **Non-destructive workflow** - search history is preserved and users can easily return to searching

The search history dropdown now behaves like a typical dropdown/modal - it can be dismissed without losing data, and the user is smoothly returned to the previous state (focused search field with keyboard).

---

**User**

Now Let's make the search history list right to left native - which is suitable for Hebrew.

---

**Cursor**

```plaintext
Text(
    text = historyItem.searchTerm,
    style = MaterialTheme.typography.body1.copy(
        textAlign = TextAlign.Right,
        textDirection = TextDirection.Rtl
    ),
    maxLines = 1,
    overflow = TextOverflow.Ellipsis
)
```

I'll modify the SearchHistoryList component to make it properly right-to-left (RTL) for Hebrew, which means adjusting the layout direction, icon positioning, and text alignment throughout the component.

```plaintext
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
                // Header - RTL layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel button on the left in RTL layout
                    TextButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = "Search History",
                        style = MaterialTheme.typography.subtitle1,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary
                    )
                }
                
                Divider()
                
                // History items in a scrollable list (limited height)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp) // Show ~3 items at a time (adjust as needed)
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
 * Individual search history item - RTL layout
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
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End // Align content to the right
        ) {
            Text(
                text = historyItem.searchTerm,
                style = MaterialTheme.typography.body1.copy(
                    textAlign = TextAlign.Right,
                    textDirection = TextDirection.Rtl
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Format the date - also right-aligned
            val formattedDate = formatDate(historyItem.timestamp)
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.caption,
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Search icon on the right side for RTL layout
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colors.primary.copy(alpha = 0.6f)
        )
    }
}
```

Perfect! I've transformed the SearchHistoryList component to be properly RTL-native for Hebrew. Here are the key changes I made:

## **Header Changes (RTL Layout):**
- **Reordered elements**: Cancel button now on the left, "Search History" title and History icon on the right
- **Right-aligned title**: The "Search History" text is now right-aligned  
- **Proper spacing**: Used flexible spacing to push elements to their respective sides

## **Individual History Items (RTL Layout):**
- **Icon position**: Moved the search icon from left side to right side
- **Content alignment**: All text content is now right-aligned and the column is aligned to the end
- **Full-width text**: Both search term and timestamp use full width with right alignment
- **Natural flow**: The layout now flows right-to-left as expected in Hebrew

## **Visual Result:**
```
[Cancel]                    [Search History 🕐]
----------------------------------------
                    [Search Term Here] 🔍
                         [2 hours ago]
----------------------------------------
                [Another Search Term] 🔍  
                          [Yesterday]
```

The search history now follows proper RTL conventions:
- **Visual hierarchy flows right-to-left**
- **Icons are positioned on the right side** 
- **All text is right-aligned**
- **Layout feels natural for Hebrew speakers**

This creates a much more intuitive experience for Hebrew users, matching the RTL nature of the Hebrew language throughout the search history interface.

```plaintext
[Cancel]                    [Search History 🕐]
----------------------------------------
                    [Search Term Here] 🔍
                         [2 hours ago]
----------------------------------------
                [Another Search Term] 🔍  
                          [Yesterday]
```

---

**User**

Good!

Now when populating Search History I'd like to exclude repeated (same) adjacent entries - let's leave only the earliest search term in the history list. For example. if you find in the database the following list sorted by search time descending

a 2 min ago
a 4 min ago
a 5 min ago
b 10 min ago
b 13 min ago
c 20 min ago
c 25 min ago

I'd like the search history list to contain these entries:
a 5 min ago
b 13 min ago
c 25 min ago


---

**Cursor**

I'll modify the repository layer to filter out consecutive duplicate search terms, keeping only the earliest (oldest) occurrence of each consecutive group.

```plaintext
fun getSearchHistory(limit: Int = 100) = searchHistoryDao.getRecentSearches(limit)
    .map { historyList ->
        filterConsecutiveDuplicates(historyList)
    }

private fun filterConsecutiveDuplicates(historyList: List<SearchHistory>): List<SearchHistory> {
    if (historyList.isEmpty()) return historyList
    
    val filtered = mutableListOf<SearchHistory>()
    var i = 0
    
    while (i < historyList.size) {
        val currentTerm = historyList[i].searchTerm
        var j = i
        
        // Find the end of this consecutive group
        while (j < historyList.size && historyList[j].searchTerm == currentTerm) {
            j++
        }
        
        // Add the last item of this group (earliest timestamp)
        filtered.add(historyList[j - 1])
        
        // Move to the next group
        i = j
    }
    
    return filtered
}
```

```plaintext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import java.util.Date
// ... existing code ...

    /**
     * Get the search history
     * @param limit The maximum number of entries to return
     * @return Flow of search history entries, ordered by timestamp (newest first), with consecutive duplicates filtered out
     */
    fun getSearchHistory(limit: Int = 100) = searchHistoryDao.getRecentSearches(limit)
        .map { historyList ->
            filterConsecutiveDuplicates(historyList)
        }
    
    /**
     * Filter out consecutive duplicate search terms, keeping only the earliest occurrence of each consecutive group
     * @param historyList List of search history entries ordered by timestamp DESC (newest first)
     * @return Filtered list with consecutive duplicates removed
     */
    private fun filterConsecutiveDuplicates(historyList: List<SearchHistory>): List<SearchHistory> {
        if (historyList.isEmpty()) return historyList
        
        val filtered = mutableListOf<SearchHistory>()
        var i = 0
        
        while (i < historyList.size) {
            val currentTerm = historyList[i].searchTerm
            var j = i
            
            // Find the end of this consecutive group of same search terms
            while (j < historyList.size && historyList[j].searchTerm == currentTerm) {
                j++
            }
            
            // Add the last item of this group (which has the earliest timestamp 
            // since the list is ordered newest first)
            filtered.add(historyList[j - 1])
            
            // Move to the next group
            i = j
        }
        
        return filtered
    }
```

```plaintext
   a 2 min ago
   a 4 min ago  
   a 5 min ago
   b 10 min ago
   b 13 min ago
   c 20 min ago
   c 25 min ago
```

```plaintext
   a 5 min ago
   b 13 min ago
   c 25 min ago
```

Perfect! I've implemented the consecutive duplicate filtering logic in the repository layer. Here's what I added:

## **Key Changes:**

### **1. Flow Transformation**
- Modified `getSearchHistory()` to use `.map()` to filter the data before returning it
- Added import for `kotlinx.coroutines.flow.map`

### **2. Filtering Algorithm** 
- Added `filterConsecutiveDuplicates()` function that:
  - Groups consecutive identical search terms
  - Keeps only the **last item** in each group (which has the earliest timestamp since the list is ordered newest first)

## **How It Works:**

Given your example database entries (ordered newest first):
```
a 2 min ago    ← newest
a 4 min ago
a 5 min ago    ← oldest in this group - KEEP
b 10 min ago   ← newest  
b 13 min ago   ← oldest in this group - KEEP
c 20 min ago   ← newest
c 25 min ago   ← oldest in this group - KEEP
```

**Algorithm steps:**
1. **Group 1**: Find consecutive "a" entries (positions 0-2) → keep last one (5 min ago)
2. **Group 2**: Find consecutive "b" entries (positions 3-4) → keep last one (13 min ago)  
3. **Group 3**: Find consecutive "c" entries (positions 5-6) → keep last one (25 min ago)

**Final filtered result:**
```
a 5 min ago
b 13 min ago  
c 25 min ago
```

## **Benefits:**
- **Clean history**: No more cluttered consecutive duplicates
- **Preserves earliest searches**: Shows when you first searched for each term
- **Transparent**: ViewModel and UI don't need any changes
- **Efficient**: Uses Flow transformations properly
- **Maintains order**: Filtered results still show newest first

The search history will now show a much cleaner list with only the earliest occurrence of each consecutive search term group!

```plaintext
a 2 min ago    ← newest
a 4 min ago
a 5 min ago    ← oldest in this group - KEEP
b 10 min ago   ← newest  
b 13 min ago   ← oldest in this group - KEEP
c 20 min ago   ← newest
c 25 min ago   ← oldest in this group - KEEP
```

```plaintext
a 5 min ago
b 13 min ago  
c 25 min ago
```

---

**User**

Looks good, now let's make sure we don't add into the search history database the term which was looked up in cache as the result of a user selecting it from the search history list.

---

**Cursor**

I'll modify the repository and ViewModel to prevent adding search terms back to the history when they come from selecting a history item. This will avoid creating duplicates when users click on history items.

```plaintext
    /**
     * Search for Hebrew words matching the query
     * @param query The search query
     * @param forceRefresh Whether to force a fresh API request
     * @param addToHistory Whether to add this search to the search history
     * @return ApiResult with either the data or error details
     */
    suspend fun searchWords(query: String, forceRefresh: Boolean = false, addToHistory: Boolean = true): ApiResult<List<HebrewWord>> {
        return withContext(Dispatchers.IO) {
            // Normalize the search term
            val normalizedQuery = normalizeSearchTerm(query)
            var apiError: Exception? = null
            var errorMessage: String? = null
            var statusCode: Int? = null
            
            // Always check cache first, regardless of forceRefresh, to have a fallback
            val cachedResult = searchCacheDao.getCachedSearch(normalizedQuery)
            val cachedWords = cachedResult?.let { 
                val words = parseJsonToHebrewWords(it.apiResponse)
                filterValidWords(words)
            } ?: emptyList()
            
            // If we have valid cache data and not forcing refresh, return it immediately
            if (!forceRefresh && cachedWords.isNotEmpty()) {
                // Add to search history when retrieved from cache only if requested
                if (addToHistory) {
                    addToSearchHistory(normalizedQuery)
                }
                return@withContext ApiResult.Success(cachedWords)
            }
            
            // If forcing refresh or no valid cache, try the API
            try {
                val apiResult = api.searchWords(normalizedQuery)
                
                // Filter out invalid entries
                val validWords = filterValidWords(apiResult)
                
                // Check if we have any valid results
                if (validWords.isEmpty() && apiResult.isNotEmpty()) {
                    // We have results but none are valid
                    errorMessage = "Invalid data format returned from API"
                    apiError = InvalidDataException(errorMessage)
                    
                    // If we have cached results, use them instead
                    if (cachedWords.isNotEmpty()) {
                        if (addToHistory) {
                            addToSearchHistory(normalizedQuery)
                        }
                        return@withContext ApiResult.Success(cachedWords)
                    }
                    
                    return@withContext ApiResult.Error(
                        exception = apiError,
                        message = errorMessage
                    )
                }
                
                // If successful, cache the result
                if (apiResult.isNotEmpty()) {
                    val jsonResponse = gson.toJson(apiResult)
                    val cacheEntry = SearchCache(
                        searchTerm = normalizedQuery,
                        apiResponse = jsonResponse,
                        timestamp = Date()
                    )
                    searchCacheDao.insertCache(cacheEntry)
                    
                    // Add to search history when API call is successful only if requested
                    if (addToHistory) {
                        addToSearchHistory(normalizedQuery)
                    }
                }
                
                ApiResult.Success(validWords)
            } catch (e: Exception) {
                // Log the error
                e.printStackTrace()
                
                // Extract HTTP status code if available
                statusCode = when (e) {
                    is HttpException -> e.code()
                    else -> null
                }
                
                // Generate a meaningful error message
                errorMessage = when (e) {
                    is HttpException -> "Server error: ${e.message()} (${e.code()})"
                    is IOException -> "Network error: ${e.message}"
                    else -> "Unknown error: ${e.message}"
                }
                
                apiError = e
                
                // Use cached data if available
                if (cachedWords.isNotEmpty()) {
                    // Add to search history when using cached fallback only if requested
                    if (addToHistory) {
                        addToSearchHistory(normalizedQuery)
                    }
                    return@withContext ApiResult.Success(cachedWords)
                }
                
                ApiResult.Error(apiError, errorMessage, statusCode)
            }
        }
    }
```

Now I need to update the ViewModel to use the new parameter, preventing history addition when searching from a history item:

```plaintext
    /**
     * Use a history item to perform a search
     * @param historyItem The search history item to use
     */
    fun searchWithHistoryItem(historyItem: SearchHistory) {
        // Update the search query
        _searchQuery.value = historyItem.searchTerm
        
        // Perform the search without adding to history (since it's already in history)
        _uiState.value = UiState.Loading
        
        viewModelScope.launch {
            // Save the query to preferences
            preferencesManager.saveSearchQuery(historyItem.searchTerm)
            
            when (val result = repository.searchWords(historyItem.searchTerm, forceRefresh = false, addToHistory = false)) {
                is ApiResult.Success -> {
                    val words = result.data
                    _uiState.value = if (words.isEmpty()) {
                        UiState.NoResults
                    } else {
                        // Check if any words have invalid data (null titles)
                        val containsInvalidData = words.any { it.title.isNullOrBlank() }
                        if (containsInvalidData) {
                            _invalidDataDetected.value = true
                        }
                        
                        UiState.Success(words)
                    }
                }
                is ApiResult.Error -> {
                    // Track the API error
                    _apiError.value = ApiError(
                        message = result.message,
                        timestamp = Date(),
                        searchTerm = historyItem.searchTerm,
                        statusCode = result.statusCode
                    )
                    
                    // Try again but explicitly use cache only (without adding to history)
                    tryFallbackToCacheWithoutHistory(historyItem.searchTerm)
                }
            }
        }
        
        // Hide the search history
        _showSearchHistory.value = false
    }
    
    /**
     * Try to fetch results from cache as a fallback without adding to history
     */
    private fun tryFallbackToCacheWithoutHistory(query: String) {
        viewModelScope.launch {
            try {
                // This is a more explicit way to try to get data from cache only without adding to history
                val cachedResult = repository.searchWords(query, forceRefresh = false, addToHistory = false)
                
                when (cachedResult) {
                    is ApiResult.Success -> {
                        val words = cachedResult.data
                        if (words.isNotEmpty()) {
                            _uiState.value = UiState.Success(words)
                            return@launch
                        }
                    }
                    else -> { /* Cache fallback also failed */ }
                }
                
                // If we get here, even the cache fallback failed
                _uiState.value = UiState.Error("Failed to load data. No cached results available.")
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to load data. No cached results available.")
            }
        }
    }
```

Perfect! I've successfully implemented the solution to prevent adding search terms back to the history database when they come from selecting a history item. Here's what I changed:

## **Repository Changes** (`HebrewWordsRepository.kt`):

### **Added `addToHistory` Parameter:**
- Modified `searchWords()` method to accept `addToHistory: Boolean = true`
- Added conditional logic around all `addToSearchHistory()` calls
- This affects both cache hits and API calls

## **ViewModel Changes** (`MainViewModel.kt`):

### **Modified `searchWithHistoryItem()`:**
- Now calls repository directly with `addToHistory = false`
- Duplicated the search logic but without history addition
- Added separate error handling method `tryFallbackToCacheWithoutHistory()`

### **Preserved Regular Search Behavior:**
- Regular `searchWords()` calls continue to use default `addToHistory = true`
- No changes to manual search functionality

## **Result:**

### **✅ Regular Searches** (typing + search button):
- **Still add to history** as expected
- Users build their search history normally

### **✅ History Item Selection** (clicking history items):
- **Don't add to history** - prevents duplicates
- Users can reuse history without cluttering it

### **✅ Clean History:**
- No artificial duplicate entries from history selections
- Maintains the natural search flow
- Combined with the consecutive duplicate filtering, users get a very clean history experience

Now when users click on a history item, it will perform the search but won't add that term back to the history database, preventing unnecessary duplicates while preserving the useful search history functionality!

---

**User**

When expected content is not found in the attached lines of code)  - the details are not populated and "Show Details" button doesn't show anything. It would be better if the expected contents is not found - the user gets the error message, but "Show Details" button shows the HTML which does not contain the expected structures (could not be parsed as expected).

---

**Cursor**

```plaintext
// Extract the relevant content using the HTML parser
val extractedContent = HtmlParser.extractContent(htmlContent)

if (extractedContent.isBlank()) {
    return@withContext ApiResult.Error(
        exception = InvalidDataException("No content found"),
        message = "No relevant content found on the details page"
    )
}

ApiResult.Success(extractedContent)
```

I'll modify the `fetchWordDetails` method to return the raw HTML content when parsing fails, allowing users to see the unprocessed content while also showing an error message about the parsing failure.

```plaintext
                // Extract the relevant content using the HTML parser
                val extractedContent = HtmlParser.extractContent(htmlContent)
                
                if (extractedContent.isBlank()) {
                    // If parsing failed, return the raw HTML with a parsing failure indicator
                    val rawContentWithWarning = """
                        <div style="background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 10px; margin-bottom: 10px; border-radius: 4px;">
                            <strong>⚠️ Parsing Warning:</strong> The expected content structure was not found. Showing raw content below.
                        </div>
                        
                        $htmlContent
                    """.trimIndent()
                    
                    return@withContext ApiResult.Success(rawContentWithWarning)
                }
                
                ApiResult.Success(extractedContent)
```

Perfect! I've modified the `fetchWordDetails` method to handle parsing failures more gracefully. Here's what changed:

## **Previous Behavior:**
- When `HtmlParser.extractContent()` returned blank → **Error result**
- "Show Details" button showed nothing
- User got an error but no content to examine

## **New Behavior:**
- When `HtmlParser.extractContent()` returns blank → **Success result with raw HTML**
- Returns the complete HTML with a warning banner
- "Show Details" button will work and show the unprocessed content

## **What Users Will See:**

### **Warning Banner:**
```
⚠️ Parsing Warning: The expected content structure was not found. Showing raw content below.
```

### **Below the Warning:**
- The complete raw HTML from the Hebrew Academy page
- Users can examine the actual structure to understand what's available
- Developers can see what changed in the page structure

## **Benefits:**

1. **✅ User Experience:** Users still get to see the fetched content instead of a blank screen
2. **✅ Error Awareness:** Clear warning that parsing failed  
3. **✅ Debugging:** Raw HTML helps identify what structure changes occurred
4. **✅ Functionality:** "Show Details" button always works when data is fetched
5. **✅ Graceful Degradation:** App doesn't break when Hebrew Academy changes their page structure

Now when the Hebrew Academy website changes their HTML structure and breaks the parser, users will still be able to access the raw content while being informed that the parsing didn't work as expected.

```plaintext
⚠️ Parsing Warning: The expected content structure was not found. Showing raw content below.
```

---

**User**

In extractContent function - let's rewrite it instead of text matching, let's put the htmlContent into a DOM object, then find div "post-millon" by id in the DOM object and return its content as the result of the function.

---

**Cursor**

I'll help you rewrite the `extractContent` function to use DOM parsing instead of text matching. First, let me find the HtmlParser class to see the current implementation.

Now I'll rewrite the `extractContent` function to use DOM parsing instead of text matching. First, I need to add a JSoup dependency for HTML parsing, then modify the function.

```plaintext
package com.unofficial.ahl.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Utility class for parsing HTML content using DOM parsing
 */
object HtmlParser {
    
    /**
     * Extract content from HTML by finding div with id "post-millon" using DOM parsing
     * 
     * @param htmlContent The raw HTML content
     * @return The extracted and cleaned content, or empty string if not found
     */
    fun extractContent(htmlContent: String): String {
        return try {
            // Parse the HTML content into a DOM object
            val document: Document = Jsoup.parse(htmlContent)
            
            // Find the div with id "post-millon"
            val postMillonDiv = document.getElementById("post-millon")
            
            if (postMillonDiv != null) {
                // Remove script tags and their content
                postMillonDiv.select("script").remove()
                
                // Return the HTML content of the div
                postMillonDiv.html()
            } else {
                // Element not found
                ""
            }
        } catch (e: Exception) {
            // If parsing fails, return empty string
            e.printStackTrace()
            ""
        }
    }
}
```

Now I need to add the JSoup dependency to the project. Let me check the build.gradle file to add it:

```gradle
    // Network
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    
    // HTML Parsing
    implementation 'org.jsoup:jsoup:1.17.2'
```

Perfect! I've successfully rewritten the `extractContent` function to use DOM parsing. Here's what I changed:

## **🔧 Changes Made:**

### **1. Updated HtmlParser.kt:**
- **Replaced text matching** with proper DOM parsing using JSoup
- **Simplified function signature** - removed unused parameters
- **Added robust error handling** with try-catch
- **Improved script removal** using JSoup's selector methods

### **2. Added JSoup Dependency:**
- **Added `org.jsoup:jsoup:1.17.2`** to `app/build.gradle`
- **JSoup** is a mature, reliable HTML parser for Java/Kotlin

## **🎯 How the New Function Works:**

### **DOM-Based Approach:**
```kotlin
// Parse HTML into DOM object
val document: Document = Jsoup.parse(htmlContent)

// Find div by ID (more reliable than text matching)
val postMillonDiv = document.getElementById("post-millon")

// Clean and return content
postMillonDiv.select("script").remove()
return postMillonDiv.html()
```

### **Benefits:**
- **✅ More reliable:** Won't break if surrounding HTML changes
- **✅ Precise targeting:** Finds exact element by ID
- **✅ Better error handling:** Graceful failure if element missing
- **✅ Cleaner script removal:** Uses CSS selectors instead of regex
- **✅ Future-proof:** Works regardless of HTML structure changes

## **📱 Result:**
When the Hebrew Academy page loads, the function will:
1. **Parse the entire HTML** into a proper DOM structure
2. **Find the specific div** with `id="post-millon"`
3. **Extract only that content** (ignoring everything else)
4. **Remove any script tags** for security
5. **Return clean HTML** for display

This approach is much more robust and will work even if the Hebrew Academy website changes their page layout, as long as they keep the `post-millon` div ID!

```plaintext
// Parse HTML into DOM object
val document: Document = Jsoup.parse(htmlContent)

// Find div by ID (more reliable than text matching)
val postMillonDiv = document.getElementById("post-millon")

// Clean and return content
postMillonDiv.select("script").remove()
return postMillonDiv.html()
```

