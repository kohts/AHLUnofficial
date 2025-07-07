package com.unofficial.ahl.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unofficial.ahl.R
import com.unofficial.ahl.model.HebrewWord
import com.unofficial.ahl.model.DafMilaResponse
import com.unofficial.ahl.model.MillonHaHoveItem
import com.unofficial.ahl.viewmodel.MainViewModel

/**
 * Composable for the word details screen with structured DafMila data
 */
@Composable
fun DetailScreen(
    selectedWord: HebrewWord?,
    dafMilaState: MainViewModel.DafMilaState,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle system back button/gesture
    BackHandler {
        onBackClick()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = selectedWord?.keyword ?: selectedWord?.menukad ?: 
                              selectedWord?.menukadWithoutNikud ?: 
                              stringResource(R.string.no_title_available), 
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
                },
                actions = {
                    IconButton(onClick = onRefreshClick) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
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
                when (dafMilaState) {
                    is MainViewModel.DafMilaState.Initial -> {
                        // Show selected word basic info
                        selectedWord?.let { word ->
                            BasicWordInfo(word)
                        }
                    }
                    is MainViewModel.DafMilaState.Loading -> {
                        selectedWord?.let { word ->
                            BasicWordInfo(word)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        LoadingIndicator()
                    }
                    is MainViewModel.DafMilaState.Success -> {
                        DafMilaContent(dafMilaState.data)
                    }
                    is MainViewModel.DafMilaState.Error -> {
                        selectedWord?.let { word ->
                            BasicWordInfo(word)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Text(
                            text = dafMilaState.message,
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.body1.copy(
                                textAlign = TextAlign.Right,
                                textDirection = TextDirection.Rtl
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BasicWordInfo(word: HebrewWord) {
    val displayTitle = word.title 
        ?: word.menukad
        ?: word.menukadWithoutNikud
        ?: word.ktivMale
        ?: stringResource(R.string.no_title_available)
    
    Text(
        text = displayTitle,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        style = MaterialTheme.typography.h5.copy(
            textAlign = TextAlign.Right,
            textDirection = TextDirection.Rtl
        ),
        modifier = Modifier.fillMaxWidth()
    )
    
    // Show basic definition if available
    word.definition?.let { definition ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = definition,
            fontSize = 16.sp,
            style = MaterialTheme.typography.body1.copy(
                textAlign = TextAlign.Right,
                textDirection = TextDirection.Rtl
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DafMilaContent(data: DafMilaResponse) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        // 1. Title from Koteret (bigger font)
        data.koteret?.let { title ->
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                style = MaterialTheme.typography.h4.copy(
                    textAlign = TextAlign.Right,
                    textDirection = TextDirection.Rtl
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // 2. במילון Section
        data.millonHaHoveList?.firstOrNull()?.let { millonItem ->
            BemillonSection(millonItem)
        }
    }
}

@Composable
fun BemillonSection(millonItem: MillonHaHoveItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        // Section title: במילון
        Text(
            text = "במילון",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            style = MaterialTheme.typography.h6.copy(
                textAlign = TextAlign.Right,
                textDirection = TextDirection.Rtl
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 3.1) Subsection without subtitle - SafaTxt
        millonItem.safaTxt?.takeIf { it.isNotBlank() }?.let { safaTxt ->
            Text(
                text = safaTxt,
                fontSize = 16.sp,
                style = MaterialTheme.typography.body1.copy(
                    textAlign = TextAlign.Right,
                    textDirection = TextDirection.Rtl
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // 3.2) Key-value list
        KeyValueList(millonItem)
        
        // 3.3) הגדרה subsection
        millonItem.hagdara?.takeIf { it.isNotBlank() }?.let { hagdara ->
            Spacer(modifier = Modifier.height(16.dp))
            HagdaraSection(hagdara)
        }
    }
}

@Composable
fun KeyValueList(millonItem: MillonHaHoveItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        // מין - MinDikdukiTxt
        millonItem.minDikdukiTxt?.takeIf { it.isNotBlank() }?.let { value ->
            KeyValueRow("מין", value)
        }
        
        // שורש - ShoreshTxt
        millonItem.shoreshTxt?.takeIf { it.isNotBlank() }?.let { value ->
            KeyValueRow("שורש", value)
        }
        
        // NetiyyotTtl - NetiyyotTxt
        millonItem.netiyyotTtl?.takeIf { it.isNotBlank() }?.let { key ->
            millonItem.netiyyotTxt?.takeIf { it.isNotBlank() }?.let { value ->
                KeyValueRow(key, value)
            }
        }
    }
}

@Composable
fun KeyValueRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
            style = MaterialTheme.typography.body2.copy(
                textDirection = TextDirection.Rtl
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$key:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.body2.copy(
                textDirection = TextDirection.Rtl
            )
        )
    }
}

@Composable
fun HagdaraSection(hagdara: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        // Subsection title: הגדרה
        Text(
            text = "הגדרה",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            style = MaterialTheme.typography.h6.copy(
                textAlign = TextAlign.Right,
                textDirection = TextDirection.Rtl
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Parse pipe-separated values and display as bullet list
        // RTL: rightmost entry goes first in the list
        val definitions = hagdara.split("|").reversed().map { it.trim() }.filter { it.isNotBlank() }
        
        definitions.forEach { definition ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = definition,
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.body2.copy(
                        textDirection = TextDirection.Rtl
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "•",
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.body2
                )
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