package com.example.afinal.ui.screen

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.afinal.data.model.Story
import com.example.afinal.logic.AudioPlayerService
import com.example.afinal.models.AuthViewModel
import com.example.afinal.models.StoryViewModel
import com.example.afinal.ui.theme.AppGradients
import kotlinx.coroutines.delay
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

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
                    Log.e("AudioPlayerScreen", "Audio URL is empty")
                    // Handle error logic
                    return@LaunchedEffect
                }
                if (audioService.currentStoryId != story.id) {
                    val intent = Intent(context, AudioPlayerService::class.java).apply {
                        action = AudioPlayerService.ACTION_PLAY
                        putExtra(AudioPlayerService.EXTRA_AUDIO_URL, story.audioUrl)
                        putExtra(AudioPlayerService.EXTRA_TITLE, story.title.ifBlank { "Untitled" })
                        putExtra(AudioPlayerService.EXTRA_USER, story.user_name.ifBlank { "Unknown" })
                        putExtra(AudioPlayerService.EXTRA_STORY_ID, story.id)
                    }
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("AudioPlayerScreen", "Error starting audio service", e)
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

    // --- MAIN UI ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = AppGradients.audioPlayer)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, bottom = 10.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PLAYING FROM",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Main Library", // Hoặc tên playlist
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(onClick = { /* Menu Action */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                }
            }
        ) { innerPadding ->
            if (story != null && story.audioUrl.isNotBlank()) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // --- 1. COVER ART ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        // Lấy ảnh đầu tiên trong list (nếu có)
                        val coverImage = story.imageUrl

                        if (!coverImage.isNullOrBlank()) {
                            AsyncImage(
                                model = coverImage,
                                contentDescription = "Album Art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = "Cover Art Placeholder",
                                modifier = Modifier.size(120.dp),
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // --- 2. TITLE & AUTHOR & HEART ICON ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = story.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "by ${if (story.user_name.isBlank()) "Anonymous" else story.user_name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (story.description.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 100.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = story.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // --- 3. SLIDER & CONTROLS ---
                    PlayerControlsSection(
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        totalDuration = totalDuration,
                        onSeek = { newPos -> audioService?.seekTo(newPos) },
                        onPlayPause = {
                            if (isPlaying) audioService?.pauseAudio() else audioService?.resumeAudio()
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- 4. REACTION BOX (Glassmorphism) ---
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(vertical = 16.dp, horizontal = 8.dp)
                    ) {
                        ReactionSection(storyViewModel, authViewModel, storyId)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- 5. COMMENT SECTION (Darker Box) ---
                    CommentsSectionStyle(storyViewModel, authViewModel, storyId)

                    Spacer(modifier = Modifier.height(30.dp)) // Bottom padding
                }
            } else {
                // Loading / Error State
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
    }
}

@Composable
fun PlayerControlsSection(
    isPlaying: Boolean,
    currentPosition: Long,
    totalDuration: Long,
    onSeek: (Int) -> Unit,
    onPlayPause: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Slider
        val sliderValue = if (totalDuration > 0) currentPosition.toFloat() / totalDuration else 0f
        Slider(
            value = sliderValue.coerceIn(0f, 1f),
            onValueChange = { percent ->
                onSeek((percent * totalDuration).toInt())
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Time Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatTime(currentPosition), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            Text(formatTime(totalDuration), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons: Shuffle, Prev, Play, Next, Repeat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Shuffle, "Shuffle", tint = Color.White, modifier = Modifier.size(24.dp))

            Icon(Icons.Default.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(36.dp))

            // Nút Play Trắng Lớn
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onPlayPause() }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color(0xFF8E24AA),
                    modifier = Modifier.size(36.dp)
                )
            }

            Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(36.dp))

            Icon(Icons.Default.Repeat, "Repeat", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun CommentsSectionStyle(storyViewModel: StoryViewModel, authViewModel: AuthViewModel, storyId: String) {
    val comments by storyViewModel.comments
    var newComment by remember { mutableStateOf("") }
    val userEmail = authViewModel.userEmail ?: "Anonymous"

    LaunchedEffect(storyId) { storyViewModel.getComments(storyId) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.Black.copy(alpha = 0.2f), // Nền tối hơn cho phần comment
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ChatBubbleOutline, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Comments (${comments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White) // Icon thu gọn giả lập
        }

        // Input Field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = newComment,
                onValueChange = { newComment = it },
                placeholder = { Text("Add a comment...", color = Color.White.copy(alpha = 0.5f)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            )

            // send button
            IconButton(
                onClick = {
                    if (newComment.isNotBlank()) {
                        val comment = com.example.afinal.data.model.Comment(
                            userId = userEmail,
                            comment = newComment,
                            timestamp = System.currentTimeMillis()
                        )
                        storyViewModel.addComment(storyId, comment)
                        newComment = ""
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE040FB), CircleShape) // Màu hồng tím nổi bật
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List Comments
        if (comments.isNotEmpty()) {
            if (comments.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    comments.forEach { comment ->
                        Row(modifier = Modifier.padding(vertical = 8.dp)) {
                            // Avatar giả
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFFE1BEE7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = comment.userId.take(1).uppercase(),
                                    color = Color(0xFF4A148C),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = comment.userId,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = comment.comment,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Divider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                    }
                }
            } else {
                Text(
                    "No comments yet. Be the first to share!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}