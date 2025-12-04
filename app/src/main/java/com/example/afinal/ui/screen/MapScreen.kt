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

    // 3. Distance Check Logic
    LaunchedEffect(myLocation, locations, hasBackgroundPermission) {
        if (locations.isNotEmpty() && myLocation != null) {
            val results = FloatArray(1)
            var closestLoc: String? = null
            var minDist = Float.MAX_VALUE

            locations.forEach { loc ->
                Location.distanceBetween(myLocation!!.latitude, myLocation!!.longitude, loc.latitude, loc.longitude, results)
                if (results[0] < minDist) {
                    minDist = results[0]
                    closestLoc = loc.id
                }
            }
            if (minDist < 50 && closestLoc != null) {
                storyViewModel.fetchStoriesForLocation(closestLoc!!)
            } else {
                storyViewModel.clearLocation()
            }
            if (hasBackgroundPermission) {
                Location.distanceBetween(myLocation!!.latitude, myLocation!!.longitude, schoolCenter.latitude, schoolCenter.longitude, results)
                if (results[0] < 500) {
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
            // [CHANGE 1] Added User Circle (Radius 10m)
            // This follows the user's live position
            myLocation?.let { userLoc ->
                Circle(
                    center = LatLng(userLoc.latitude, userLoc.longitude),
                    radius = 3.0, // Requested 10m radius
                    fillColor = Color(0x220000FF), // Light Blue transparent fill
                    strokeColor = Color.Blue,      // Solid Blue border
                    strokeWidth = 2f
                )
            }

            // [CHANGE 2] Loop through locations but REMOVED the static geofence circles
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