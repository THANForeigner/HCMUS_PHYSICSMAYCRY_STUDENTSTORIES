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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LocationGPS(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val pdrSystem: PDRSystem
    private val indoorDetector = IndoorDetector(context)

    private var locationCallback: LocationCallback? = null
    private var locationJob: Job? = null

    // Scope for running the 1-minute timer
    private var trackingScope: CoroutineScope? = null

    // State Variables
    private var lastKnownLocation: LocationData? = null
    private var isTracking = false
    private var isIndoor = false
    private var isInZone = false

    private var isPdrActive = false

    // New Flag: Are we currently waiting for the initial GPS fix?
    private var isAcquiringInitialLocation = false

    init {
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
        trackingScope = scope // Store the scope to use for the timer

        updateListener = { loc ->
            viewModel.updateLocation(loc)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locationJob = scope.launch(Dispatchers.Main) {
                indoorDetector.observeIndoorStatus().collectLatest { indoorStatus ->
                    isIndoor = indoorStatus
                    Log.d("LocationGPS", "Indoor Status: $isIndoor")
                    checkAndSwitchMode()
                }
            }
        }

        // Always start with GPS initially
        startGpsUpdates()
    }

    fun stopTracking() {
        isTracking = false
        locationJob?.cancel()
        stopGpsUpdates()
        pdrSystem.stop()
        updateListener = null
        isPdrActive = false
        isAcquiringInitialLocation = false
    }

    fun setZoneStatus(inZone: Boolean) {
        if (isInZone != inZone) {
            isInZone = inZone
            Log.d("LocationGPS", "Zone Status Changed: $isInZone")
            checkAndSwitchMode()
        }
    }

    private fun checkAndSwitchMode() {
        if (isIndoor && isInZone) {
            // Case 1: We already have a location -> Switch to PDR immediately
            if (lastKnownLocation != null) {
                switchToPDR()
            } else {
                // Case 2: No location yet -> Start 1-minute GPS acquisition if not already running
                if (!isAcquiringInitialLocation) {
                    startInitialLocationAcquisition()
                }
            }
        } else {
            // Outdoor or Not in Zone -> Use GPS
            // If we were acquiring, stop the acquisition flag (timer will finish harmlessly)
            isAcquiringInitialLocation = false
            switchToGPS()
        }
    }

    private fun startInitialLocationAcquisition() {
        Log.d("LocationGPS", "Indoor detected but no location. Running GPS for 1 minute...")
        isAcquiringInitialLocation = true

        // Ensure GPS updates are running
        if (locationCallback == null) {
            startGpsUpdates()
        }

        // Launch 1-minute timer
        trackingScope?.launch {
            delay(60_000) // Wait 1 minute

            // After 1 minute, if we are still in the acquiring state
            if (isAcquiringInitialLocation) {
                Log.d("LocationGPS", "1-minute acquisition finished. Switching to PDR.")
                isAcquiringInitialLocation = false

                // If we found a location during the wait, this will switch.
                // If still null, checkAndSwitchMode will trigger acquisition again (retry).
                checkAndSwitchMode()
            }
        }
    }

    private fun switchToPDR() {
        if (isPdrActive) return

        if (lastKnownLocation == null) {
            Log.e("LocationGPS", "Cannot switch to PDR: Last location is null.")
            return
        }

        Log.d("LocationGPS", ">>> Switching to PDR Mode (Indoor + In Zone)")
        stopGpsUpdates()
        isPdrActive = true
        pdrSystem.start(lastKnownLocation!!)
    }

    private fun switchToGPS() {
        if (!isPdrActive && locationCallback != null) return // Already in GPS
        Log.d("LocationGPS", ">>> Switching to GPS Mode")

        isPdrActive = false
        pdrSystem.stop()
        startGpsUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startGpsUpdates() {
        if (!hasLocationPermission(context)) return
        if (locationCallback != null) return

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    val locData = LocationData(it.latitude, it.longitude)
                    lastKnownLocation = locData
                    updateListener?.invoke(locData)
                    // Note: We don't auto-switch here anymore. We wait for the timer
                    // or checkAndSwitchMode logic.
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