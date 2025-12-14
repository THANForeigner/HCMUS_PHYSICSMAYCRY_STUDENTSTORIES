package com.example.afinal.models

import com.example.afinal.data.model.Position
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class StoryModel(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val user: String = "",
    val likes: Int = 0,
    val dislikes: Int = 0,
    @PropertyName("audioUrl")
    val audioUrl: String? = null,
    var playableUrl: String = "",
    var locationName: String = "",
    val position: Position? = null,
    val pictures: List<String> = emptyList(),
    val floor: Int? = null,
    val createdAt: Timestamp? = null
)