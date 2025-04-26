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
import com.unofficial.ahl.ui.DetailScreen
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AhlApp(viewModel)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AhlApp(viewModel: MainViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val apiError by viewModel.apiError.collectAsState()
    val selectedWord by viewModel.selectedWord.collectAsState()
    val wordDetailsState by viewModel.wordDetailsState.collectAsState()
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
            // Show detail screen if a word is selected, otherwise show main screen
            if (selectedWord != null) {
                DetailScreen(
                    selectedWord = selectedWord,
                    detailsState = wordDetailsState,
                    onBackClick = { viewModel.clearWordSelection() }
                )
            } else {
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
                                    onWordClick = { word ->
                                        viewModel.selectWord(word)
                                    },
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
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {

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

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch() }
                ),
                textStyle = MaterialTheme.typography.body1.copy(
                    textAlign = TextAlign.Right,
                    textDirection = TextDirection.Rtl
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
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
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun WordsList(
    words: List<HebrewWord>,
    onWordClick: (HebrewWord) -> Unit,
    onCopyText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Get keyboard controller to hide keyboard on scroll
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    val now = System.currentTimeMillis()

    // Variable to track if we've hidden the keyboard
    var keyboardLastHid by remember { mutableStateOf(now) }

    // Check if list is scrolling to hide keyboard (do not hide too often)
    if (listState.isScrollInProgress && now - keyboardLastHid > 500) {
        // This will run every time the composition recomposes while scrolling
        keyboardController?.hide()
        keyboardLastHid = System.currentTimeMillis()
    }
    
    LazyColumn(
        modifier = modifier,
        state = listState
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
                    onTap = { onWordClick(word) },
                    onLongPress = {
                        // Copy to clipboard
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("hebrew_word", copyContent)
                        clipboardManager.setPrimaryClip(clipData)
                        
                        // Notify through callback
                        onCopyText(copyContent)
                    }
                )
            },
        horizontalAlignment = Alignment.End,
    ) {
        // Title with nikud and without
        Text(
            text = word.title,
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
        Text(
            text = word.itemTypeName,
            fontSize = 12.sp,
            color = MaterialTheme.colors.secondary
        )
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(top = 16.dp),
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

@Composable
fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colors.error,
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = message,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.dismiss),
                    tint = Color.White
                )
            }
        }
    }
}