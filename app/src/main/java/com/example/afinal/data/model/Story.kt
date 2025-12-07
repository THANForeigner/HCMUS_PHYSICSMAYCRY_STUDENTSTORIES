package com.example.afinal.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Story(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val user_name: String = "",
    val tags: List<String> = emptyList(),
    val created_at: Timestamp? = null,
    val is_finished: Boolean = false,

    @get:PropertyName("audio_url") @set:PropertyName("audio_url")
    var audioUrl: String = "",

    @get:PropertyName("image_url") @set:PropertyName("image_url")
    var imageUrl: String = "",

    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val audioResourceId: Int = 0
) {
    val name: String get() = title
}