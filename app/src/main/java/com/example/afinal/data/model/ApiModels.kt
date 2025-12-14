package com.example.afinal.data.model

import android.util.Log
import com.example.afinal.models.StoryModel
import com.google.firebase.Timestamp
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale

data class SearchRequest(val query: String, @SerializedName("top_k") val topK: Int = 10)

data class InteractRequest(
        @SerializedName("user_id") val userId: String,
        @SerializedName("audio_firestore_id") val audioFirestoreId: String,
        val action: String, // "like", "listen", "dislike", "skip"
        @SerializedName("duration_percent") val durationPercent: Float = 0.0f
)

data class RecommendRequest(
        @SerializedName("user_id") val userId: String,
        @SerializedName("top_k") val topK: Int = 10,
        @SerializedName("name_building") val nameBuilding: String? = null
)

data class CommentRequest(
        @SerializedName("user_id") val userId: String,
        @SerializedName("audio_firestore_id") val audioFirestoreId: String,
        val content: String,
        @SerializedName("collection_name") val collectionName: String = "records"
)

data class AudioItem(
        @SerializedName("firestore_id") val firestoreId: String,
        val title: String,
        @SerializedName("final_text") val finalText: String,
        @SerializedName("audio_url") val audioUrl: String,
        @SerializedName("image_url") val imageUrl: String?,
        val score: Double,
        val tags: List<String> = emptyList(),
        @SerializedName("is_discovery") val isDiscovery: Boolean = false,
        @SerializedName("created_at") val createdAt: String? = null
) {
  fun toStoryModel(): StoryModel {
    return StoryModel(
            id = this.firestoreId,
            name = this.title,
            description = this.finalText,
            audioUrl = this.audioUrl,
            // Các trường dưới đây API Recommend chưa trả về đủ,
            // ta để giá trị mặc định hoặc cần API trả thêm Position
            pictures = if (this.imageUrl != null) listOf(this.imageUrl) else emptyList(),
            user = "AI Recommendation",
            locationName = if (this.isDiscovery) "Khám phá mới" else "Gợi ý cho bạn",
            createdAt = this.createdAt?.let { parseTimestamp(it) }
    )
  }
}

fun AudioItem.toStory(): Story {
    return Story(
        id = this.firestoreId,
        title = this.title,
        description = this.finalText,
        audioUrl = this.audioUrl,
        imageUrl = this.imageUrl ?: "",
        locationName = if (this.isDiscovery) "Khám phá mới" else "Gợi ý cho bạn",
        tags = this.tags,
        isFinished = true,
        created_at = this.createdAt?.let { parseTimestamp(it) }
    )
}

data class ApiResponse(
        val results: List<AudioItem>,
        val type: String?, // "hybrid_smart", "cold_start_latest", ...
        val message: String?
)

data class GenericResponse(
        val status: String,
        val message: String?,
        val tags: String? // Tag mới AI học được (nếu có)
)

private fun parseTimestamp(timestamp: String): Timestamp? {
    // Custom formatter for the user's pattern "December 14, 2025 at 6:11:42 AM UTC+7"
    val userProvidedFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm:ss a 'UTC'X", Locale.ENGLISH)

    try {
        val offsetDateTime = OffsetDateTime.parse(timestamp, userProvidedFormatter)
        return Timestamp(Date.from(offsetDateTime.toInstant()))
    } catch (e: DateTimeParseException) {
        Log.e("ApiModels", "Error parsing with user-provided format: '$timestamp'", e)
    }

    // Fallback to ISO 8601 format (e.g., "yyyy-MM-dd'T'HH:mm:ss'Z'")
    try {
        val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME // Handles "yyyy-MM-dd'T'HH:mm:ssZ" or "yyyy-MM-dd'T'HH:mm:ss+HH:mm"
        val offsetDateTime = OffsetDateTime.parse(timestamp, isoFormatter)
        return Timestamp(Date.from(offsetDateTime.toInstant()))
    } catch (e: DateTimeParseException) {
        Log.e("ApiModels", "Error parsing with ISO_OFFSET_DATE_TIME format: '$timestamp'", e)
    }

    // Fallback to simpler ISO 8601 format without offset (assuming UTC)
    try {
        val simpleIsoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val offsetDateTime = OffsetDateTime.parse(timestamp, simpleIsoFormatter)
        return Timestamp(Date.from(offsetDateTime.toInstant()))
    } catch (e: DateTimeParseException) {
        Log.e("ApiModels", "Error parsing with simple ISO 8601 format: '$timestamp'", e)
    }


    Log.e("ApiModels", "Could not parse timestamp with any known format: '$timestamp'")
    return null
}
