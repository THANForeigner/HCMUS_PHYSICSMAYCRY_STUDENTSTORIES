package com.example.afinal.logic
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
import com.example.afinal.data.model.RecommendRequest
import com.example.afinal.data.model.AudioItem // [THÊM] Import AudioItem
import com.example.afinal.data.model.ApiResponse // [THÊM] Import ApiResponse
import com.example.afinal.data.network.RetrofitClient
import com.example.afinal.data.model.LocationModel
import com.google.android.gms.location.LocationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Response // [THÊM] Import Retrofit Response
class LocationReceiver : BroadcastReceiver() {

    companion object {
        const val DISCOVERY_CHANNEL_ID = "discovery_channel"
        private const val TAG = "LocationReceiverDebug"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (LocationResult.hasResult(intent)) {
            val result = LocationResult.extractResult(intent)
            val location = result?.lastLocation ?: return

            Log.d(TAG, ">>> Background Location: ${location.latitude}, ${location.longitude}")

            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    checkStoriesNearby(context, location.latitude, location.longitude)
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking stories", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun checkStoriesNearby(context: Context, lat: Double, lng: Double) {
        val db = FirebaseFirestore.getInstance()
        val allLocations = mutableListOf<LocationModel>()

        val collections = mapOf("indoor_locations" to "indoor", "outdoor_locations" to "outdoor")

        for ((collectionName, type) in collections) {
            val snapshot = db.collection("locations").document("locations")
                .collection(collectionName).get().await()

            for (doc in snapshot.documents) {
                val lLat = doc.getDouble("latitude")
                val lLng = doc.getDouble("longitude")
                val isZone = doc.getBoolean("zone") ?: false

                val lat1 = doc.getDouble("latitude1"); val lng1 = doc.getDouble("longitude1")
                val lat2 = doc.getDouble("latitude2"); val lng2 = doc.getDouble("longitude2")
                val lat3 = doc.getDouble("latitude3"); val lng3 = doc.getDouble("longitude3")
                val lat4 = doc.getDouble("latitude4"); val lng4 = doc.getDouble("longitude4")

                if (lLat != null && lLng != null) {
                    allLocations.add(
                        LocationModel(
                            id = doc.id,
                            locationName = doc.id,
                            latitude = lLat,
                            longitude = lLng,
                            type = type,
                            isZone = isZone,
                            latitude1 = lat1, longitude1 = lng1,
                            latitude2 = lat2, longitude2 = lng2,
                            latitude3 = lat3, longitude3 = lng3,
                            latitude4 = lat4, longitude4 = lng4
                        )
                    )
                }
            }
        }

        val currentLoc = DistanceCalculator.findNearestLocation(lat, lng, allLocations)

        if (currentLoc != null) {
            Log.d(TAG, ">>> Entered Location: ${currentLoc.id}. Starting logic...")
            fetchRecommendationOrLatestStory(context, currentLoc)
        } else {
            Log.d(TAG, ">>> No locations match.")
        }
    }

    private suspend fun fetchRecommendationOrLatestStory(context: Context, location: LocationModel) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "test_user"

        try {
            Log.d(TAG, "Fetching recommendations for user: $userId at ${location.locationName}")
            val request = RecommendRequest(userId = userId, nameBuilding = location.locationName)

            val response: Response<ApiResponse> = RetrofitClient.api.getRecommendations(request)

            if (response.isSuccessful) {
                val apiResponse = response.body()
                val results = apiResponse?.results

                if (!results.isNullOrEmpty()) {
                    val firstRecommend: AudioItem = results[0]
                    Log.d(TAG, ">>> Found Recommendation: ${firstRecommend.title}")

                    val resolvedAudioUrl = resolveAudioUrl(firstRecommend.audioUrl)

                    if (resolvedAudioUrl.isNotEmpty()) {
                        sendDiscoveryNotification(
                            context = context,
                            locationId = location.id,
                            storyId = firstRecommend.firestoreId,
                            title = firstRecommend.title,
                            user = firstRecommend.userName ?: "Anonymous",
                            audioUrl = resolvedAudioUrl,
                            isRecommend = true
                        )
                        return
                    }
                }
            } else {
                Log.e(TAG, "Recommend API failed code: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recommendations API", e)
        }

        Log.d(TAG, ">>> Fallback to Firestore (Fetching Latest Story)")
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

        try {
            val snapshot = postsQuery
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val storyDoc = snapshot.documents.first()
                val storyId = storyDoc.id
                val title = storyDoc.getString("title") ?: "New Story"
                val user = storyDoc.getString("user_name") ?: "Unknown User"
                val rawAudioUrl = storyDoc.getString("audio_url") ?: ""

                Log.d(TAG, ">>> Found Latest Firestore Story: $title")

                val resolvedAudioUrl = resolveAudioUrl(rawAudioUrl)

                if (resolvedAudioUrl.isNotEmpty()) {
                    sendDiscoveryNotification(
                        context = context,
                        locationId = location.id,
                        storyId = storyId,
                        title = title,
                        user = user,
                        audioUrl = resolvedAudioUrl,
                        isRecommend = false
                    )
                }
            } else {
                Log.d(TAG, ">>> No stories found in Firestore for this location.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Firestore", e)
        }
    }

    private suspend fun resolveAudioUrl(originalUrl: String): String {
        if (originalUrl.isEmpty()) return ""

        return if (originalUrl.startsWith("gs://")) {
            try {
                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(originalUrl)
                storageRef.downloadUrl.await().toString()
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving gs:// URL", e)
                ""
            }
        } else {
            originalUrl
        }
    }

    private fun sendDiscoveryNotification(
        context: Context, locationId: String, storyId: String,
        title: String, user: String, audioUrl: String, isRecommend: Boolean
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(DISCOVERY_CHANNEL_ID, "Story Discoveries", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_story_id", storyId)
        }
        val pendingOpenApp = PendingIntent.getActivity(
            context, locationId.hashCode(), openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playIntent = Intent(context, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_PLAY
            putExtra(AudioPlayerService.EXTRA_AUDIO_URL, audioUrl)
            putExtra(AudioPlayerService.EXTRA_TITLE, title)
            putExtra(AudioPlayerService.EXTRA_USER, user)
            putExtra(AudioPlayerService.EXTRA_STORY_ID, storyId)
            putExtra("EXTRA_NOTIFICATION_ID", locationId)
        }
        val pendingPlay = PendingIntent.getService(
            context, locationId.hashCode(), playIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Thay đổi nội dung thông báo dựa trên việc đây là Gợi ý hay Mới nhất
        val contentText = if (isRecommend) "Recommended for you by $user" else "New story from $user"

        val notification = NotificationCompat.Builder(context, DISCOVERY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(pendingOpenApp)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_media_play, "Listen Now", pendingPlay)
            .build()

        manager.notify(locationId.hashCode(), notification)
    }
}