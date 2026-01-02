package com.example.afinal.logic

import android.location.Location
import android.util.Log
import com.example.afinal.data.ZoneData
import com.example.afinal.data.model.LocationModel
import com.google.android.gms.maps.model.LatLng

object DistanceCalculator {
    private const val DEFAULT_DISCOVERY_RADIUS = 3.0f

    fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
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

            if (((lng1 > userLng) != (lng2 > userLng)) &&
                (userLat < (lat2 - lat1) * (userLng - lng1) / (lng2 - lng1) + lat1)
            ) {
                intersectCount++
            }
        }
        return (intersectCount % 2) == 1
    }

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

        val t = ((userLat - lat1) * dLat + (userLng - lon1) * dLon) / (dLat * dLat + dLon * dLon)

        val tClamped = t.coerceIn(0.0, 1.0)

        val closestLat = lat1 + tClamped * dLat
        val closestLon = lon1 + tClamped * dLon

        return getDistance(userLat, userLng, closestLat, closestLon)
    }

    private fun getDistanceToZone(userLat: Double, userLng: Double, zone: ZoneData): Float {
        if (isInsidePolygon(userLat, userLng, zone)) return 0f

        var minDistance = Float.MAX_VALUE
        val corners = zone.corners
        if (corners.isEmpty()) return Float.MAX_VALUE

        for (i in 0 until corners.size) {
            val p1 = corners[i]
            val p2 = corners[(i + 1) % corners.size]

            val d = pointToSegmentDistance(userLat, userLng, p1.first, p1.second, p2.first, p2.second)
            if (d < minDistance) {
                minDistance = d
            }
        }
        Log.d("ZoneDebug","$minDistance")
        return minDistance
    }

    fun getZoneCorners(loc: LocationModel): List<LatLng> {
        val zoneData = loc.toZoneData()
        return zoneData.corners.map { LatLng(it.first, it.second) }
    }

    fun findNearestLocation(
        userLat: Double,
        userLng: Double,
        candidates: List<LocationModel>,
        radius: Float = DEFAULT_DISCOVERY_RADIUS
    ): LocationModel? {
        if (candidates.isEmpty()) return null

        val closestPair = candidates.map { loc ->
            val distance = if (loc.isZone) {
                getDistanceToZone(userLat, userLng, loc.toZoneData())
            } else {
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