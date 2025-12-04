package com.example.afinal.ultis // Or com.example.afinal.logic, depending on your structure

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.afinal.R
import com.example.afinal.logic.AudioPlayerService
import com.example.afinal.logic.MainActivity
import com.example.afinal.models.LocationModel
import com.google.android.gms.location.LocationResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LocationReceiver : BroadcastReceiver() {

    companion object {
        const val DISCOVERY_CHANNEL_ID = "discovery_channel"
        private const val R_EARTH = 6371.0 // Earth radius in km
    }

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Check if this intent contains a Location Result
        if (LocationResult.hasResult(intent)) {
            val result = LocationResult.extractResult(intent)
            val location = result?.lastLocation ?: return

            Log.d("LocationReceiver", "Received location: ${location.latitude}, ${location.longitude}")

            // 2. CALL GOASYNC() - This keeps the BroadcastReceiver alive
            val pendingResult = goAsync()

            // 3. Start Background Work
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    checkStoriesNearby(context, location.latitude, location.longitude)
                } catch (e: Exception) {
                    Log.e("LocationReceiver", "Error checking stories", e)
                } finally {
                    // 4. IMPORTANT: Always finish the pending result
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun checkStoriesNearby(context: Context, lat: Double, lng: Double) {
        val db = FirebaseFirestore.getInstance()
        val allLocations = mutableListOf<LocationModel>()

        // 1. Fetch ALL locations (Indoor & Outdoor)
        // Note: In a production app, you might want to cache this in a local Room DB to save reads
        val collections = mapOf("indoor_locations" to "indoor", "outdoor_locations" to "outdoor")

        for ((collectionName, type) in collections) {
            val snapshot = db.collection("locations").document("locations")
                .collection(collectionName).get().await()

            for (doc in snapshot.documents) {
                val lLat = doc.getDouble("latitude")
                val lLng = doc.getDouble("longitude")
                if (lLat != null && lLng != null) {
                    allLocations.add(LocationModel(doc.id, doc.id, lLat, lLng, type))
                }
            }
        }

        // 2. Find nearest
        val nearest = findNearestLocation(lat, lng, allLocations)

        // 3. If close enough (e.g., < 30 meters), fetch story details
        if (nearest != null) {
            fetchStoryAndNotify(context, nearest)
        }
    }

    private suspend fun fetchStoryAndNotify(context: Context, location: LocationModel) {
        val db = FirebaseFirestore.getInstance()
        val docRef = if (location.type == "indoor") {
            db.collection("locations").document("locations")
                .collection("indoor_locations").document(location.id)
        } else {
            db.collection("locations").document("locations")
                .collection("outdoor_locations").document(location.id)
        }

        val postsQuery = if (location.type == "indoor") {
            docRef.collection("floor").document("1").collection("posts")
        } else {
            docRef.collection("posts")
        }

        val snapshot = postsQuery.get().await()
        if (!snapshot.isEmpty) {
            val storyDoc = snapshot.documents.first()
            val title = storyDoc.getString("title") ?: "New Story"
            val user = storyDoc.getString("user") ?: "Unknown User"
            val audioUrl = storyDoc.getString("audioURL") ?: ""

            if (audioUrl.isNotEmpty()) {
                sendDiscoveryNotification(context, location.id, title, user, audioUrl)
            }
        }
    }

    private fun sendDiscoveryNotification(context: Context, storyId: String, title: String, user: String, audioUrl: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(DISCOVERY_CHANNEL_ID, "Story Discoveries", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        // Open App Intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_story_id", storyId)
        }
        val pendingOpenApp = PendingIntent.getActivity(
            context, storyId.hashCode(), openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Play Audio Intent
        val playIntent = Intent(context, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_PLAY
            putExtra(AudioPlayerService.EXTRA_AUDIO_URL, audioUrl)
            putExtra(AudioPlayerService.EXTRA_TITLE, title)
            putExtra(AudioPlayerService.EXTRA_USER, user)
            putExtra(AudioPlayerService.EXTRA_STORY_ID, storyId)
        }
        val pendingPlay = PendingIntent.getService(
            context, storyId.hashCode(), playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, DISCOVERY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Nearby: $title")
            .setContentText("By $user")
            .setContentIntent(pendingOpenApp)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, "Play Now", pendingPlay)
            .build()

        manager.notify(storyId.hashCode(), notification)
    }

    // Helper: Simple distance calc
    private fun findNearestLocation(lat: Double, lng: Double, locations: List<LocationModel>): LocationModel? {
        var minDest = Double.MAX_VALUE
        var nearest: LocationModel? = null

        for (loc in locations) {
            val dLat = Math.toRadians(loc.latitude - lat)
            val dLon = Math.toRadians(loc.longitude - lng)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat)) * cos(Math.toRadians(loc.latitude)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            val distance = R_EARTH * c * 1000 // meters

            if (distance < 3) {
                if (distance < minDest) {
                    minDest = distance
                    nearest = loc
                }
            }
        }
        return nearest
    }
}