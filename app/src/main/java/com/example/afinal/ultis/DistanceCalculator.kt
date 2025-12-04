package com.example.afinal.ultis

import android.location.Location
import com.example.afinal.models.LocationModel

object DistanceCalculator {

    private const val DISCOVERY_THRESHOLD_METERS = 3.0f

    /**
     * Calculates the distance in meters between two points.
     */
    fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Finds the nearest location from a list that is within the discovery threshold.
     * Returns the LocationModel if found, or null if no location is close enough.
     */
    fun findNearestLocation(
        userLat: Double,
        userLng: Double,
        candidates: List<LocationModel>
    ): LocationModel? {
        val closestPair = candidates.map { loc ->
            val distance = getDistance(userLat, userLng, loc.latitude, loc.longitude)
            loc to distance
        }.minByOrNull { it.second }

        return if (closestPair != null && closestPair.second < DISCOVERY_THRESHOLD_METERS) {
            closestPair.first
        } else {
            null
        }
    }
}