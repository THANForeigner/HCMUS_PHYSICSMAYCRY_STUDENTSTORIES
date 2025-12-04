package com.example.afinal.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.afinal.models.LocationViewModel
import com.example.afinal.models.StoryViewModel
import com.example.afinal.navigation.Routes
import com.example.afinal.ultis.DistanceCalculator
import com.example.afinal.ultis.IndoorDetector

@Composable
fun AudiosScreen(
    navController: NavController,
    storyViewModel: StoryViewModel,
    locationViewModel: LocationViewModel
) {
    val context = LocalContext.current

    var showAddLocationDialog by remember { mutableStateOf(false) }
    var newLocationName by remember { mutableStateOf("") }

    // 1. Collect State
    val currentLocationId by storyViewModel.currentLocationId
    val currentLocation by storyViewModel.currentLocation
    val isIndoor by storyViewModel.isIndoor
    val currentFloor by storyViewModel.currentFloor
    val currentStories by storyViewModel.currentStories
    val userLocation by locationViewModel.location
    val allLocations by storyViewModel.locations

    val indoorDetector = remember { IndoorDetector(context) }
    val isUserIndoor by indoorDetector.observeIndoorStatus().collectAsState(initial = false)

    // 2. REPLACED FetchAudio WITH SYNCED LOGIC
    // This ensures MapScreen and AudioScreen behave exactly the same.
    LaunchedEffect(userLocation, allLocations) {
        if (userLocation != null && allLocations.isNotEmpty()) {
            val nearest = DistanceCalculator.findNearestLocation(
                userLat = userLocation!!.latitude,
                userLng = userLocation!!.longitude,
                candidates = allLocations,
                radius = 50.0f // Matches MapScreen visibility radius
            )

            if (nearest != null) {
                // Only fetch if we changed location to avoid spamming
                if (currentLocationId != nearest.id) {
                    storyViewModel.fetchStoriesForLocation(nearest.id)
                }
            } else {
                storyViewModel.clearLocation()
            }
        }
    }

    // Update StoryViewModel with the Sensor Status
    LaunchedEffect(isUserIndoor, currentLocation) {
        if (currentLocation != null && currentLocation?.type == "outdoor") {
            storyViewModel.setIndoorStatus(false)
        } else {
            storyViewModel.setIndoorStatus(isUserIndoor)
        }
    }

    // 3. UI Layout
    val showFloorButton = isIndoor && (currentLocation?.type == "indoor")
    var showFloorMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header
            Text(
                text = "Stories",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )

            // Content Area
            if (currentLocationId == null) {
                // LOCKED STATE
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Landscape, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isUserIndoor) "Locating nearest building..." else "Exploring nearby...",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                        if (isUserIndoor) {
                            Text("(Indoor Mode)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        if (userLocation != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            FloatingActionButton(
                                onClick = {
                                    showAddLocationDialog = true
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Add, "Add new location")
                            }
                        }
                    }
                }
            } else {
                // UNLOCKED STATE
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isIndoor) Icons.Default.Business else Icons.Default.Landscape,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isIndoor) "Inside: $currentLocationId" else "Outdoor: $currentLocationId",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                // Feedback for user
                                if (isIndoor && isUserIndoor) {
                                    Text(
                                        text = "Location Pinned (GPS ignored)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        if (isIndoor && showFloorButton) {
                            Text("Floor $currentFloor", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = { navController.navigate(Routes.ADD_POST) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Story",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }

                    items(currentStories) { story ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            onClick = {
                                navController.navigate("${Routes.AUDIO_PLAYER}/${story.id}")
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(story.name, style = MaterialTheme.typography.titleMedium)
                                    if (story.user.isNotEmpty()) {
                                        Text(
                                            text = "By: ${story.user}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Text(story.description, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                                Icon(Icons.Default.PlayCircleOutline, contentDescription = "Play")
                            }
                        }
                    }
                }
            }
        }

        // Add Location Dialog
        if (showAddLocationDialog) {
            AlertDialog(
                onDismissRequest = { showAddLocationDialog = false },
                title = { Text("Add New Location") },
                text = {
                    TextField(
                        value = newLocationName,
                        onValueChange = { newLocationName = it },
                        label = { Text("Location Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newLocationName.isNotBlank()) {
                                userLocation?.let {
                                    val locationType = if (isIndoor) "indoor" else "outdoor"
                                    storyViewModel.addLocation(
                                        latitude = it.latitude,
                                        longitude = it.longitude,
                                        locationName = newLocationName,
                                        type = locationType
                                    )
                                }
                                showAddLocationDialog = false
                                newLocationName = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    Button(onClick = { showAddLocationDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Floor Button
        if (currentLocationId != null && showFloorButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                FloatingActionButton(
                    onClick = { showFloorMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Icon(Icons.Default.Layers, contentDescription = "Change Floor")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Floor $currentFloor")
                    }
                }

                DropdownMenu(
                    expanded = showFloorMenu,
                    onDismissRequest = { showFloorMenu = false }
                ) {
                    listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11).forEach { floor ->
                        DropdownMenuItem(
                            text = { Text("Floor $floor") },
                            onClick = {
                                storyViewModel.setCurrentFloor(floor)
                                showFloorMenu = false
                            },
                            leadingIcon = {
                                if (floor == currentFloor) {
                                    Icon(Icons.Default.PlayCircleOutline, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}