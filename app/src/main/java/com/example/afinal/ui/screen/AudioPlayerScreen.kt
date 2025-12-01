package com.example.afinal.ui.screen

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.afinal.StoryViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(navController: NavController, storyId: String) {
    val storyViewModel: StoryViewModel = viewModel()
    // Look for story in both lists
    val story = storyViewModel.getStory(storyId)
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(false) }
    // NEW: Track if player is ready to prevent seeking/playing in wrong state
    var isAudioReady by remember { mutableStateOf(false) }

    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }

    val mediaPlayer = remember { MediaPlayer() }

    LaunchedEffect(story) {
        if (story != null && story.playableUrl.isNotEmpty()) {
            try {
                isAudioReady = false // Reset ready state
                mediaPlayer.reset()
                mediaPlayer.setDataSource(story.playableUrl)
                mediaPlayer.prepareAsync() // Async preparation

                mediaPlayer.setOnPreparedListener { mp ->
                    isAudioReady = true // Mark as ready
                    totalDuration = mp.duration.toLong()
                }

                mediaPlayer.setOnCompletionListener {
                    isPlaying = false
                    // seekTo is valid here because playback just completed
                    it.seekTo(0)
                    currentPosition = 0
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading audio", Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
    }

    // Update slider (Only run if playing AND ready)
    LaunchedEffect(isPlaying, isAudioReady) {
        while (isPlaying && isAudioReady) {
            try {
                currentPosition = mediaPlayer.currentPosition.toLong()
            } catch (e: IllegalStateException) {
                // Ignore errors if player state changes unexpectedly
                isPlaying = false
            }
            delay(500)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(story?.name ?: "Player") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (story == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading or Story not found...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(story.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(story.locationName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (story.user.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Posted by: ${story.user}",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(story.description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)

                Spacer(modifier = Modifier.weight(1f))

                // Slider
                Slider(
                    value = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f,
                    enabled = isAudioReady, // Disable dragging until ready
                    onValueChange = { newPercent ->
                        if (isAudioReady) {
                            val newPosition = (newPercent * totalDuration).toLong()
                            currentPosition = newPosition
                            mediaPlayer.seekTo(newPosition.toInt())
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentPosition))
                    Text(formatTime(totalDuration))
                }

                IconButton(
                    onClick = {
                        if (isAudioReady) {
                            if (isPlaying) {
                                mediaPlayer.pause()
                                isPlaying = false
                            } else {
                                mediaPlayer.start()
                                isPlaying = true
                            }
                        }
                    },
                    // Disable button if audio isn't loaded yet
                    enabled = isAudioReady,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.fillMaxSize(),
                        tint = if (isAudioReady) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                if (!isAudioReady) {
                    Text("Loading audio...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}