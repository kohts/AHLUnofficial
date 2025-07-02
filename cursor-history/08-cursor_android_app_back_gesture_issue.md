# Android App Back Gesture Issue
_Exported on 6/28/2025 at 00:33:56 GMT+3 from Cursor (1.1.5)_

---

**User**

On the detail screen when I make android back gesture, the app closes instead of going back to the search screen. Could you try to fix it?

---

**Cursor**

Let me check how the app's navigation is set up to fix the back gesture issue.

After analyzing the code, I can see the issue. The app is using a simple state-based navigation approach where the `selectedWord` state determines whether to show the detail screen or the main screen. 

The problem is that the system back gesture isn't being handled. When on the detail screen, pressing the back button in the top app bar works because it calls `viewModel.clearWordSelection()`, but the system back gesture isn't connected to this behavior.

Let's fix this by adding a `BackHandler` to handle the Android system back gesture:

```plaintext
package com.unofficial.ahl.ui

import android.text.Html
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
                }
            )
        }
    ) { paddingValues ->
// ... existing code ...
```

I've fixed the issue by adding a `BackHandler` to the `DetailScreen` composable. This component from `androidx.activity.compose` intercepts the system back button press and back gesture, executing the provided lambda when triggered.

Now when you use the back gesture on the detail screen, it will call the same `onBackClick` function that's used by the back arrow in the top app bar, returning you to the search screen instead of closing the app.

This approach maintains consistency between the top app bar back button and the system back gesture, ensuring both perform the same navigation action.

