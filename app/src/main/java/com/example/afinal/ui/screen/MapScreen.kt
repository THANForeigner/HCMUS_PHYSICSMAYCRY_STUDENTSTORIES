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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.afinal.logic.LocationGPS
import com.example.afinal.models.LocationViewModel
import com.example.afinal.models.StoryViewModel
import com.example.afinal.navigation.Routes
import com.example.afinal.ultis.DistanceCalculator
import com.example.afinal.ultis.GeofenceHelper
import com.example.afinal.ultis.IndoorDetector
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapScreen(navController: NavController, storyViewModel: StoryViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val locationViewModel: LocationViewModel = viewModel()

    val locations by storyViewModel.locations
    val myLocation = locationViewModel.location.value // Live User Location

    val myLocationUtils = remember { LocationGPS(context) }
    val indoorDetector = remember { IndoorDetector(context) }
    val geofenceHelper = remember { GeofenceHelper(context) }

    val schoolCenter = LatLng(10.762867, 106.682496)

    // Permissions Check
    val hasForegroundPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    var hasBackgroundPermission by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasBackgroundPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

    // 3. Centralized Distance Check Logic
    LaunchedEffect(myLocation, locations, hasBackgroundPermission) {
        if (locations.isNotEmpty() && myLocation != null) {
            // SYNCED: Use DistanceCalculator with 50m radius for UI visibility
            val nearestLocation = DistanceCalculator.findNearestLocation(
                userLat = myLocation!!.latitude,
                userLng = myLocation!!.longitude,
                candidates = locations,
                radius = 50.0f
            )

            if (nearestLocation != null) {
                storyViewModel.fetchStoriesForLocation(nearestLocation.id)
            } else {
                storyViewModel.clearLocation()
            }

            // Geofencing Logic (Keep distinct as it controls system fences, not UI)
            if (hasBackgroundPermission) {
                val distToSchool = DistanceCalculator.getDistance(
                    myLocation!!.latitude, myLocation!!.longitude,
                    schoolCenter.latitude, schoolCenter.longitude
                )

                if (distToSchool < 500) {
                    geofenceHelper.addGeofences(locations)
                } else {
                    geofenceHelper.removeGeofences()
                }
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
                Marker(
                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = loc.locationName,
                    snippet = "Tap to see ${loc.type} stories",
                    onClick = {
                        storyViewModel.fetchStoriesForLocation(loc.id)

                        navController.navigate(Routes.AUDIOS) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
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