package com.example.afinal.data.model

import com.google.firebase.firestore.DocumentId

data class StoryModel(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val audioUrl: String = ""
)