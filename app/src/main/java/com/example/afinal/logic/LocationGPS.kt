package com.example.afinal.logic

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.afinal.data.LocationData
import com.example.afinal.models.LocationViewModel
import com.example.afinal.ultis.IndoorDetector
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LocationGPS(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val pdrSystem: PDRSystem
    private val indoorDetector = IndoorDetector(context)

    private var locationCallback: LocationCallback? = null
    private var locationJob: Job? = null

    // State Variables
    private var lastKnownLocation: LocationData? = null
    private var isTracking = false
    private var isIndoor = false
    private var isInZone = false // New flag: User must be in a zone to use PDR

    // Mode tracking to prevent redundant switching
    private var isPdrActive = false

    init {
        // Initialize PDR System with a callback to update ViewModel
        pdrSystem = PDRSystem(context) { newLocation ->
            lastKnownLocation = newLocation
            updateListener?.invoke(newLocation)
        }
    }

    private var updateListener: ((LocationData) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun startTracking(viewModel: LocationViewModel, scope: CoroutineScope) {
        if (isTracking) return
        isTracking = true

        updateListener = { loc ->
            viewModel.updateLocation(loc)
        }

        // 1. Start monitoring Indoor status
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locationJob = scope.launch(Dispatchers.Main) {
                indoorDetector.observeIndoorStatus().collectLatest { indoorStatus ->
                    isIndoor = indoorStatus
                    Log.d("LocationGPS", "Indoor Status: $isIndoor")
                    checkAndSwitchMode()
                }
            }
        }

        // 2. Initial Start (Default to GPS)
        startGpsUpdates()
    }

    fun stopTracking() {
        isTracking = false
        locationJob?.cancel()
        stopGpsUpdates()
        pdrSystem.stop()
        updateListener = null
        isPdrActive = false
    }

    /**
     * Called by UI (AudioScreen/MapScreen) when DistanceCalculator determines
     * if the user is inside a valid zone.
     */
    fun setZoneStatus(inZone: Boolean) {
        if (isInZone != inZone) {
            isInZone = inZone
            Log.d("LocationGPS", "Zone Status Changed: $isInZone")
            checkAndSwitchMode()
        }
    }

    private fun checkAndSwitchMode() {
        // CONDITION: Turn on indoor tracking ONLY if (Indoor + In Zone)
        if (isIndoor && isInZone) {
            switchToPDR()
        } else {
            switchToGPS()
        }
    }

    private fun switchToPDR() {
        if (isPdrActive) return // Already in PDR
        Log.d("LocationGPS", ">>> Switching to PDR Mode (Indoor + In Zone)")

        stopGpsUpdates()
        isPdrActive = true

        if (lastKnownLocation != null) {
            pdrSystem.start(lastKnownLocation!!)
        } else {
            Log.w("LocationGPS", "Cannot start PDR: No start location. Attempting single GPS fix.")
            startGpsUpdates(singleUpdate = true)
        }
    }

    private fun switchToGPS() {
        if (!isPdrActive && locationCallback != null) return // Already in GPS
        Log.d("LocationGPS", ">>> Switching to GPS Mode")

        isPdrActive = false
        pdrSystem.stop()
        startGpsUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startGpsUpdates(singleUpdate: Boolean = false) {
        if (!hasLocationPermission(context)) return

        // If we already have a callback and not asking for single update, skip
        if (locationCallback != null && !singleUpdate) return

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    val locData = LocationData(it.latitude, it.longitude)
                    lastKnownLocation = locData
                    updateListener?.invoke(locData)

                    if (singleUpdate) {
                        // Needed a fix to start PDR, now switch back
                        stopGpsUpdates()
                        if (isIndoor && isInZone) pdrSystem.start(locData)
                    }
                }
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateDistanceMeters(2f)
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
    }

    private fun stopGpsUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
    }

    private fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}