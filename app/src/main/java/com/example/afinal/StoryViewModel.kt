package com.example.afinal

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.afinal.data.model.LocationModel
import com.example.afinal.data.model.StoryModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects

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
}