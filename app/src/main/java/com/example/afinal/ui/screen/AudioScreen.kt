package com.example.afinal.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.afinal.logic.LocationGPS
import com.example.afinal.models.LocationViewModel
import com.example.afinal.data.model.Story
import com.example.afinal.models.StoryViewModel
import com.example.afinal.navigation.Routes
import com.example.afinal.ultis.DistanceCalculator
import com.example.afinal.ultis.IndoorDetector
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.google.firebase.Timestamp
import java.util.Date

val STORY_TAGS = listOf(
    "Romance", "Pet", "Mysteries", "Facilities information",
    "Health", "Food and drink", "Social and communities",
    "Personal experience", "Warning", "Study Hacks",
    "Library Vibes", "Confessions", "Motivation",
    "Gaming", "Burnout", "Emotional support"
)

@Composable
fun AudiosScreen(
    navController: NavController,
    storyViewModel: StoryViewModel,
    locationViewModel: LocationViewModel
) {
    val context = LocalContext.current

    var showAddLocationDialog by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    val currentLocationId by storyViewModel.currentLocationId
    val currentLocation by storyViewModel.currentLocation
    val isIndoor by storyViewModel.isIndoor
    val currentFloor by storyViewModel.currentFloor
    val currentStories by storyViewModel.currentStories

    // Filter stories based on selected tag
    val filteredStories = remember(currentStories, selectedTag) {
        if (selectedTag == null) {
            currentStories
        } else {
            currentStories.filter { it.tags.contains(selectedTag) }
        }
    }

    val userLocation by locationViewModel.location
    val allLocations by storyViewModel.locations

    val indoorDetector = remember { IndoorDetector(context) }
    val isUserIndoor by indoorDetector.observeIndoorStatus()
        .collectAsState(initial = storyViewModel.isIndoor.value)

    LaunchedEffect(Unit) {
        LocationGPS(context).requestLocationUpdate(locationViewModel)
    }

    LaunchedEffect(userLocation, allLocations, isUserIndoor) {
        if (!isUserIndoor && userLocation != null && allLocations.isNotEmpty()) {
            val currentLoc = DistanceCalculator.findNearestLocation(
                userLat = userLocation!!.latitude,
                userLng = userLocation!!.longitude,
                candidates = allLocations
            )
            if (currentLoc != null) {
                if (currentLocationId != currentLoc.id) {
                    selectedTag = null
                    storyViewModel.fetchStoriesForLocation(currentLoc.id)
                }
            } else {
                storyViewModel.clearLocation()
            }
        }
    }

    LaunchedEffect(isUserIndoor) {
        if (isUserIndoor != isIndoor) {
            storyViewModel.setIndoorStatus(isUserIndoor)
            currentLocationId?.let { locId ->
                selectedTag = null
                storyViewModel.fetchStoriesForLocation(locId, currentFloor, forceRefresh = true)
            }
        }
    }

    val showFloorButton = isIndoor && (currentLocation?.type == "indoor")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Stories",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            if (currentLocationId == null) {
                EmptyLocationView(
                    isUserIndoor = isUserIndoor,
                    hasGpsSignal = userLocation != null,
                    onAddLocationClick = { showAddLocationDialog = true }
                )
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    LocationHeader(
                        locationId = currentLocationId!!,
                        isIndoor = isIndoor,
                        floor = currentFloor
                    )

                    TagFilterBar(
                        tags = STORY_TAGS,
                        selectedTag = selectedTag,
                        onTagSelected = { tag ->
                            selectedTag = if (selectedTag == tag) null else tag
                        }
                    )

                    StoryList(
                        stories = filteredStories,
                        onAddStoryClick = { navController.navigate(Routes.ADD_POST) },
                        onStoryClick = { story -> navController.navigate("${Routes.AUDIO_PLAYER}/${story.id}") }
                    )
                }
            }
        }

        if (currentLocationId != null && showFloorButton) {
            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)) {
                FloorSelector(
                    currentFloor = currentFloor,
                    onFloorSelected = { storyViewModel.setCurrentFloor(it) }
                )
            }
        }

        if (showAddLocationDialog) {
            AddLocationDialog(
                onDismiss = { showAddLocationDialog = false },
                onConfirm = { name ->
                    if (userLocation != null) {
                        storyViewModel.addLocation(
                            userLocation!!.latitude,
                            userLocation!!.longitude,
                            name,
                            if (isIndoor) "indoor" else "outdoor"
                        )
                    }
                    showAddLocationDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagFilterBar(
    tags: List<String>,
    selectedTag: String?,
    onTagSelected: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                                Text(
                    text = if (selectedTag == null) "Filter by topics (All)" else "Filtered by: $selectedTag",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selectedTag != null) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }

            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand tags",
                    tint = Color.Gray
                )
            }
        }

        Divider(modifier = Modifier.padding(horizontal = 16.dp).alpha(0.5f))
        Spacer(modifier = Modifier.height(8.dp))

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTag == null,
                    onClick = { onTagSelected("") },
                    label = { Text("All") },
                    leadingIcon = if (selectedTag == null) {
                        { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                    } else null
                )

                tags.forEach { tag ->
                    val isSelected = selectedTag == tag
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTagSelected(tag) },
                        label = { Text(tag) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        if (!isExpanded) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTag == null,
                        onClick = { onTagSelected("") },
                        label = { Text("All") },
                        leadingIcon = if (selectedTag == null) {
                            { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                        } else null
                    )
                }

                items(tags) { tag ->
                    val isSelected = selectedTag == tag
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTagSelected(tag) },
                        label = { Text(tag) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun EmptyLocationView(isUserIndoor: Boolean, hasGpsSignal: Boolean, onAddLocationClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Landscape, null, Modifier.size(64.dp), tint = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isUserIndoor) "Indoor detected. Waiting..." else "Exploring nearby...",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            if (isUserIndoor) Text("(Position Locked)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            if (hasGpsSignal) {
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(onClick = onAddLocationClick, modifier = Modifier.size(48.dp)) { Icon(Icons.Filled.Add, "Add") }
            }
        }
    }
}

