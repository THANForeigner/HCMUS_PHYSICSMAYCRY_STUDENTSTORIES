package com.example.afinal.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.afinal.logic.AudioPlayerService
import com.example.afinal.models.StoryModel
import com.example.afinal.models.StoryViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    navController: NavController,
    storyId: String,
    storyViewModel: StoryViewModel,
    audioService: AudioPlayerService?,
    onStoryLoaded: (StoryModel) -> Unit
) {
    val context = LocalContext.current
    val story = storyViewModel.getStory(storyId)

    // Notify parent (MainActivity) which story is selected (for MiniPlayer)
    LaunchedEffect(story) {
        story?.let { onStoryLoaded(it) }
    }

    // --- UI State ---
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(0L) }

    // --- 1. LOGIC: Load & Play Audio ---
    // If the service is bound and the requested story is NOT the one currently playing, start it.
    LaunchedEffect(audioService, story) {
        if (audioService != null && story != null) {
            // Check if we need to change the track
            if (audioService.currentStoryId != story.id) {
                val intent = Intent(context, AudioPlayerService::class.java).apply {
                    action = AudioPlayerService.ACTION_PLAY
                    putExtra(AudioPlayerService.EXTRA_AUDIO_URL, story.playableUrl)
                    putExtra(AudioPlayerService.EXTRA_TITLE, story.name)
                    putExtra(AudioPlayerService.EXTRA_USER, story.user)
                    putExtra(AudioPlayerService.EXTRA_STORY_ID, story.id)
                    putExtra(AudioPlayerService.EXTRA_LOCATION, story.locationName)
                }
                // Send Intent to Service to update metadata and start playing
                context.startService(intent)
            }
        }
    }

    // --- 2. LOGIC: Update UI Loop ---
    // Poll the service every 500ms to update the slider and timestamps
    LaunchedEffect(audioService) {
        while (true) {
            if (audioService != null) {
                isPlaying = audioService.isPlaying
                currentPosition = audioService.getCurrentPosition()
                totalDuration = audioService.getDuration()
            }
            delay(500)
        }
    }

    // --- UI SETUP ---
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("Now Playing", style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minimize")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        if (story == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. IMAGE AREA
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Cover Art",
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 2. TEXT INFO
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                    Text(story.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(story.user, style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 3. SLIDER
                Column(modifier = Modifier.fillMaxWidth()) {
                    val sliderValue = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f

                    Slider(
                        value = sliderValue.coerceIn(0f, 1f),
                        onValueChange = { newPercent ->
                            val newPos = (newPercent * totalDuration).toLong()
                            // Update local state immediately for smooth dragging
                            currentPosition = newPos
                            audioService?.seekTo(newPos.toInt())
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(currentPosition), style = MaterialTheme.typography.labelMedium)
                        Text(formatTime(totalDuration), style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. CONTROLS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.Default.SkipPrevious, null, Modifier.size(32.dp))
                    }

                    FilledIconButton(
                        onClick = {
                            if (isPlaying) {
                                audioService?.pauseAudio()
                            } else {
                                audioService?.resumeAudio()
                            }
                        },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.Default.SkipNext, null, Modifier.size(32.dp))
                    }
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