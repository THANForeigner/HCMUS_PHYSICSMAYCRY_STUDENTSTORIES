package com.example.afinal.ultis

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.afinal.models.LocationModel
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceHelper(context: Context) : ContextWrapper(context) {

    private val geofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags or PendingIntent.FLAG_MUTABLE
        }
        PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    @SuppressLint("MissingPermission")
    fun addGeofences(locations: List<LocationModel>) {
        if (locations.isEmpty()) return

        val geofenceList = locations.map { loc ->
            Geofence.Builder()
                .setRequestId(loc.id)
                .setCircularRegion(loc.latitude, loc.longitude, GEOFENCE_RADIUS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofenceList)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceHelper", "Added ${geofenceList.size} geofences successfully.")
            }
            .addOnFailureListener { e ->
                val errorMessage = getErrorString(e)
                Log.e("GeofenceHelper", "Failed to add geofences: $errorMessage")
            }
    }

    fun removeGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener { Log.d("GeofenceHelper", "Geofences removed") }
            .addOnFailureListener { Log.e("GeofenceHelper", "Failed to remove geofences") }
    }

    private fun getErrorString(e: Exception): String {
        if (e is com.google.android.gms.common.api.ApiException) {
            return when (e.statusCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "Geofence not available (Location disabled?)"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "Too many geofences"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "Too many pending intents"
                1004 -> "Insufficient Location Permissions (Need Background Location)"
                else -> "Unknown Geofence Error: ${e.statusCode}"
            }
        }
        return e.localizedMessage ?: "Unknown Error"
    }

    companion object {
        const val GEOFENCE_RADIUS = 3f
    }
}