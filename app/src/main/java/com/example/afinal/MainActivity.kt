package com.example.afinal

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import com.example.afinal.navigation.AppNavigation
import com.example.afinal.ui.theme.FINALTheme

import com.example.afinal.StoryViewModel

class MainActivity : ComponentActivity() {
//    companion object {
//        var isAppInForeground = false
//    }
//
//    // 2. OVERRIDE onResume (Set to true)
//    override fun onResume() {
//        super.onResume()
//        isAppInForeground = true
//    }
//
//    // 3. OVERRIDE onPause (Set to false)
//    override fun onPause() {
//        super.onPause()
//        isAppInForeground = false
//    }

        private val locationViewModel: LocationViewModel by viewModels()
        private val storyViewModel: StoryViewModel by viewModels()
    
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                FINALTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val context = LocalContext.current
                        val locationGPS = remember { LocationGPS(context) }
    
                        // 1. Track Foreground Permission State
                        var hasForegroundPermission by remember {
                            mutableStateOf(
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            )
                        }
                        var hasAllPermissions by remember {
                            mutableStateOf(
                                // Check Location
                                (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                                        // Check Notification (Only for Android 13+)
                                        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                        } else true)
                            )
                        }
                        val backgroundPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) { isGranted ->
                            if (isGranted) {
                                Toast.makeText(context, "Background location enabled!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Background location needed for Geofencing", Toast.LENGTH_LONG).show()
                            }
                        }
    
                        val foregroundPermissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestMultiplePermissions()
                        ) { permissions ->
                            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    
                            if (fineLocationGranted || coarseLocationGranted) {
                                hasForegroundPermission = true
    
                                // If Foreground granted, NOW ask for Background (Android 10+)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val bgPermission = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                    )
                                    if (bgPermission != PackageManager.PERMISSION_GRANTED) {
                                        // This typically opens a dialog or settings screen on Android 11+
                                        backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                    }
                                }
                            }
                        }
                        val permissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestMultiplePermissions()
                        ) { permissions ->
                            val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                            val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissions[Manifest.permission.POST_NOTIFICATIONS] == true
                            } else true
    
                            hasAllPermissions = locationGranted && notificationGranted
                            if (locationGranted) {
                                locationGPS.requestLocationUpdate(locationViewModel)
                            }
                        }
                        LaunchedEffect(Unit) {
                            if (!hasAllPermissions) {
                                val permissionsToRequest = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                // Add Notification permission for Android 13+
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
    
                                permissionLauncher.launch(permissionsToRequest.toTypedArray())
                            } else {
                                locationGPS.requestLocationUpdate(locationViewModel)
                            }
                        }
                        LaunchedEffect(Unit) {
                            val permissionsToRequest = mutableListOf<String>()
    
                            // Check Foreground
                            if (!hasForegroundPermission) {
                                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                                permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
    
                            // Check Notification (Android 13+)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
    
                            // Launch Foreground Request if needed
                            if (permissionsToRequest.isNotEmpty()) {
                                foregroundPermissionLauncher.launch(permissionsToRequest.toTypedArray())
                            } else {
                                // If Foreground already granted, check Background explicitly
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                    }
                                }
                            }
                        }
    
                        AppNavigation(startIntent = intent, locationViewModel = locationViewModel, storyViewModel = storyViewModel)
                    }
                }
            }
        }}