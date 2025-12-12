package com.example.afinal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.afinal.models.StoryViewModel
import com.example.afinal.models.LocationViewModel
import com.example.afinal.navigation.Routes
import com.example.afinal.ui.component.StoryCard
import com.example.afinal.ui.component.*
import com.example.afinal.ui.theme.AppGradients

@Composable
fun HomeScreen(
    storyViewModel: StoryViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToAudios: () -> Unit,
    onStoryClick: (String) -> Unit
) {
    val trendingStories by storyViewModel.topTrendingStories

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // --- LAYER 1: GRADIENT BACKGROUND ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(brush = AppGradients.Purple)
        )

        // --- LAYER 2: SCROLLABLE CONTENT ---
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // A. Header Text & Welcome
            item {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 60.dp, bottom = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Title
                    Text(
                        text = "Student Stories",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Slogan
                    Text(
                        text = "Discover the hidden tales of your school",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // B. Action Buttons (Explore Map / Browse Stories)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. MAP
                    DashboardCardButton(
                        title = "Explore Map",
                        icon = Icons.Default.Map,
                        iconColor = Color(0xFF00BFA5), // Teal
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMap
                    )

                    // 2. STORIES
                    DashboardCardButton(
                        title = "Browse Stories",
                        icon = Icons.Default.Headphones,
                        iconColor = Color(0xFFFF4081), // Pink
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAudios
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // C. White Sheet Container
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(Color.White)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Title Section: Trending
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(AppGradients.Orange),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Trending Stories",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF2D2D2D)
                            )
                        }

                        // Loading State
                        if (trendingStories.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }

            // D. List Items
            items(trendingStories) { story ->
                // Wrapper Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 24.dp)
                ) {
                    StoryCard(
                        story = story,
                        onClick = {
                            onStoryClick(story.id)
                        }
                    )
                }
            }

            // E. Footer Filler
            item {
                Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(Color.White))
            }
        }
    }
}