package com.example.afinal.models

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afinal.models.StoryModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.collections.iterator

class StoryViewModel : ViewModel() {
    private val _locations = mutableStateOf<List<LocationModel>>(emptyList())
    val locations: State<List<LocationModel>> = _locations
    private val _currentStories = mutableStateOf<List<StoryModel>>(emptyList())
    val currentStories: State<List<StoryModel>> = _currentStories
    private val _allStories = mutableStateOf<List<StoryModel>>(emptyList())
    val allStories: State<List<StoryModel>> = _allStories
    private val _isIndoor = mutableStateOf(false)
    val isIndoor: State<Boolean> = _isIndoor
    private val _currentFloor = mutableStateOf(1)
    val currentFloor: State<Int> = _currentFloor
    private val _currentLocationId = mutableStateOf<String?>(null)
    val currentLocationId: State<String?> = _currentLocationId
    val currentLocation = derivedStateOf {
        _locations.value.find { it.id == _currentLocationId.value }
    }

    private var _loadedFloor = 0

    init {
        fetchLocations()
        fetchAllStories()
    }
    fun getStory(id: String): StoryModel? {
        return _currentStories.value.find { it.id == id }
            ?: _allStories.value.find { it.id == id }
    }
    fun setIndoorStatus(isIndoor: Boolean) {
        _isIndoor.value = isIndoor
    }
    fun setCurrentFloor(floor: Int) {
        _currentFloor.value = floor
        _currentLocationId.value?.let { locId ->
            fetchStoriesForLocation(locId, floor)
        }
    }
    fun clearLocation() {
        if (_currentLocationId.value != null) {
            _currentLocationId.value = null
            _currentStories.value = emptyList()
            _isIndoor.value = false
        }
    }

