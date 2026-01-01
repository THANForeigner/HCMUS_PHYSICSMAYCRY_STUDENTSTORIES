package com.example.afinal.logic

import android.location.Location
import android.util.Log
import com.example.afinal.data.ZoneData
import com.example.afinal.data.model.LocationModel
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

    // --- NEW HELPER: Distance from point to segment ---
    private fun pointToSegmentDistance(
        userLat: Double, userLng: Double,
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        if (dLat == 0.0 && dLon == 0.0) {
            return getDistance(userLat, userLng, lat1, lon1)
        }

        // Project point onto line (parameter t) to find the closest point on the infinite line
        val t = ((userLat - lat1) * dLat + (userLng - lon1) * dLon) / (dLat * dLat + dLon * dLon)

        // Clamp t to the segment [0, 1] to handle cases where the closest point is an endpoint
        val tClamped = t.coerceIn(0.0, 1.0)

        val closestLat = lat1 + tClamped * dLat
        val closestLon = lon1 + tClamped * dLon

        return getDistance(userLat, userLng, closestLat, closestLon)
    }

    // --- NEW HELPER: Distance to Zone (0 if inside, else dist to closest edge) ---
    private fun getDistanceToZone(userLat: Double, userLng: Double, zone: ZoneData): Float {
        // If inside, distance is effectively 0
        if (isInsidePolygon(userLat, userLng, zone)) return 0f

        var minDistance = Float.MAX_VALUE
        val corners = zone.corners
        if (corners.isEmpty()) return Float.MAX_VALUE

        // Iterate over all edges to find the closest one
        for (i in 0 until corners.size) {
            val p1 = corners[i]
            val p2 = corners[(i + 1) % corners.size] // Wrap around to form closed loop

            val d = pointToSegmentDistance(userLat, userLng, p1.first, p1.second, p2.first, p2.second)
            if (d < minDistance) {
                minDistance = d
            }
        }
        Log.d("ZoneDebug","$minDistance")
        return minDistance
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
            val distance = if (loc.isZone) {
                // Use new zone distance logic
                getDistanceToZone(userLat, userLng, loc.toZoneData())
            } else {
                // Use existing point logic
                getDistance(userLat, userLng, loc.latitude, loc.longitude)
            }
            loc to distance
        }.minByOrNull { it.second }

        return if (closestPair != null && closestPair.second < radius) {
            closestPair.first
        } else {
            null
        }
    }
}