package com.example.afinal.models

import com.google.firebase.firestore.DocumentId
import com.example.afinal.data.ZoneData
import com.example.afinal.data.LocationData

data class LocationModel(
    @DocumentId
    val id: String = "",
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val type: String = "outdoor",
    val floors: List<Int> = emptyList(),
    val isZone: Boolean = false,

    val latitude1: Double? = null, val longitude1: Double? = null,
    val latitude2: Double? = null, val longitude2: Double? = null,
    val latitude3: Double? = null, val longitude3: Double? = null,
    val latitude4: Double? = null, val longitude4: Double? = null
) {
    fun toZoneData(): ZoneData {
        val points = mutableListOf<Pair<Double, Double>>()

        if (latitude1 != null && longitude1 != null) points.add(latitude1 to longitude1)
        if (latitude2 != null && longitude2 != null) points.add(latitude2 to longitude2)
        if (latitude3 != null && longitude3 != null) points.add(latitude3 to longitude3)
        if (latitude4 != null && longitude4 != null) points.add(latitude4 to longitude4)

        return ZoneData(corners = points)
    }

    fun toLocationData(): LocationData {
        return LocationData(latitude, longitude)
    }
}