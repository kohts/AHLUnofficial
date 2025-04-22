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
import com.unofficial.ahl.model.HebrewWord
import com.unofficial.ahl.viewmodel.MainViewModel
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

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
    val keyboardController = LocalSoftwareKeyboardController.current
    
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
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
                        
                        WordsList(words = words)
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
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
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
                onSearch = { onSearch() }
            ),
            textStyle = MaterialTheme.typography.body1.copy(
                textAlign = TextAlign.Right
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun WordsList(words: List<HebrewWord>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(words) { word ->
            WordItem(word = word)
            Divider()
        }
    }
}

@Composable
fun WordItem(word: HebrewWord, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
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