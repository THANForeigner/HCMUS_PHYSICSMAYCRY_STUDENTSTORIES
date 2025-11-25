package com.example.afinal.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.afinal.LocationGPS
import com.example.afinal.LocationViewModel
import com.example.afinal.StoryViewModel
import com.example.afinal.navigation.Routes
import com.example.afinal.utils.GeofenceHelper
import com.example.afinal.utils.IndoorDetector
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapScreen(navController: NavController) {
    val context = LocalContext.current
    val locationViewModel: LocationViewModel = viewModel()
    // Ensure we use the SAME ViewModel instance (scoped to NavGraph typically, but here implicitly activity if not scoped)
    val storyViewModel: StoryViewModel = viewModel()

    val locations by storyViewModel.locations
    val myLocation = locationViewModel.location.value

    val geofenceHelper = remember { GeofenceHelper(context) }
    val myLocationUtils = remember { LocationGPS(context) }
    val indoorDetector = remember { IndoorDetector(context) }

    val schoolCenter = LatLng(10.762867, 106.682496)

    // Permissions Check
    val hasForegroundPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val hasBackgroundPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else { true }

    // 1. Start GPS
    LaunchedEffect(hasForegroundPermission) {
        if (hasForegroundPermission) myLocationUtils.requestLocationUpdate(locationViewModel)
    }

    // 2. Indoor Detection Flow
    // We assume that if SNR is low -> Indoor.
    // We only care if we are also NEAR a building.
    LaunchedEffect(Unit) {
        if (hasForegroundPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            indoorDetector.observeIndoorStatus().collect { isIndoor ->
                storyViewModel.setIndoorStatus(isIndoor)
            }
        }
    }

    // 3. Geofencing & Proximity Logic
    LaunchedEffect(myLocation, locations, hasBackgroundPermission) {
        if (locations.isNotEmpty() && myLocation != null && hasBackgroundPermission) {
            val results = FloatArray(1)

            // Check closest building
            var closestLoc: String? = null
            var minDist = Float.MAX_VALUE

            locations.forEach { loc ->
                Location.distanceBetween(myLocation.latitude, myLocation.longitude, loc.latitude, loc.longitude, results)
                if (results[0] < minDist) {
                    minDist = results[0]
                    closestLoc = loc.id
                }
            }

            // If very close (< 10m), auto-set location in ViewModel
            if (minDist < 10 && closestLoc != null) {
                storyViewModel.fetchStoriesForLocation(closestLoc!!)
            } else {
                storyViewModel.clearLocation()
            }

            // Add Geofences if near school center
            Location.distanceBetween(myLocation.latitude, myLocation.longitude, schoolCenter.latitude, schoolCenter.longitude, results)
            if (results[0] < 500) {
                geofenceHelper.addGeofences(locations)
            } else {
                geofenceHelper.removeGeofences()
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(schoolCenter, 17f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasForegroundPermission)
        ) {
            locations.forEach { loc ->
                Circle(
                    center = LatLng(loc.latitude, loc.longitude),
                    radius = GeofenceHelper.GEOFENCE_RADIUS.toDouble(),
                    fillColor = Color(0x44FF0000),
                    strokeColor = Color.Red,
                    strokeWidth = 2f
                )
                Marker(
                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = loc.locationName,
                    snippet = "Tap to see ${loc.type} stories",
                    onClick = {
                        // Load data for this location
                        storyViewModel.fetchStoriesForLocation(loc.id)
                        // Navigate to List Screen (AudioScreen) instead of Player
                        navController.navigate(Routes.AUDIOS)
                        false
                    }
                )
            }
        }
    }
}