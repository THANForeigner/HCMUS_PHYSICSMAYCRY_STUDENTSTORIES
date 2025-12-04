package com.example.afinal.ultis

import android.location.Location
import com.example.afinal.models.LocationModel

object DistanceCalculator {
    private const val DEFAULT_DISCOVERY_THRESHOLD = 3.0f
    fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
    fun findNearestLocation(
        userLat: Double,
        userLng: Double,
        candidates: List<LocationModel>,
        radius: Float = DEFAULT_DISCOVERY_THRESHOLD
    ): LocationModel? {
        if (candidates.isEmpty()) return null

        val closestPair = candidates.map { loc ->
            val distance = getDistance(userLat, userLng, loc.latitude, loc.longitude)
            loc to distance
        }.minByOrNull { it.second }

        return if (closestPair != null && closestPair.second < radius) {
            closestPair.first
        } else {
            null
        }
    }
}