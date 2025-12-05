package com.example.afinal.ultis

import android.location.Location
import com.example.afinal.data.ZoneData
import com.example.afinal.models.LocationModel
import com.google.android.gms.maps.model.LatLng

object DistanceCalculator {
    private const val DEFAULT_DISCOVERY_RADIUS = 3.0f // Meters for "Point" locations

    fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    // --- RAY CASTING ALGORITHM (Point in Polygon) ---
    // Checks if user(lat, lng) is inside the shape defined by zone.corners
    private fun isInsidePolygon(userLat: Double, userLng: Double, zone: ZoneData): Boolean {
        val corners = zone.corners
        if (corners.size < 3) return false // Not a polygon

        var intersectCount = 0
        for (j in 0 until corners.size) {
            val i = if (j == 0) corners.size - 1 else j - 1

            val lat1 = corners[i].first
            val lng1 = corners[i].second
            val lat2 = corners[j].first
            val lng2 = corners[j].second

            // Check if ray crosses the edge
            if (((lng1 > userLng) != (lng2 > userLng)) &&
                (userLat < (lat2 - lat1) * (userLng - lng1) / (lng2 - lng1) + lat1)
            ) {
                intersectCount++
            }
        }
        // If odd intersections, point is inside
        return (intersectCount % 2) == 1
    }

    // --- MAIN FINDER ---
    fun findCurrentLocation(
        userLat: Double,
        userLng: Double,
        candidates: List<LocationModel>
    ): LocationModel? {
        return candidates.find { loc ->
            if (loc.isZone) {
                // Use the new Corner/Polygon Logic
                isInsidePolygon(userLat, userLng, loc.toZoneData())
            } else {
                // Use the old Point Radius Logic
                getDistance(userLat, userLng, loc.latitude, loc.longitude) <= DEFAULT_DISCOVERY_RADIUS
            }
        }
    }

    // --- HELPER FOR MAP DRAWING ---
    fun getZoneCorners(loc: LocationModel): List<LatLng> {
        val zoneData = loc.toZoneData()
        return zoneData.corners.map { LatLng(it.first, it.second) }
    }

    // Legacy support for basic nearest point finding (used for outdoor proximity)
    fun findNearestLocation(
        userLat: Double,
        userLng: Double,
        candidates: List<LocationModel>,
        radius: Float = DEFAULT_DISCOVERY_RADIUS
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