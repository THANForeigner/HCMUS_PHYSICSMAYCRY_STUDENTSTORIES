package com.example.afinal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.afinal.models.LocationModel
import com.example.afinal.models.StoryViewModel
import com.example.afinal.ui.component.StoryCard
import com.example.afinal.ui.theme.AppGradients

@Composable
fun HomeScreen(
    storyViewModel: StoryViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToAudios: () -> Unit,
    onStoryClick: (String) -> Unit
) {
    val trendingStories by storyViewModel.topTrendingStories
    val hotLocations by storyViewModel.hotLocations
    val allStories by storyViewModel.allStories
    val recommendedStories by storyViewModel.recommendedStories

    // Config News
    val targetNewsTags = listOf("Important Announcement", "Facilities information", "Warning")
    val newsStories = allStories.filter { story ->
        story.tags.any { tag -> targetNewsTags.contains(tag) }
    }.sortedByDescending { it.created_at }.take(5)

    // Config drl hunting
    val trainingScoreHunting = listOf("Social Activities", "Seminar", "After-class Activities",
        "Volunteer Campaigns", "Online Activities")
    val trainingStoriesHunting = allStories.filter { story ->
        story.tags.any { tag -> trainingScoreHunting.contains(tag) }
    }.sortedByDescending { it.created_at }.take(5)

    // 3. Gradients & Colors
    val fireGradient = Brush.linearGradient(listOf(Color(0xFFFF8008), Color(0xFFFFC837)))
    val newsGradient = Brush.linearGradient(listOf(Color(0xFF00C6FF), Color(0xFF0072FF)))
    val trainingGradient = Brush.linearGradient(listOf(Color(0xFF00FFC6), Color(0xFF00FF72)))
    val locationGradient = Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))
    val recommendGradient = Brush.linearGradient(listOf(Color(0xFF43e97b), Color(0xFF38f9d7)))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // LAYER 1: Header Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(brush = AppGradients.homeScreen)
        )

        // LAYER 2: Scrollable Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // A. Header Text
            item {
                Column(
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 60.dp, bottom = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Headphones, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Student Stories", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Discover the hidden tales of your school", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f))
                }
            }

            // B. Main Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HomeMenuCard("Explore Map", Icons.Default.Place, AppGradients.mapScreen, Modifier.weight(1f), onNavigateToMap)
                    HomeMenuCard("Browse Stories", Icons.Default.Headphones, AppGradients.audioScreen, Modifier.weight(1f), onNavigateToAudios)
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }

            // C. Main Content Area (White Sheet)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(Color.White)
                ) {
                    Column(modifier = Modifier.padding(vertical = 24.dp)) {

                        // --- SECTION 1: LATEST UPDATES ---
                        SectionHeader(title = "Latest Updates", icon = Icons.Default.Campaign, gradient = newsGradient)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (newsStories.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(newsStories) { story ->
                                    Box(modifier = Modifier.fillParentMaxWidth(0.9f)) {
                                        StoryCard(
                                            story = story,
                                            onClick = { onStoryClick(story.id) }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // --- SECTION 2: RECOMMENDED FOR YOU ---
                        SectionHeader(title = "Recommended For You", icon = Icons.Default.Star, gradient = recommendGradient)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (recommendedStories.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(recommendedStories) { story ->
                                    Box(modifier = Modifier.fillParentMaxWidth(0.9f)) {
                                        StoryCard(
                                            story = story,
                                            onClick = { onStoryClick(story.id) }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // --- SECTION 3: TRENDING STORIES ---
                        SectionHeader(title = "Trending Stories", icon = Icons.Default.LocalFireDepartment, gradient = fireGradient)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (trendingStories.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(trendingStories) { story ->
                                    Box(modifier = Modifier.fillParentMaxWidth(0.9f)) {
                                        StoryCard(
                                            story = story,
                                            onClick = { onStoryClick(story.id) }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }

                        // --- SECTION 4: HOT LOCATIONS ---
                        SectionHeader(title = "Hot Locations", icon = Icons.Default.Star, gradient = locationGradient)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (hotLocations.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(hotLocations) { location ->
                                    HotLocationCard(
                                        location = location,
                                        storyCount = storyViewModel.getStoryCountForLocation(location.id),
                                        onClick = { onNavigateToMap() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun SectionHeader(title: String, icon: ImageVector, gradient: Brush) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(brush = gradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = Color(0xFF2D2D2D), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HotLocationCard(
    location: LocationModel,
    storyCount: Int,
    onClick: () -> Unit
) {
    val isIndoor = location.type == "indoor"
    val icon = if (isIndoor) Icons.Default.Apartment else Icons.Default.Park
    val iconColor = if (isIndoor) Color(0xFF6A11CB) else Color(0xFF2575FC)
    val bgColor = if (isIndoor) Color(0xFFF3E5F5) else Color(0xFFE3F2FD) // Nhạt hơn để làm nền

    Card(
        modifier = Modifier
            .width(140.dp)
            .height(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon Circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }

            Column {
                Text(
                    text = location.locationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$storyCount stories",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

// Giữ lại HomeMenuCard cũ
@Composable
fun HomeMenuCard(
    title: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), spotColor = Color.LightGray.copy(alpha = 0.5f))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(brush = gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2D2D2D))
        }
    }
}