package com.example.afinal.logic

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.afinal.data.LocationData
import com.example.afinal.data.model.LocationViewModel
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.tasks.await

class LocationTracking(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val pdrSystem: PDRSystem
    private val indoorDetector = IndoorDetector(context)

    // Job management
    private var trackingJob: Job? = null
    private var indoorJob: Job? = null
    private var trackingScope: CoroutineScope? = null

    // State Tracking
    private var isTracking = false
    private var currentMode = "None"

    // StudentStories specific flags
    private var isIndoor = false
    private var isInZone = false

    // Location Listeners
    private var activeLocationCallback: LocationCallback? = null
    private var updateListener: ((LocationData) -> Unit)? = null

    // Callback to update UI or Logs (Optional)
    var onStatusChanged: ((mode: String) -> Unit)? = null

    init {
        // Initialize PDR
        pdrSystem = PDRSystem(context) { newLocation ->
            // PDR updates come here
            updateListener?.invoke(newLocation)
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking(viewModel: LocationViewModel, scope: CoroutineScope) {
        if (isTracking) return
        isTracking = true
        trackingScope = scope
        updateListener = { loc -> viewModel.updateLocation(loc) }

        // 1. Observe Indoor Detector (StudentStories Logic)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            indoorJob = scope.launch(Dispatchers.Main) {
                indoorDetector.observeIndoorStatus().collectLatest { indoorStatus ->
                    isIndoor = indoorStatus
                    Log.d("LocationGPS", "Indoor Status: $isIndoor")
                    checkAndChooseBestMode() // Re-evaluate mode when sensor changes
                }
            }
        }

        // 2. Start the Periodic Check Loop (TestLocation Logic)
        trackingJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                checkAndChooseBestMode()
                delay(500) // Re-check every 5s
            }
        }
    }

    fun stopTracking() {
        isTracking = false
        trackingJob?.cancel()
        indoorJob?.cancel()
        stopActiveUpdates()
        pdrSystem.stop()
        updateListener = null
        currentMode = "Stopped"
        onStatusChanged?.invoke(currentMode)
    }

    fun setZoneStatus(inZone: Boolean) {
        if (isInZone != inZone) {
            isInZone = inZone
            Log.d("LocationGPS", "Zone Status Changed: $isInZone")
            // Trigger a mode check immediately
            trackingScope?.launch(Dispatchers.Main) {
                checkAndChooseBestMode()
            }
        }
    }
    @SuppressLint("MissingPermission")
    private suspend fun checkAndChooseBestMode() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        Log.d("LocationGPS", "Checking for best provider...")

        // If we are strictly Indoor AND In the Zone, force PDR (or Network+PDR)
        if (isIndoor && isInZone) {
            // Use last known location or try to fetch one quickly to start PDR
            val lastLocation = fusedLocationClient.lastLocation.await()
            val startLoc = if (lastLocation != null) {
                LocationData(lastLocation.latitude, lastLocation.longitude)
            } else {
                null // PDR will warn if no start location
            }
            switchToNetworkPdrMode(startLoc)
            return
        }

        // If not forced by Zone, check GPS quality.
        val tokenSource = CancellationTokenSource()
        val locationTask = fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            tokenSource.token
        )

        try {
            val location = locationTask.await()

            // If we have a location AND it's accurate (< 10m), use GPS.
            if (location != null && location.accuracy < 5.0f) {
                switchToGpsMode()
            } else {
                // GPS is weak or null -> Fallback to Network + PDR
                val networkLoc = if (location != null) {
                    LocationData(location.latitude, location.longitude)
                } else {
                    val last = fusedLocationClient.lastLocation.await()
                    if (last != null) LocationData(last.latitude, last.longitude) else null
                }
                switchToNetworkPdrMode(networkLoc)
            }
        } catch (e: Exception) {
            Log.e("LocationGPS", "Error checking location: ${e.message}")
            switchToNetworkPdrMode(null)
        }
    }

    private fun switchToGpsMode() {
        if (currentMode == "GPS (High Accuracy)") return

        Log.d("LocationGPS", ">>> Switching to GPS Mode")
        currentMode = "GPS (High Accuracy)"
        onStatusChanged?.invoke(currentMode)

        // Stop PDR, Start High Accuracy GPS
        pdrSystem.stop()
        startActiveUpdates(Priority.PRIORITY_HIGH_ACCURACY)
    }

    private fun switchToNetworkPdrMode(startLocation: LocationData?) {
        // If we are already in this mode, just ensure PDR is running if needed
        if (currentMode == "Wifi/Network + PDR") {
            return
        }

        Log.d("LocationGPS", ">>> Switching to Network + PDR Mode")
        currentMode = "Wifi/Network + PDR"
        onStatusChanged?.invoke(currentMode)

        // Start Balanced Updates (Wifi/Cell) to assist PDR
        startActiveUpdates(Priority.PRIORITY_BALANCED_POWER_ACCURACY)

        // Start PDR
        if (startLocation != null) {
            pdrSystem.start(startLocation)
        } else {
            Log.w("LocationGPS", "Cannot start PDR: No start location available")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startActiveUpdates(priority: Int) {
        stopActiveUpdates()

        activeLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    val locData = LocationData(it.latitude, it.longitude)

                    // In GPS mode, update UI directly.
                    if (currentMode == "GPS (High Accuracy)") {
                        updateListener?.invoke(locData)
                    } else {
                        pdrSystem.calibrateLocation(locData)
                    }
                }
            }
        }

        val request = LocationRequest.Builder(priority, 500) // Update every 5s
            .setMinUpdateDistanceMeters(2f) // testlocation used 5f, studentstories used 2f
            .build()

        fusedLocationClient.requestLocationUpdates(request, activeLocationCallback!!, Looper.getMainLooper())
    }

    private fun stopActiveUpdates() {
        activeLocationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        activeLocationCallback = null
    }
}