@Composable
fun LocationHeader(locationId: String, isIndoor: Boolean, floor: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isIndoor) Icons.Default.Business else Icons.Default.Landscape, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isIndoor) "Inside: $locationId" else "Outdoor: $locationId",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (isIndoor) {
                    Text("GPS Locked • Floor $floor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun StoryList(stories: List<Story>, onAddStoryClick: () -> Unit, onStoryClick: (Story) -> Unit) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                onClick = onAddStoryClick,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Add, "Add Story", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share your story here", fontWeight = FontWeight.Medium)
                }
            }
        }

        if (stories.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No stories found with this tag.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        items(stories) { story ->
            StoryCard(story = story, onClick = { onStoryClick(story) })
        }
    }
}

@Composable
fun FloorSelector(currentFloor: Int, onFloorSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    FloatingActionButton(onClick = { expanded = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Icon(Icons.Default.Layers, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Floor $currentFloor")
                    }
                }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        (1..11).forEach { floor ->
            DropdownMenuItem(text = { Text("Floor $floor") }, onClick = { onFloorSelected(floor); expanded = false })
        }
    }
}

@Composable
fun AddLocationDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Location") },
        text = { TextField(value = name, onValueChange = { name = it }, label = { Text("Location Name") }, singleLine = true) },
        confirmButton = { Button(onClick = { if (name.isNotBlank()) onConfirm(name) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StoryCard(story: Story, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),

        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = story.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (story.is_finished) Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, Modifier.size(14.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    story.user_name.ifBlank { "Unknown" },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(Icons.Default.CalendarToday, null, Modifier.size(12.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = story.created_at.toNormalDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = story.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    story.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Icon(
                    Icons.Default.PlayCircleOutline,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

fun Timestamp?.toNormalDate(): String {
    if (this == null) return "Updating..."
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(this.toDate())
    }