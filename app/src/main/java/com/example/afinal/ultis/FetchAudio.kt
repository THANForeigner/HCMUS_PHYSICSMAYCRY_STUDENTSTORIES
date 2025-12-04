package com.example.afinal.ultis

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.afinal.models.StoryViewModel
import com.example.afinal.models.LocationModel
import com.example.afinal.data.LocationData

/**
 * Handles the logic for detecting nearby locations and triggering audio fetches.
 * Implements "Handover & Lock" logic for smoother Indoor/Outdoor transitions.
 */
@Composable
fun FetchAudio(
    userLocation: LocationData?,
    allLocations: List<LocationModel>,
    isUserIndoor: Boolean,
    currentLocationId: String?,
    storyViewModel: StoryViewModel
) {
    // MAIN LOGIC: Pinning & Tracking
    LaunchedEffect(userLocation, allLocations, isUserIndoor) {
        val userLoc = userLocation

        // Safety check: ensure we have data
        if (allLocations.isEmpty()) return@LaunchedEffect

        // 1. ANALYZE CURRENT STATE
        val currentLocModel = allLocations.find { it.id == currentLocationId }
        val isCurrentlyPinnedIndoor = currentLocModel?.type == "indoor"

        // 2. INDOOR LOCK: If we are physically indoors AND validly pinned to an indoor ID
        if (isUserIndoor && isCurrentlyPinnedIndoor) {
            // STOP PROCESSING. We are locked to this building.
            // We ignore GPS updates here to prevent "drift" or clearing the list.
            Log.d("FetchAudio", "Indoor Locked: Staying at $currentLocationId")
            return@LaunchedEffect
        }

        // 3. HANDOVER: Transitioning from Outdoor -> Indoor
        // If the detector says "Indoor" but we are currently at an "Outdoor" location (or null)
        if (isUserIndoor && !isCurrentlyPinnedIndoor) {
            // Strategy: Don't trust the User's GPS (it might be weak/drifting).
            // Instead, trust the LAST KNOWN LOCATION (currentLocModel).
            // Find the nearest "Indoor" version of where we just were.

            val referenceLat = currentLocModel?.latitude ?: userLoc?.latitude
            val referenceLng = currentLocModel?.longitude ?: userLoc?.longitude

            if (referenceLat != null && referenceLng != null) {
                val nearestIndoor = allLocations
                    .filter { it.type == "indoor" }
                    .minByOrNull { loc ->
                        DistanceCalculator.getDistance(
                            referenceLat, referenceLng,
                            loc.latitude, loc.longitude
                        )
                    }

                // If we found a building close to our last point (e.g., within 50m), SNAP to it.
                if (nearestIndoor != null) {
                    val dist = DistanceCalculator.getDistance(
                        referenceLat, referenceLng,
                        nearestIndoor.latitude, nearestIndoor.longitude
                    )

                    // 50m Threshold to associate the outdoor point with the building entrance
                    if (dist < 25.0) {
                        Log.d("FetchAudio", "Handover: Switching from ${currentLocationId ?: "GPS"} to Indoor: ${nearestIndoor.id}")
                        storyViewModel.fetchStoriesForLocation(nearestIndoor.id)
                        return@LaunchedEffect
                    }
                }
            }
        }

        // 4. LIVE TRACKING (Standard Outdoor Behavior)
        // We only reach here if:
        // a) We are Outdoor (isUserIndoor = false) -> Normal GPS tracking.
        // b) We are Indoor but haven't found a building yet -> Desperately looking using GPS.
        if (userLoc != null) {
            // Filter candidates based on mode
            val candidates = if (isUserIndoor) {
                allLocations.filter { it.type == "indoor" }
            } else {
                allLocations
            }

            val foundLocation = DistanceCalculator.findNearestLocation(
                userLoc.latitude,
                userLoc.longitude,
                candidates
            )

            if (foundLocation != null) {
                if (foundLocation.id != currentLocationId) {
                    storyViewModel.fetchStoriesForLocation(foundLocation.id)
                }
            } else {
                // Exit condition: Only clear if we are NOT pinned/locked
                // If we are outdoor and walked away from everything, clear it.
                if (currentLocationId != null) {
                    storyViewModel.clearLocation()
                }
            }
        }
    }
}