package com.example.afinal.logic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.afinal.data.LocationData
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PDRSystem(private val context: Context, private val onLocationUpdate: (LocationData) -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    // Sensor data
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    // PDR State
    private var lastLocation: LocationData? = null
    private var stepCount = 0
    private var isRunning = false

    // Step Detection Constants
    private val STEP_THRESHOLD = 10.5f // m/s^2 (Standard gravity is ~9.8, so peaks > 10.5)
    private val MIN_TIME_BETWEEN_STEPS = 350L // ms
    private val STEP_LENGTH = 0.5f // meters (Average step length)

    // Step Detection Variables
    private var lastStepTime = 0L
    private var currentAccel = 0f
    private var lastAccel = 0f

    // Heading Smoothing
    private var azimuth = 0f
    private val ALPHA = 0.97f // Low-pass filter factor

    fun start(startLocation: LocationData) {
        if (isRunning) return
        lastLocation = startLocation

        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }

        isRunning = true
        Log.d("PDRSystem", "PDR Started at ${startLocation.latitude}, ${startLocation.longitude}")
    }

    fun stop() {
        if (!isRunning) return
        sensorManager.unregisterListener(this)
        isRunning = false
        Log.d("PDRSystem", "PDR Stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isRunning) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values.clone()
            detectStep(event.values)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values.clone()
        }

        // Calculate Orientation (Azimuth)
        if (gravity != null && geomagnetic != null) {
            val R = FloatArray(9)
            val I = FloatArray(9)
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                // orientation[0] is azimuth in radians (-pi to pi)

                // Smooth the azimuth
                azimuth = (ALPHA * azimuth + (1 - ALPHA) * orientation[0])
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun detectStep(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]

        // Calculate magnitude of acceleration
        currentAccel = sqrt(x * x + y * y + z * z)

        // Peak detection logic
        val currentTime = System.currentTimeMillis()
        if ((currentTime - lastStepTime) > MIN_TIME_BETWEEN_STEPS) {
            // Check if we crossed the threshold upwards
            if (currentAccel > STEP_THRESHOLD && lastAccel <= STEP_THRESHOLD) {
                lastStepTime = currentTime
                stepCount++
                updateLocation()
            }
        }
        lastAccel = currentAccel
    }

    private fun updateLocation() {
        lastLocation?.let { loc ->
            // Current Heading in Radians (adjust -PI/2 or similar depending on device orientation, usually 0 is North)
            // Note: Standard android azimuth: 0=North, PI/2=East, PI=South, -PI/2=West

            // Calculate displacement
            val distance = STEP_LENGTH

            // Earth Radius in meters
            val R = 6378137.0

            // Calculate change in position (in meters)
            val dx = distance * sin(azimuth) // East
            val dy = distance * cos(azimuth) // North

            // Convert meters to lat/lng degrees
            val dLat = (dy / R) * (180 / Math.PI)
            val dLon = (dx / (R * cos(Math.toRadians(loc.latitude)))) * (180 / Math.PI)

            val newLat = loc.latitude + dLat
            val newLon = loc.longitude + dLon

            val newLoc = LocationData(newLat, newLon)
            lastLocation = newLoc

            Log.d("PDRSystem", "Step detected! New Pos: $newLat, $newLon")

            // Send update to UI
            onLocationUpdate(newLoc)
        }
    }
    fun calibrateLocation(newLocation: LocationData) {
        this.lastLocation = newLocation
        Log.d("PDRSystem", "Location calibrated/corrected to: ${newLocation.latitude}, ${newLocation.longitude}")
    }
}