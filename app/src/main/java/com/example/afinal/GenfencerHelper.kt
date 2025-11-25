package com.example.afinal.utils

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.util.Log
import com.example.afinal.data.model.LocationModel
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceHelper(base: Context) : ContextWrapper(base) {

    private val geofencingClient = LocationServices.getGeofencingClient(this)

    // PendingIntent để gọi BroadcastReceiver khi Geofence kích hoạt
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        // FLAG_UPDATE_CURRENT và FLAG_MUTABLE (cho Android 12+) là bắt buộc
        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }

    @SuppressLint("MissingPermission") // Quyền đã được check ở UI trước khi gọi hàm này
    fun addGeofences(locations: List<LocationModel>) {
        if (locations.isEmpty()) return

        val geofenceList = locations.map { loc ->
            Geofence.Builder()
                .setRequestId(loc.id) // ID của Location (ví dụ: "Building_I")
                .setCircularRegion(
                    loc.latitude,
                    loc.longitude,
                    GEOFENCE_RADIUS // Bán kính (mét)
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER) // Chỉ kích hoạt khi đi VÀO
                .build()
        }

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER) // Kích hoạt ngay nếu đang đứng trong vùng đó
            .addGeofences(geofenceList)
            .build()

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceHelper", "Đã thêm ${geofenceList.size} geofences thành công!")
            }
            .addOnFailureListener { e ->
                Log.e("GeofenceHelper", "Lỗi thêm geofence: ${e.message}")
            }
    }

    fun removeGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("GeofenceHelper", "Đã xóa toàn bộ geofences")
            }
    }

    companion object {
        const val GEOFENCE_RADIUS = 50f // Bán kính 50m
    }
}