    fun addLocation(latitude: Double, longitude: Double, locationName: String, type: String) {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()
            // Use the user-provided locationName as the document ID
            val newLocationRef = db.collection("locations").document("locations")
                .collection("${type}_locations").document(locationName)

            val newLocationModel = LocationModel(
                id = locationName,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude,
                type = type
            )

            try {
                newLocationRef.set(newLocationModel).await()
                Log.d("StoryViewModel", "Location added successfully: $newLocationModel")

                // Update the local list of locations
                _locations.value = _locations.value + newLocationModel

                // Optionally, set this new location as the current one
                _currentLocationId.value = locationName

            } catch (e: Exception) {
                Log.e("StoryViewModel", "Error adding new location: $newLocationModel", e)
            }
        }
    }
    private fun fetchLocations() {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()
            val loadedLocations = mutableListOf<LocationModel>()
            val rootRef = db.collection("locations").document("locations")
            val collectionsMap = mapOf(
                "indoor_locations" to "indoor",
                "outdoor_locations" to "outdoor"
            )

            try {
                for ((collectionName, type) in collectionsMap) {
                    val snapshot = rootRef.collection(collectionName).get().await()
                    for (document in snapshot.documents) {
                        val lat = document.getDouble("latitude")
                        val lng = document.getDouble("longitude")

                        // 1. Fetch Zone Flag (default to false if missing)
                        val isZone = document.getBoolean("zone") ?: false

                        // 2. Fetch Corners (1, 2, 3, 4)
                        val lat1 = document.getDouble("latitude1")
                        val lng1 = document.getDouble("longitude1")
                        val lat2 = document.getDouble("latitude2")
                        val lng2 = document.getDouble("longitude2")
                        val lat3 = document.getDouble("latitude3")
                        val lng3 = document.getDouble("longitude3")
                        val lat4 = document.getDouble("latitude4")
                        val lng4 = document.getDouble("longitude4")

                        if (lat != null && lng != null) {
                            val locationId = document.id
                            var floors = emptyList<Int>()
                            if (type == "indoor") {
                                // ... (fetching floors logic remains the same) ...
                                val floorSnapshot = rootRef.collection(collectionName)
                                    .document(locationId)
                                    .collection("floor")
                                    .get()
                                    .await()
                                floors = floorSnapshot.documents.mapNotNull { it.id.toIntOrNull() }
                            }

                            loadedLocations.add(
                                LocationModel(
                                    id = locationId,
                                    locationName = document.id,
                                    latitude = lat,
                                    longitude = lng,
                                    type = type,
                                    floors = floors,
                                    isZone = isZone,
                                    // Map the corners
                                    latitude1 = lat1, longitude1 = lng1,
                                    latitude2 = lat2, longitude2 = lng2,
                                    latitude3 = lat3, longitude3 = lng3,
                                    latitude4 = lat4, longitude4 = lng4
                                )
                            )
                        }
                    }
                }
                _locations.value = loadedLocations
            } catch (e: Exception) {
                Log.e("Firestore", "Error fetching locations", e)
            }
        }
    }
    private fun fetchAllStories() {
        val db = FirebaseFirestore.getInstance()
        db.collectionGroup("posts").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("Firestore", "Error fetching all stories", e)
                return@addSnapshotListener
            }
            snapshot?.let {
                processSnapshot(it.documents, null, isAllStories = true)
            }
        }
    }

    fun fetchStoriesForLocation(locationId: String, floor: Int = 1) {
        if (_currentLocationId.value == locationId && _loadedFloor == floor && _currentStories.value.isNotEmpty()) {
            return
        }
        _currentLocationId.value = locationId
        _currentFloor.value = floor
        val db = FirebaseFirestore.getInstance()
        val locationType = _locations.value.find { it.id == locationId }?.type ?: "outdoor"
        val query = if (locationType == "indoor") {
            db.collection("locations").document("locations")
                .collection("indoor_locations").document(locationId)
                .collection("floor").document(floor.toString())
                .collection("posts")
        } else {
            db.collection("locations").document("locations")
                .collection("outdoor_locations").document(locationId)
                .collection("posts")
        }

        Log.d("StoryViewModel", "Fetching stories for location: $locationId, type: $locationType, query path: ${query.path}")

        query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("StoryViewModel", "Error fetching stories for location", e)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                Log.e("StoryViewModel", "Snapshot is null for location: $locationId")
                return@addSnapshotListener
            }
            Log.d("StoryViewModel", "Snapshot listener triggered for location: $locationId. Documents found: ${snapshot.documents.size}")
            processSnapshot(snapshot.documents, locationId, isAllStories = false)
        }
        _loadedFloor = floor
        setIndoorStatus(locationType == "indoor")
    }
    private fun processSnapshot(
        documents: List<DocumentSnapshot>,
        locationId: String?,
        isAllStories: Boolean
    ) {
        val storage = FirebaseStorage.getInstance()
        val storiesList = mutableListOf<StoryModel>()

        Log.d("StoryViewModel", "processSnapshot: received ${documents.size} documents. isAllStories: $isAllStories")

        if (documents.isEmpty()) {
            if (isAllStories) _allStories.value = emptyList() else _currentStories.value = emptyList()
            Log.d("StoryViewModel", "processSnapshot: No documents, clearing list.")
            return
        }

        var processedCount = 0
        val total = documents.size

        fun checkDone() {
            processedCount++
            if (processedCount == total) {
                if (isAllStories) {
                    _allStories.value = storiesList
                    Log.d("StoryViewModel", "processSnapshot: Finished processing all stories. Total: ${storiesList.size}")
                } else {
                    _currentStories.value = storiesList
                    Log.d("StoryViewModel", "processSnapshot: Finished processing stories for location. Total: ${storiesList.size}")
                }
            }
        }

        for (doc in documents) {
            val extractedLoc = locationId ?: doc.reference.path.split("/").getOrNull(3) ?: ""
            val story = try {
                doc.toObject(StoryModel::class.java)?.copy(id = doc.id, locationName = extractedLoc)
            } catch (e: Exception) { null }

            if (story == null) {
                Log.e("StoryViewModel", "processSnapshot: Failed to convert document to StoryModel. Doc ID: ${doc.id}")
                checkDone()
                continue
            }

            if (story.audioUrl?.startsWith("gs://") == true) {
                try {
                    storage.getReferenceFromUrl(story.audioUrl).downloadUrl
                        .addOnSuccessListener { uri ->
                            story.playableUrl = uri.toString()
                            storiesList.add(story)
                            Log.d("StoryViewModel", "processSnapshot: Successfully fetched playable URL for ${story.id}")
                            checkDone()
                        }
                        .addOnFailureListener { e ->
                            Log.e("StoryViewModel", "processSnapshot: Failed to get download URL for ${story.id}", e)
                            checkDone()
                        }
                } catch (e: Exception) {
                    Log.e("StoryViewModel", "processSnapshot: Exception while getting download URL for ${story.id}", e)
                    checkDone()
                }
            } else {
                story.playableUrl = story.audioUrl ?: ""
                storiesList.add(story)
                checkDone()
            }
        }
    }
}