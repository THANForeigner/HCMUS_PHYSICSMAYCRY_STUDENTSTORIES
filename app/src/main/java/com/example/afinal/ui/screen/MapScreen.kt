package com.example.afinal.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.afinal.logic.LocationGPS
import com.example.afinal.models.LocationViewModel
import com.example.afinal.models.StoryViewModel
import com.example.afinal.navigation.Routes
import com.example.afinal.ui.theme.AppGradients
import com.example.afinal.ultis.DistanceCalculator
import com.example.afinal.ultis.IndoorDetector
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapScreen(navController: NavController, storyViewModel: StoryViewModel) {
    val context = LocalContext.current
    val locationViewModel: LocationViewModel = viewModel()

    val locations by storyViewModel.locations
    val myLocation by locationViewModel.location // Live User Location

    val myLocationUtils = remember { LocationGPS(context) }
    val indoorDetector = remember { IndoorDetector(context) }

    // --- COLOR DEFINITIONS ---
    val colorIndoor = Color(0xFF4285F4) // Google Blue
    val colorOutdoor = Color(0xFF34A853) // Google Green
    val colorActive = Color(0xFFEA4335)  // Google Red (Active Zone)

    // Center point for map camera (University of Science)
    val schoolCenter = LatLng(10.762867, 106.682496)

    // Permissions Check
    val hasForegroundPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // 1. Start GPS
    LaunchedEffect(hasForegroundPermission) {
        if (hasForegroundPermission) myLocationUtils.requestLocationUpdate(locationViewModel)
    }

    // 2. Indoor Detection Flow
    LaunchedEffect(Unit) {
        if (hasForegroundPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            indoorDetector.observeIndoorStatus().collect { isIndoor ->
                storyViewModel.setIndoorStatus(isIndoor)
            }
        }
    }

    // 3. Determine Current "Active" Location
    var activeLocationId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(myLocation, locations) {
        if (locations.isNotEmpty() && myLocation != null) {
            val targetLocation = DistanceCalculator.findCurrentLocation(
                userLat = myLocation!!.latitude,
                userLng = myLocation!!.longitude,
                candidates = locations
            )

            activeLocationId = targetLocation?.id

            if (targetLocation != null) {
                storyViewModel.fetchStoriesForLocation(targetLocation.id)
            } else {
                storyViewModel.clearLocation()
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(schoolCenter, 17f)
    }

    // --- UI STRUCTURE ---
    Column(modifier = Modifier.fillMaxSize()) {

        // --- 1. HEADER SECTION ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = AppGradients.Teal)
                .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text: Title & Subtitle
                Column {
                    Text(
                        text = "School Map",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Explore story locations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // --- 2. MAP SECTION ---
        Box(modifier = Modifier.weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasForegroundPermission)
            ) {
                // Optional: Explicit User Dot
                myLocation?.let { userLoc ->
                    Circle(
                        center = LatLng(userLoc.latitude, userLoc.longitude),
                        radius = 3.0,
                        fillColor = Color(0x220000FF),
                        strokeColor = Color.Blue,
                        strokeWidth = 2f
                    )
                }

                locations.forEach { loc ->
                    val isActive = loc.id == activeLocationId

                    // Determine Colors based on Type and Active State
                    val baseColor = if (isActive) colorActive else if (loc.type == "indoor") colorIndoor else colorOutdoor
                    val fillAlpha = if (isActive) 0.3f else 0.15f
                    val strokeThickness = if (isActive) 5f else 2f

                    // DRAW ZONE POLYGON
                    if (loc.isZone) {
                        val corners = DistanceCalculator.getZoneCorners(loc)
                        if (corners.isNotEmpty()) {
                            Polygon(
                                points = corners,
                                fillColor = baseColor.copy(alpha = fillAlpha),
                                strokeColor = baseColor,
                                strokeWidth = strokeThickness
                            )
                        }
                    }

                    // DRAW MARKER (Center point)
                    Marker(
                        state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                        title = loc.locationName,
                        snippet = if (isActive) "You are here!" else "Tap to view stories",
                        icon = if (isActive) {
                            com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                            )
                        } else {
                            com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                if (loc.type == "indoor") com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE
                                else com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                            )
                        },
                        onClick = {
                            storyViewModel.fetchStoriesForLocation(loc.id)
                            navController.navigate(Routes.AUDIOS) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            false
                        }
                    )
                }
            }
        }
    }
}