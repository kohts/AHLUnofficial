package com.unofficial.ahl.ui

import android.text.Html
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.unofficial.ahl.R
import com.unofficial.ahl.model.HebrewWord
import com.unofficial.ahl.viewmodel.MainViewModel

/**
 * Composable for the word details screen
 */
@Composable
fun DetailScreen(
    selectedWord: HebrewWord?,
    detailsState: MainViewModel.WordDetailsState,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = selectedWord?.keyword ?: "", 
                        textAlign = TextAlign.Center
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End
            ) {
                // Title
                selectedWord?.let { word ->
                    Text(
                        text = word.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        style = MaterialTheme.typography.h5.copy(
                            textAlign = TextAlign.Right,
                            textDirection = TextDirection.Rtl
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Details content based on state
                    when (detailsState) {
                        is MainViewModel.WordDetailsState.Initial -> {
                            // Initial state, show nothing or instructions
                        }
                        is MainViewModel.WordDetailsState.Loading -> {
                            LoadingIndicator()
                        }
                        is MainViewModel.WordDetailsState.Success -> {
                            // Display the HTML content using AndroidView with TextView
                            AndroidView(
                                factory = { context ->
                                    TextView(context).apply {
                                        textAlignment = TextView.TEXT_ALIGNMENT_VIEW_END
                                    }
                                },
                                update = { textView ->
                                    // Set the text using HTML.fromHtml to parse HTML
                                    textView.text = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                        Html.fromHtml(detailsState.detailsHtml, Html.FROM_HTML_MODE_COMPACT)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        Html.fromHtml(detailsState.detailsHtml)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is MainViewModel.WordDetailsState.Error -> {
                            Text(
                                text = detailsState.message,
                                color = MaterialTheme.colors.error,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
} 