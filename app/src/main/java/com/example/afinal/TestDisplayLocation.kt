package com.example.afinal

import androidx.compose.runtime.Composable
import android.content.Context
import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat

@Composable
fun MyLocationApp(viewModel: LocationViewModel){
    val context = LocalContext.current
    val myLocationUtils = LocationGPS(context)
    DisplayLocation(myLocationUtils, viewModel = viewModel, context = context)
}

@Composable
fun DisplayLocation(
    locationUtils: LocationGPS,
    viewModel: LocationViewModel,
    context: Context
) {
    // Launcher for requesting permissions
    val location = viewModel.location.value
    val requestPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fine || coarse) {
                Toast.makeText(context, "Permissions granted", Toast.LENGTH_SHORT).show()
                locationUtils.requestLocationUpdate(viewModel=viewModel)
            } else {
                Toast.makeText(context, "Permissions denied", Toast.LENGTH_SHORT).show()
                val rationaleRequired = ActivityCompat.shouldShowRequestPermissionRationale(
                                         context as MainActivity,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                        )
                                         ||
                                         ActivityCompat.shouldShowRequestPermissionRationale(
                                         context as MainActivity,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                if(rationaleRequired){
                    Toast.makeText(context,"This feature require location permission", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context,"Please enable location permission", Toast.LENGTH_LONG).show()
                }
            }
        }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (location != null){
            Text("Location, lat: ${location.latitude} long: ${location.longitude}")
        }else {
            Text("Location not available")
        }
        Button(
            onClick = {
                if (locationUtils.hasLocationPermission(context)) {
                    // Update location
                    locationUtils.requestLocationUpdate(viewModel)
                } else {
                    // Request both permissions
                    requestPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        ) {
            Text("Get Location")
        }
    }
}