package com.example.afinal

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.afinal.data.model.StoryModel
import com.google.firebase.firestore.toObjects
import com.google.firebase.firestore.FirebaseFirestore
import com.example.afinal.data.model.LocationModel

class StoryViewModel : ViewModel() {
    private val _locations = mutableStateOf<List<LocationModel>>(emptyList())
    val locations: State<List<LocationModel>> = _locations
    private val _currentStories = mutableStateOf<List<StoryModel>>(emptyList())
    val currentStories: State<List<StoryModel>> = _currentStories

    init {
        fetchLocations()
    }
    private fun fetchLocations() {
        val db = FirebaseFirestore.getInstance()
        db.collection("Locations")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    _locations.value = snapshot.toObjects<LocationModel>()
                }
            }
    }

    fun fetchStoriesForLocation(locationId: String) {
        val db = FirebaseFirestore.getInstance()

        // Truy cập vào sub-collection "Stories"
        db.collection("Locations").document(locationId).collection("Stories")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val stories = snapshot.toObjects<StoryModel>()
                    _currentStories.value = stories
                    Log.d("Firestore", "Tìm thấy ${stories.size} câu chuyện")
                } else {
                    _currentStories.value = emptyList()
                }
            }
            .addOnFailureListener {
                _currentStories.value = emptyList()
            }
    }

    fun fetchLocationsForGeofence(){
        val db = FirebaseFirestore.getInstance()
        db.collectionGroup("coordinate")
            .get()
            .addOnSuccessListener { snapshot ->
                val fetchedLocations = mutableListOf<LocationModel>()
                for (document in snapshot.documents) {
                    val lat = document.getDouble("latitude")
                    val lng = document.getDouble("longitude")
                    val buildingDoc = document.reference.parent.parent
                    val buildingName = buildingDoc?.id ?: "Unknown Location"
                    if (lat != null && lng != null) {
                        fetchedLocations.add(
                            LocationModel(
                                id = buildingName,
                                locationName = buildingName,
                                latitude = lat,
                                longitude = lng,
                            )
                        )
                    }
                }
                _locations.value = fetchedLocations
                Log.d("Firestore", "Loaded ${fetchedLocations.size} locations from nested structure")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error loading nested locations", e)
            }
    }
}