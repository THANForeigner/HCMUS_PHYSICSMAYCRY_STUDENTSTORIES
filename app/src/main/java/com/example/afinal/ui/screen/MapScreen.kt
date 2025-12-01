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
fun MapScreen(navController: NavController, storyViewModel: StoryViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val locationViewModel: LocationViewModel = viewModel()

    val locations by storyViewModel.locations
    val myLocation = locationViewModel.location.value


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
    // We assume that if SNR is low -> Indoor.
    // We only care if we are also NEAR a building.
    LaunchedEffect(Unit) {
        if (hasForegroundPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            indoorDetector.observeIndoorStatus().collect { isIndoor ->
                storyViewModel.setIndoorStatus(isIndoor)
            }
        }
    }

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
            locations.forEach { loc ->
                Circle(
                    center = LatLng(loc.latitude, loc.longitude),
                    radius = 10.0,
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