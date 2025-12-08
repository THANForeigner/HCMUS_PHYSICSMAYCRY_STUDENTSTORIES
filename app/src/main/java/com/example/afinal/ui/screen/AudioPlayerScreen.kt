package com.example.afinal.ui.screen

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.afinal.data.model.Story
import com.example.afinal.logic.AudioPlayerService
import com.example.afinal.models.AuthViewModel
import com.example.afinal.models.StoryViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
        navController: NavController,
        storyId: String,
        storyViewModel: StoryViewModel,
        authViewModel: AuthViewModel,
        audioService: AudioPlayerService?,
        onStoryLoaded: (Story) -> Unit
) {
  val context = LocalContext.current
  val story = storyViewModel.getStory(storyId)

  LaunchedEffect(story) { story?.let { onStoryLoaded(it) } }

  var isPlaying by remember { mutableStateOf(false) }
  var currentPosition by remember { mutableLongStateOf(0L) }
  var totalDuration by remember { mutableLongStateOf(0L) }

  LaunchedEffect(audioService, story) {
    if (audioService != null && story != null) {
      try {
        if (story.audioUrl.isBlank()) {
          Log.e("AudioPlayerScreen", "Audio URL is empty for story: ${story.id}")
          Toast.makeText(context, "Invalid audio URL", Toast.LENGTH_SHORT).show()
          navController.popBackStack()
          return@LaunchedEffect
        }

        if (audioService.currentStoryId != story.id) {
          val intent =
                  Intent(context, AudioPlayerService::class.java).apply {
                    action = AudioPlayerService.ACTION_PLAY
                    putExtra(AudioPlayerService.EXTRA_AUDIO_URL, story.audioUrl)
                    putExtra(AudioPlayerService.EXTRA_TITLE, story.title.ifBlank { "Untitled" })
                    putExtra(AudioPlayerService.EXTRA_USER, story.user_name.ifBlank { "Unknown" })
                    putExtra(AudioPlayerService.EXTRA_STORY_ID, story.id)
                    putExtra(AudioPlayerService.EXTRA_LOCATION, story.locationName.ifBlank { "" })
                  }
          context.startService(intent)
        }
      } catch (e: Exception) {
        Log.e("AudioPlayerScreen", "Error starting audio service", e)
        Toast.makeText(context, "Error playing audio: ${e.message}", Toast.LENGTH_SHORT).show()
        navController.popBackStack()
      }
    }
  }

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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          CircularProgressIndicator()
          Spacer(modifier = Modifier.height(16.dp))
          Text("Loading audio information...", style = MaterialTheme.typography.bodyMedium)
        }
      }
    } else if (story.audioUrl.isBlank()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Audio not available", style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(8.dp))
          TextButton(onClick = { navController.popBackStack() }) { Text("Go back") }
        }
      }
    } else {
      val scrollState = rememberScrollState()
      Column(
              modifier =
                      Modifier.fillMaxSize()
                              .padding(innerPadding)
                              .padding(horizontal = 24.dp, vertical = 16.dp)
                              .verticalScroll(scrollState), // Make the main content scrollable
              horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
                modifier =
                        Modifier.fillMaxWidth() // Remove weight here
                                .aspectRatio(1f)
                                .shadow(12.dp, RoundedCornerShape(16.dp))
                                .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(16.dp)
                                ),
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

        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
          Text(
                  story.title,
                  style = MaterialTheme.typography.headlineSmall,
                  fontWeight = FontWeight.Bold
          )
          Text(story.user_name, style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
          val sliderValue = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f

          Slider(
                  value = sliderValue.coerceIn(0f, 1f),
                  onValueChange = { newPercent ->
                    val newPos = (newPercent * totalDuration).toLong()
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
        Spacer(modifier = Modifier.height(16.dp))

        ReactionSection(storyViewModel = storyViewModel, authViewModel = authViewModel, storyId = storyId)

        CommentsSection(storyViewModel, authViewModel, storyId)
      }
    }
  }
}

@Composable
fun CommentsSection(storyViewModel: StoryViewModel, authViewModel: AuthViewModel, storyId: String) {
  val comments by storyViewModel.comments
  var newComment by remember { mutableStateOf("") }
  val userEmail = authViewModel.userEmail ?: "Anonymous"

  LaunchedEffect(storyId) { storyViewModel.getComments(storyId) }

  Column(modifier = Modifier.fillMaxWidth()) { // Removed weight here
    Text("Comments", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) { // Constrain LazyColumn height
      items(comments) { comment ->
        Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                colors =
                        CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(comment.userId, fontWeight = FontWeight.Bold)
            Text(comment.comment)
          }
        }
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      OutlinedTextField(
              value = newComment,
              onValueChange = { newComment = it },
              label = { Text("Add a comment") },
              modifier = Modifier.weight(1f)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Button(
              onClick = {
                if (newComment.isNotBlank()) {
                  val comment =
                          com.example.afinal.data.model.Comment(
                                  userId = userEmail,
                                  comment = newComment,
                                  timestamp = System.currentTimeMillis()
                          )
                  storyViewModel.addComment(storyId, comment)
                  newComment = ""
                }
              },
              enabled = newComment.isNotBlank()
      ) { Icon(Icons.Default.Send, contentDescription = "Send") }
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

