package com.example.afinal.data.model

import com.google.firebase.firestore.DocumentId

data class LocationModel(
    @DocumentId
    val id: String = "", // ID của document cha (ví dụ: b9d6...)
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)