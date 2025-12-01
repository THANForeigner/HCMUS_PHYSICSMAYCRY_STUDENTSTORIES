package com.example.afinal.data.model

import com.google.firebase.firestore.DocumentId

data class PostModel(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val audioUrl: String? = null,
    val pictures: List<String> = emptyList(),
    val position: Position? = null
)
