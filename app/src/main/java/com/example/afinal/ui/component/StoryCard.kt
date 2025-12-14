package com.example.afinal.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ModeComment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.afinal.data.model.Story
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun StoryCard(
    story: Story,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProcessing: Boolean = !story.isFinished

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
            .clickable(enabled = !isProcessing) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isProcessing) {
                ProcessingStateView()
            } else {
                CompletedStateView(story)
            }
        }
    }
}

@Composable
private fun ProcessingStateView() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "AI is writing your story...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun CompletedStateView(story: Story) {
    // --- ROW 1: Title + Date Badge ---
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Title
        Text(
            text = story.title.ifBlank { "Untitled Story" },
            style = MaterialTheme.typography.titleMedium, // Font size 16-18sp
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D2D2D), // Dark Gray
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )

        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = story.created_at.toNormalDate(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // --- ROW 2: Author ---
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = story.user_name.ifBlank { "Anonymous" },
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // --- ROW 3: Description ---
    Text(
        text = story.description,
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF757575), // Gray 600
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 20.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    // --- ROW 4: Footer (Tags + Stats + Play Button) ---
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Tags & Stats
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Tags
            if (story.tags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(story.tags) { tag ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF5F5F5), // Gray 100
                        ) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF616161),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Stats (Likes / Comments)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconWithCount(icon = Icons.Outlined.FavoriteBorder, count = story.reactionsCount)
                Spacer(modifier = Modifier.width(16.dp))
                IconWithCount(icon = Icons.Outlined.ModeComment, count = story.commentsCount)
            }
        }
    }
}

@Composable
fun IconWithCount(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF9E9E9E), // Gray icon
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF757575)
        )
    }
}

// Extension function giữ nguyên
fun Timestamp?.toNormalDate(): String {
    if (this == null) return "Just now"
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(this.toDate())
}