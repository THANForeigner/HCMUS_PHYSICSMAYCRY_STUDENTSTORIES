package com.example.afinal.ultis

import android.os.Handler
import android.os.Looper
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Collections

class IndoorDetector(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // Realistic & research-backed thresholds (Marjasz et al. 2024 / Kim et al.)
    // Note: If using Median, these thresholds (23-28) remain effective for discriminating
    // "good" outdoor signals from "attenuated" indoor signals.
    private val INDOOR_SNR_THRESHOLD = 23.0f      // Indoor if metric <= 23
    private val OUTDOOR_SNR_THRESHOLD = 28.0f     // Outdoor if metric >= 28
    private val MIN_OUTDOOR_SATELLITES = 7        // Outdoor
    private val MAX_INDOOR_SATELLITES = 3         // Indoor

    private val handler = Handler(Looper.getMainLooper())

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("MissingPermission")
    fun observeIndoorStatus(): Flow<Boolean> = callbackFlow {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            close()
            return@callbackFlow
        }

        val callback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                super.onSatelliteStatusChanged(status)

                val usedSatellitesSnr = ArrayList<Float>()
                var usedSatCount = 0

                val totalSat = status.satelliteCount
                if (totalSat == 0) {
                    trySend(true) // No GPS visible → deep indoor
                    return
                }

                for (i in 0 until totalSat) {
                    if (status.usedInFix(i)) {
                        val cn0 = status.getCn0DbHz(i)
                        if (cn0 > 0.0f) {
                            usedSatellitesSnr.add(cn0)
                            usedSatCount++
                        }
                    }
                }

                if (usedSatCount == 0) {
                    trySend(true) // No usable signal → indoor
                    return
                }

                usedSatellitesSnr.sort()
                val medianSnr = if (usedSatellitesSnr.size % 2 == 1) {
                    usedSatellitesSnr[usedSatellitesSnr.size / 2]
                } else {
                    (usedSatellitesSnr[usedSatellitesSnr.size / 2 - 1] + usedSatellitesSnr[usedSatellitesSnr.size / 2]) / 2.0f
                }

                // 3. Use the Median SNR for the threshold checks
                val isIndoor =
                    (medianSnr <= INDOOR_SNR_THRESHOLD && usedSatCount <= MAX_INDOOR_SATELLITES)

                val isOutdoor =
                    (medianSnr >= OUTDOOR_SNR_THRESHOLD && usedSatCount >= MIN_OUTDOOR_SATELLITES)

                when {
                    isIndoor -> trySend(true)
                    isOutdoor -> trySend(false)
                    else -> {
                        // In transition/semi-outdoor, trust the SNR metric more than the count
                        trySend(medianSnr <= INDOOR_SNR_THRESHOLD)
                    }
                }
            }
        }

        locationManager.registerGnssStatusCallback(callback, handler)

        awaitClose {
            locationManager.unregisterGnssStatusCallback(callback)
        }
    }
}