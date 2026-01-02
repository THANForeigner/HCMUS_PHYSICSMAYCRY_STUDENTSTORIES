package com.example.afinal.logic

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
import kotlin.math.ceil

class IndoorDetector(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val INDOOR_SNR_THRESHOLD = 23.0f
    private val OUTDOOR_SNR_THRESHOLD = 28.0f
    private val MIN_OUTDOOR_SATELLITES = 7
    private val MAX_INDOOR_SATELLITES = 3

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
                    trySend(true) // No GPS visible -> indoor
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
                    trySend(true) // No usable signal -> indoor
                    return
                }

                usedSatellitesSnr.sort()

                val percentileIndex = ceil(usedSatellitesSnr.size * 0.7).toInt() - 1
                val safeIndex = percentileIndex.coerceIn(0, usedSatellitesSnr.lastIndex)

                val snrMetric = usedSatellitesSnr[safeIndex]

                val isIndoor =
                    (snrMetric <= INDOOR_SNR_THRESHOLD && usedSatCount <= MAX_INDOOR_SATELLITES)

                val isOutdoor =
                    (snrMetric >= OUTDOOR_SNR_THRESHOLD && usedSatCount >= MIN_OUTDOOR_SATELLITES)

                when {
                    isIndoor -> trySend(true)
                    isOutdoor -> trySend(false)
                    else -> {
                        trySend(snrMetric <= INDOOR_SNR_THRESHOLD)
                    }
                }            }
        }

        locationManager.registerGnssStatusCallback(callback, handler)

        awaitClose {
            locationManager.unregisterGnssStatusCallback(callback)
        }
    }
}