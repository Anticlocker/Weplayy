package com.sundram.weplay.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.TextBody
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted

@Composable
fun <T> StateContent(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit = { LoadingState(modifier) },
    empty: @Composable (String?) -> Unit = { EmptyState(it, modifier) },
    error: @Composable (String, Boolean, (() -> Unit)?) -> Unit = { message, retryable, onRetry ->
        ErrorState(message, retryable, onRetry, modifier)
    },
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    when (state) {
        UiState.Loading -> loading()
        is UiState.Success -> content(state.data)
        is UiState.Empty -> empty(state.message)
        is UiState.Error -> error(state.message, state.retryable, onRetry)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = CyanBrand, strokeWidth = 2.dp)
    }
}

@Composable
fun EmptyState(message: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Nothing here yet", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(message, color = TextBody, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    retryable: Boolean = true,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Could not load", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (retryable && onRetry != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("Retry", color = CyanBrand, fontWeight = FontWeight.Bold) }
        }
    }
}
