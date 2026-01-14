package io.github.howshous.ui.components

import android.os.SystemClock
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun DebouncedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    debounceMs: Long = 500,
    content: @Composable () -> Unit
) {
    val lastClickMs = remember { mutableStateOf(0L) }

    IconButton(
        onClick = {
            val now = SystemClock.elapsedRealtime()
            if (now - lastClickMs.value < debounceMs) return@IconButton
            lastClickMs.value = now
            onClick()
        },
        modifier = modifier,
        enabled = enabled
    ) {
        content()
    }
}
