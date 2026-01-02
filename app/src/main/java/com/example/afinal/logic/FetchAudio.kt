package com.example.afinal.logic

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.afinal.data.model.StoryViewModel
import com.example.afinal.data.model.LocationModel
import com.example.afinal.data.LocationData

@Composable
fun FetchAudio(
    userLocation: LocationData?,
    allLocations: List<LocationModel>,
    isUserIndoor: Boolean,
    currentLocationId: String?,
    storyViewModel: StoryViewModel
) {
    LaunchedEffect(userLocation, allLocations, isUserIndoor) {
        val userLoc = userLocation

        if (allLocations.isEmpty()) return@LaunchedEffect

        val currentLocModel = allLocations.find { it.id == currentLocationId }
        val isCurrentlyPinnedIndoor = currentLocModel?.type == "indoor"

        if (isUserIndoor && isCurrentlyPinnedIndoor) {
            Log.d("FetchAudio", "Indoor Locked: Staying at $currentLocationId")
            return@LaunchedEffect
        }

        if (isUserIndoor && !isCurrentlyPinnedIndoor) {


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

                if (nearestIndoor != null) {
                    val dist = DistanceCalculator.getDistance(
                        referenceLat, referenceLng,
                        nearestIndoor.latitude, nearestIndoor.longitude
                    )

                    if (dist < 25.0) {
                        Log.d("FetchAudio", "Handover: Switching from ${currentLocationId ?: "GPS"} to Indoor: ${nearestIndoor.id}")
                        storyViewModel.fetchStoriesForLocation(nearestIndoor.id)
                        return@LaunchedEffect
                    }
                }
            }
        }

        if (userLoc != null) {
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
                if (currentLocationId != null) {
                    storyViewModel.clearLocation()
                }
            }
        }
    }
}