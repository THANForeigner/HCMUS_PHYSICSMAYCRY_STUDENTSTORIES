package com.example.afinal.logic

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.afinal.R
import com.example.afinal.models.LocationModel
import com.example.afinal.ultis.DistanceCalculator
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore

class NotificationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val allLocations = mutableListOf<LocationModel>()
    private var lastNotifiedId: String? = null

    companion object {
        const val ACTION_START_TRACKING = "ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "ACTION_STOP_TRACKING"

        const val DISCOVERY_CHANNEL_ID = "discovery_channel"
        const val TRACKING_CHANNEL_ID = "tracking_status_channel"
        const val NOTIFICATION_ID_TRACKING = 202
    }

    override fun onBind(intent: Intent?): IBinder? = null // Unbound service

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return

                // Logic to find nearest location
                val nearest = DistanceCalculator.findNearestLocation(
                    location.latitude,
                    location.longitude,
                    allLocations
                )

                if (nearest != null && nearest.id != lastNotifiedId) {
                    Log.d("LocationService", "Discovered: ${nearest.locationName}")
                    lastNotifiedId = nearest.id
                    fetchStoryAndNotify(nearest)
                }
            }
        }

        fetchLocationsFromFirestore()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_TRACKING) {
            startForegroundTracking()
        } else if (intent?.action == ACTION_STOP_TRACKING) {
            stopSelf()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startForegroundTracking() {
        // 1. Create Notification to keep service alive in background
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createTrackingChannel(manager)

        val notification = NotificationCompat.Builder(this, TRACKING_CHANNEL_ID)
            .setContentTitle("Walking Mode Active")
            .setContentText("Scanning for stories nearby...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use appropriate icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID_TRACKING, notification)

        // 2. Start Fused Location Updates
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateDistanceMeters(10f) // Don't update if user hasn't moved 10m
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: Exception) {
            Log.e("LocationService", "Permission error", e)
        }
    }

    private fun fetchStoryAndNotify(locationModel: LocationModel) {
        val db = FirebaseFirestore.getInstance()
        // ... (Keep your original path logic here) ...
        val docRef = if (locationModel.type == "indoor") {
            db.collection("locations").document("locations")
                .collection("indoor_locations").document(locationModel.id)
        } else {
            db.collection("locations").document("locations")
                .collection("outdoor_locations").document(locationModel.id)
        }

        val postsQuery = if (locationModel.type == "indoor") {
            docRef.collection("floor").document("1").collection("posts")
        } else {
            docRef.collection("posts")
        }

        postsQuery.get().addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                val storyDoc = snapshot.documents.first()
                val title = storyDoc.getString("title") ?: "New Story"
                val user = storyDoc.getString("user") ?: "Unknown User"
                val audioUrl = storyDoc.getString("audioURL") ?: ""

                if (audioUrl.isNotEmpty()) {
                    sendDiscoveryNotification(locationModel.id, title, user, audioUrl)
                }
            }
        }
    }

    private fun sendDiscoveryNotification(storyId: String, title: String, user: String, audioUrl: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createDiscoveryChannel(manager)

        // 1. Intent to Open App
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_story_id", storyId)
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this, storyId.hashCode(), openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 2. CONNECTION: Intent to play audio in AudioPlayerService
        val playIntent = Intent(this, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_PLAY
            putExtra(AudioPlayerService.EXTRA_AUDIO_URL, audioUrl)
            putExtra(AudioPlayerService.EXTRA_TITLE, title)
            putExtra(AudioPlayerService.EXTRA_USER, user)
            putExtra(AudioPlayerService.EXTRA_STORY_ID, storyId)
        }

        val pendingPlay = PendingIntent.getService(
            this, storyId.hashCode(), playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, DISCOVERY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Found: $title")
            .setContentText("By $user")
            .setContentIntent(pendingOpenApp)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, "Play Now", pendingPlay) // Calls AudioService
            .build()

        manager.notify(storyId.hashCode(), notification)
    }

    private fun fetchLocationsFromFirestore() {
        // ... (Keep your original logic to populate allLocations) ...
        val db = FirebaseFirestore.getInstance()
        val rootRef = db.collection("locations").document("locations")
        val collections = mapOf("indoor_locations" to "indoor", "outdoor_locations" to "outdoor")

        collections.forEach { (collection, type) ->
            rootRef.collection(collection).get().addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val lat = doc.getDouble("latitude")
                    val lng = doc.getDouble("longitude")
                    if (lat != null && lng != null) {
                        allLocations.add(
                            LocationModel(
                                id = doc.id,
                                locationName = doc.id,
                                latitude = lat,
                                longitude = lng,
                                type = type
                            )
                        )
                    }
                }
            }
        }
    }

    private fun createDiscoveryChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(DISCOVERY_CHANNEL_ID, "Story Discoveries", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createTrackingChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(TRACKING_CHANNEL_ID, "Location Tracking", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}