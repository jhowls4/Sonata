package com.example.sonata.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sonata.data.MediaState
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun MediaProgressBar(
    mediaState: MediaState,
    modifier: Modifier = Modifier
) {
    var currentPosition by remember(mediaState.progress, mediaState.lastUpdated) {
        mutableLongStateOf(mediaState.progress)
    }

    // Smoothly increment progress while playing
    LaunchedEffect(mediaState.isPlaying, mediaState.progress) {
        if (mediaState.isPlaying) {
            val startUpdate = System.currentTimeMillis()
            val startProgress = mediaState.progress
            while (true) {
                val elapsed = System.currentTimeMillis() - startUpdate
                currentPosition = (startProgress + elapsed).coerceAtMost(mediaState.duration)
                delay(1000)
            }
        }
    }

    val progress = if (mediaState.duration > 0) currentPosition.toFloat() / mediaState.duration else 0f
    val remainingMillis = (mediaState.duration - currentPosition).coerceAtLeast(0)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMillis(currentPosition),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "-${formatMillis(remainingMillis)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
