package com.unofficial.ahl

import android.os.Bundle
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.text.style.TextDirection
import com.unofficial.ahl.model.HebrewWord
import com.unofficial.ahl.model.SearchHistory
import com.unofficial.ahl.ui.DetailScreen
import com.unofficial.ahl.ui.screens.MainScreenLayout
import com.unofficial.ahl.ui.components.SearchHistoryList
import com.unofficial.ahl.ui.components.ZoomableBox
import com.unofficial.ahl.viewmodel.MainViewModel
import com.unofficial.ahl.viewmodel.ErrorViewModel
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import android.util.Log

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val errorViewModel: ErrorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AhlApp(viewModel, errorViewModel)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AhlApp(viewModel: MainViewModel, errorViewModel: ErrorViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val apiError by viewModel.apiError.collectAsState()
    val selectedWord by viewModel.selectedWord.collectAsState()
    val dafMilaState by viewModel.dafMilaState.collectAsState()
    val invalidDataDetected by viewModel.invalidDataDetected.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState(initial = emptyList())
    val keyboardController = LocalSoftwareKeyboardController.current
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    // State to track if keyboard should be shown after scroll stops
    var showKeyboardAfterScroll by remember { mutableStateOf(false) }

    // Global zoom and pan state management
    var zoomScale by remember { mutableStateOf(1f) }
    var panOffsetX by remember { mutableStateOf(0f) }
    var panOffsetY by remember { mutableStateOf(0f) }
    var transformOrigin by remember { mutableStateOf(TransformOrigin(1f, 0f)) }
    val minScale = 1f
    val maxScale = 3f
    
    // Calculate approximate search bar height (for coordinate conversion)
    val searchBarApproxHeight = 140.dp // App title + search field + refresh button
    
    // Get density and configuration in @Composable context
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    
    // Global zoom and pan handler
    val handleZoomAndPan: (Float, Offset?, Offset?) -> Unit = { zoomChange, gestureCenter, panDelta ->
        Log.d("ZoomPan", "Handler called: zoomChange=$zoomChange, gestureCenter=$gestureCenter, panDelta=$panDelta")
        
        val newScale = (zoomScale * zoomChange).coerceIn(minScale, maxScale)
        
        Log.d("ZoomPan", "Scale: $zoomScale -> $newScale")
        
        // Update transform origin based on gesture center if provided
        gestureCenter?.let { center ->
            // Convert global Y coordinate to relative position within word view
            val searchBarHeightPx = with(density) { searchBarApproxHeight.toPx() }
            val screenHeightPx = configuration.screenHeightDp * density.density
            
            // Calculate relative Y position within the word view area
            val wordViewStartY = searchBarHeightPx
            val wordViewHeight = screenHeightPx - wordViewStartY
            
            val wordViewRelativeY = if (center.y >= wordViewStartY && wordViewHeight > 0) {
                // Gesture is in the word view area - calculate relative position
                ((center.y - wordViewStartY) / wordViewHeight).coerceIn(0f, 1f)
            } else {
                // Gesture is outside word view area - maintain current origin or default
                transformOrigin.pivotFractionY.coerceIn(0f, 1f)
            }
            
            transformOrigin = TransformOrigin(
                pivotFractionX = 1f, // Always zoom from right side
                pivotFractionY = wordViewRelativeY
            )
            
            Log.d("ZoomPan", "Updated transform origin: $transformOrigin")
        }
        
        // Handle pan offset with bounds checking
        panDelta?.let { delta ->
            Log.d("ZoomPan", "Processing pan delta: $delta")
            
            val screenWidth = configuration.screenWidthDp * density.density
            val screenHeight = configuration.screenHeightDp * density.density
            
            // Calculate maximum pan bounds based on zoom level
            // When zoomed in, content becomes larger, so we can pan more
            val maxPanX = if (newScale > 1f) (screenWidth * (newScale - 1f)) / 2f else 0f
            val maxPanY = if (newScale > 1f) (screenHeight * (newScale - 1f)) / 2f else 0f
            
            Log.d("ZoomPan", "Pan bounds: maxX=$maxPanX, maxY=$maxPanY")
            
            val oldPanX = panOffsetX
            val oldPanY = panOffsetY
            
            // Apply pan delta with bounds checking
            panOffsetX = (panOffsetX + delta.x).coerceIn(-maxPanX, maxPanX)
            panOffsetY = (panOffsetY + delta.y).coerceIn(-maxPanY, maxPanY)
            
            Log.d("ZoomPan", "Pan offset: X: $oldPanX -> $panOffsetX, Y: $oldPanY -> $panOffsetY")
        }
        
        // Reset pan when zoom is back to 1x
        if (newScale <= minScale) {
            Log.d("ZoomPan", "Resetting pan offsets (zoom back to 1x)")
            panOffsetX = 0f
            panOffsetY = 0f
        }
        
        zoomScale = newScale
        Log.d("ZoomPan", "Final state: scale=$zoomScale, panX=$panOffsetX, panY=$panOffsetY")
    }

    // Pre-load string resources
    val apiErrorTitle = stringResource(R.string.api_error)
    val textCopiedMessage = stringResource(R.string.text_copied)
    val invalidDataMessage = stringResource(R.string.invalid_data_format)

    // Update error message in the view model when API error changes
    LaunchedEffect(apiError) {
        apiError?.let { error ->
            val errorMsg = buildString {
                append(apiErrorTitle)
                error.statusCode?.let { code -> append(" ($code)") }
                append(": ${error.message}")
            }
            // The error details will be captured by the NetworkModule interceptor
            // Just need to set the message for display
            errorViewModel.setErrorMessage(errorMsg)
        }
    }

    // Show invalid data error in the error banner
    LaunchedEffect(invalidDataDetected) {
        if (invalidDataDetected) {
            errorViewModel.setErrorMessage(invalidDataMessage)
            // Auto-dismiss after a delay handled by ErrorViewModel
            viewModel.clearInvalidDataFlag()
        }
    }

    // Handle DafMila errors - trigger ErrorBanner when DafMila errors occur
    LaunchedEffect(dafMilaState) {
        if (dafMilaState is MainViewModel.DafMilaState.Error) {
            // Trigger ErrorViewModel to check for DafMila errors and show ErrorBanner
            errorViewModel.updateErrorFromAllSources()
        }
    }

    // Use our new MainScreenLayout with global gesture detection
    MainScreenLayout(
        errorViewModel = errorViewModel,
        onZoomChange = handleZoomAndPan,
        currentZoomScale = zoomScale,
        modifier = Modifier.fillMaxSize()
    ) {
        // Content based on whether a word is selected or not
        if (selectedWord != null) {
            DetailScreen(
                selectedWord = selectedWord,
                dafMilaState = dafMilaState,
                onBackClick = { viewModel.clearWordSelection() },
                onRefreshClick = { viewModel.refreshDafMilaData() }
            )
        } else {
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

                // Search bar with history
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = {
                        viewModel.searchWords()
                    },
                    onRefresh = {
                        viewModel.searchWords(forceRefresh = true)
                    },
                    onClearClick = {
                        showKeyboardAfterScroll = true
                    },
                    searchHistory = searchHistory,
                    onHistoryItemClick = { historyItem ->
                        viewModel.searchWithHistoryItem(historyItem)
                    },
                    onClearHistory = {
                        viewModel.clearSearchHistory()
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

                        ZoomableBox(
                            currentScale = zoomScale,
                            transformOrigin = transformOrigin,
                            panOffsetX = panOffsetX,
                            panOffsetY = panOffsetY,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            WordsList(
                                words = words,
                                onWordClick = { word ->
                                    viewModel.selectWordForDafMila(word)
                                },
                                onCopyText = { text ->
                                    coroutineScope.launch {
                                        scaffoldState.snackbarHostState.showSnackbar(
                                            message = textCopiedMessage,
                                            duration = SnackbarDuration.Short
                                        )
                                    }
                                },
                                showKeyboardAfterScroll = showKeyboardAfterScroll,
                                onScrollStopped = {
                                    if (showKeyboardAfterScroll) {
                                        keyboardController?.show()
                                        showKeyboardAfterScroll = false
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                currentScale = zoomScale
                            )
                        }
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
    
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

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
                onDismiss = { 
                    showHistory = false 
                    // Focus back into search field and show keyboard
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WordsList(
    words: List<HebrewWord>,
    onWordClick: (HebrewWord) -> Unit,
    onCopyText: (String) -> Unit,
    showKeyboardAfterScroll: Boolean,
    onScrollStopped: () -> Unit,
    modifier: Modifier = Modifier,
    currentScale: Float = 1f
) {
    // Get keyboard controller to hide keyboard on scroll
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val isScrolling = listState.isScrollInProgress

    val now = System.currentTimeMillis()

    // Variable to track if we've hidden the keyboard
    var keyboardLastHid by remember { mutableStateOf(now) }

    // Variable to track previous scroll state
    var wasScrollingPreviously by remember { mutableStateOf(false) }

    // Check if list is scrolling to hide keyboard (do not hide too often)
    if (isScrolling && now - keyboardLastHid > 500) {
        // This will run every time the composition recomposes while scrolling
        keyboardController?.hide()
        keyboardLastHid = System.currentTimeMillis()
    }

    // Detect when scrolling stops and call the callback
    LaunchedEffect(isScrolling) {
        if (wasScrollingPreviously && !isScrolling) {
            // Scrolling just stopped
            onScrollStopped()
        }
        wasScrollingPreviously = isScrolling
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        // Disable scrolling when zoomed in - let global pan gestures handle it
        userScrollEnabled = currentScale <= 1f,
        contentPadding = PaddingValues(
            bottom = 200.dp * (currentScale - 1f) // Add extra bottom space when zoomed in
        )
    ) {
        items(words) { word ->
            WordItem(
                word = word,
                onWordClick = onWordClick,
                onCopyText = onCopyText
            )
            Divider()
        }
    }
}

@Composable
fun WordItem(
    word: HebrewWord,
    onWordClick: (HebrewWord) -> Unit,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Format all the content that will be copied when long-pressed
    val copyContent = buildString {
        word.title?.let { append(it); append("\n") } ?: append(
            word.menukad ?: word.menukadWithoutNikud ?: ""
        )
        word.definition?.let { append(it); append("\n") }
        word.itemTypeName?.let { append(it) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clickable { onWordClick(word) },
        horizontalAlignment = Alignment.End,
    ) {
        // Title with nikud and without - use fallbacks if title is null
        val displayTitle = word.title
            ?: word.menukad
            ?: word.menukadWithoutNikud
            ?: word.ktivMale
            ?: stringResource(R.string.no_title_available)

        Text(
            text = displayTitle,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            style = MaterialTheme.typography.body1.copy(
                textAlign = TextAlign.Right,
                textDirection = TextDirection.Rtl
            ),
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Definition
        word.definition?.let { definition ->
            Text(
                text = definition,
                fontSize = 14.sp,
                style = MaterialTheme.typography.body1.copy(
                    textAlign = TextAlign.Right,
                    textDirection = TextDirection.Rtl
                ),
            )

            Spacer(modifier = Modifier.height(4.dp))
        }

        // Item type
        if (!word.itemTypeName.isNullOrBlank()) {
            Text(
                text = word.itemTypeName,
                fontSize = 12.sp,
                color = MaterialTheme.colors.secondary
            )
        }
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.subtitle1
            )
        }
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.error_loading),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }
    }
}