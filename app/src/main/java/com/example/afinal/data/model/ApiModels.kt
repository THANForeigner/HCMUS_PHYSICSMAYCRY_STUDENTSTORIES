package com.example.afinal.data.model

import com.example.afinal.models.StoryModel
import com.google.gson.annotations.SerializedName

data class SearchRequest(val query: String, @SerializedName("top_k") val topK: Int = 10)

data class InteractRequest(
        @SerializedName("user_id") val userId: String,
        @SerializedName("audio_firestore_id") val audioFirestoreId: String,
        val action: String, // "like", "listen", "dislike", "skip"
        @SerializedName("duration_percent") val durationPercent: Float = 0.0f
)

data class RecommendRequest(
        @SerializedName("user_id") val userId: String,
        @SerializedName("top_k") val topK: Int = 10
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
        @SerializedName("is_discovery") val isDiscovery: Boolean = false
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
            locationName = if (this.isDiscovery) "Khám phá mới" else "Gợi ý cho bạn"
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
        user_name = "AI Recommendation",
        locationName = if (this.isDiscovery) "Khám phá mới" else "Gợi ý cho bạn",
        tags = this.tags
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
