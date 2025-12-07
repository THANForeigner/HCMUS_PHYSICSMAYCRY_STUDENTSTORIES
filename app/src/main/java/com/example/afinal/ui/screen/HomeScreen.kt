package com.example.afinal.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.afinal.navigation.Routes
import com.example.afinal.ui.theme.FINALTheme
import com.example.afinal.data.repository.StoryRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import androidx.compose.runtime.remember
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun HomeScreen(navController: NavController) {
    val featuredStory = StoryRepository.getFeaturedStory()
    val nearbyStories = StoryRepository.getNearbyStories()

    val locationLatLng = if (featuredStory != null) {
        LatLng(featuredStory.latitude, featuredStory.longitude)
    } else {
        LatLng(10.762622, 106.660172)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(locationLatLng, 16f)
    }
    LaunchedEffect(locationLatLng) {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(locationLatLng, 16f)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text("Student Stories!", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Your location:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        rotationGesturesEnabled = false
                    )
                ) {
                    Marker(
                        state = remember { MarkerState(position = locationLatLng) },
                        title = featuredStory?.locationName ?: "Your location",
                        snippet = "You are here"
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (featuredStory != null) {
                Text(
                    text = "📍 ${featuredStory.locationName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Available stories:", style = MaterialTheme.typography.titleMedium)

            if (featuredStory != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    onClick = {
                        navController.navigate("${Routes.AUDIO_PLAYER}/${featuredStory.id}")
                    }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(featuredStory.title, style = MaterialTheme.typography.titleMedium)
                        Text(featuredStory.description)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Nearby location:", style = MaterialTheme.typography.titleMedium)
        }

        items(nearbyStories) { story ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = {
                    navController.navigate("${Routes.AUDIO_PLAYER}/${story.id}")
                }
            ) {
                Text(story.title, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
    FINALTheme() {
        HomeScreen(navController = rememberNavController())
    }
}