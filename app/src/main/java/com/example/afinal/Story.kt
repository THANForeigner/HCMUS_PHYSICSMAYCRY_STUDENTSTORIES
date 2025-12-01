package com.example.afinal.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class StoryModel(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val user: String = "",
    @PropertyName("audioUrl")
    val audioUrl: String? = null,
    val likes: Int = 0,
    val dislikes: Int = 0,
    var playableUrl: String = "",
    var locationName: String = "",
    val position: Position? = null
)