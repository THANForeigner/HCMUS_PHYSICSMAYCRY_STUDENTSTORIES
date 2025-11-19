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

    Box(modifier = Modifier.fillMaxSize()) {
        // 3. Hiển thị Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // Marker vị trí của tôi (Màu xanh dương)
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