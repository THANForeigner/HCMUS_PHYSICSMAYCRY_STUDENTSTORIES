package com.example.afinal.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.afinal.LocationGPS
import com.example.afinal.LocationViewModel
import com.example.afinal.StoryViewModel
import com.example.afinal.navigation.Routes // <-- Đã thêm import này
import com.example.afinal.utils.GeofenceHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapScreen(navController: NavController) {
    val context = LocalContext.current

    // 1. Gọi các ViewModel
    val locationViewModel: LocationViewModel = viewModel()
    val storyViewModel: StoryViewModel = viewModel()

    // 2. Lấy dữ liệu từ ViewModel
    // SỬA: Dùng .locations (số nhiều) thay vì .location
    val locations = storyViewModel.locations.value
    val myLocation = locationViewModel.location.value
    val geofenceHelper = remember { GeofenceHelper(context) }
    // Logic lấy GPS
    val myLocationUtils = remember { LocationGPS(context) }
    LaunchedEffect(Unit) {
        if (myLocationUtils.hasLocationPermission(context)) {
            myLocationUtils.requestLocationUpdate(locationViewModel)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(10.762622, 106.660172), 14f)
    }

    LaunchedEffect(locations) {
        if (locations.isNotEmpty()) {
            // Đảm bảo bạn đã check quyền ACCESS_FINE_LOCATION và ACCESS_BACKGROUND_LOCATION
            // trước khi gọi hàm này để tránh crash
            try {
                geofenceHelper.addGeofences(locations)
            } catch (e: SecurityException) {
                // Xử lý nếu chưa cấp quyền
            }
        }
    }

    LaunchedEffect(myLocation) {
        myLocation?.let { loc ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(loc.latitude, loc.longitude),
                    17f // Zoom level (15 is street, 20 is building)
                ),
                1000 // Animation duration in ms
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 3. Hiển thị Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            if (myLocation != null) {
                Marker(
                    state = MarkerState(position = LatLng(myLocation.latitude, myLocation.longitude)),
                    title = "You are here"
                )
            }

            // Marker cho các địa điểm từ Firestore (Màu đỏ mặc định)
            locations.forEach { loc ->
                Marker(
                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = loc.locationName,
                    snippet = "Press to see audio list",
                    icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                        com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE),
                    onClick = {
                        // Truyền ID của Location cha sang màn hình Player
                        navController.navigate("${Routes.AUDIO_PLAYER}/${loc.id}")
                        false
                    }
                )
            }
        }
    }
}