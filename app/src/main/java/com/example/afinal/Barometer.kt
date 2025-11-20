package com.example.afinal

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class Barometer(private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val pressureSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    fun observePressure(): Flow<Float> = callbackFlow {
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
                    trySend(event.values[0])
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not used
            }
        }

        if (pressureSensor != null) {
            sensorManager.registerListener(sensorEventListener, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            close(IllegalStateException("Pressure sensor not available"))
        }

        awaitClose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }
}
