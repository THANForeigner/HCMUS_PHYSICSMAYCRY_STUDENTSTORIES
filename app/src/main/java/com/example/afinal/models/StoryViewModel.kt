package com.example.afinal.models

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.collections.iterator

class StoryViewModel : ViewModel() {
    private val _locations = mutableStateOf<List<LocationModel>>(emptyList())
    val locations: State<List<LocationModel>> = _locations

    // Helper lists to store the latest data from each collection separately
    private var _indoorList = listOf<LocationModel>()
    private var _outdoorList = listOf<LocationModel>()

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

    // Track the active listener so we can remove it when switching locations
    private var storyListener: ListenerRegistration? = null

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
            // Remove listener when clearing location to stop updates
            storyListener?.remove()
            storyListener = null

            _currentLocationId.value = null
            _currentStories.value = emptyList()
            _isIndoor.value = false
        }
    }

    fun addLocation(latitude: Double, longitude: Double, locationName: String, type: String) {
        viewModelScope.launch {
            val db = FirebaseFirestore.getInstance()
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
                _currentLocationId.value = locationName
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Error adding new location: $newLocationModel", e)
            }
        }
    }

    private fun fetchLocations() {
        val db = FirebaseFirestore.getInstance()
        val rootRef = db.collection("locations").document("locations")
        val collectionsMap = mapOf(
            "indoor_locations" to "indoor",
            "outdoor_locations" to "outdoor"
        )

        for ((collectionName, type) in collectionsMap) {
            rootRef.collection(collectionName).addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "Listen failed for $collectionName", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    viewModelScope.launch {
                        val parsedList = mutableListOf<LocationModel>()

                        for (document in snapshot.documents) {
                            val lat = document.getDouble("latitude")
                            val lng = document.getDouble("longitude")

                            // 1. Fetch Zone Data
                            val isZone = document.getBoolean("zone") ?: false
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
                                    try {
                                        val floorSnapshot = rootRef.collection(collectionName)
                                            .document(locationId)
                                            .collection("floor")
                                            .get()
                                            .await()
                                        floors = floorSnapshot.documents.mapNotNull { it.id.toIntOrNull() }
                                    } catch (ex: Exception) {
                                        Log.e("StoryViewModel", "Error fetching floors for $locationId", ex)
                                    }
                                }

                                parsedList.add(
                                    LocationModel(
                                        id = locationId,
                                        locationName = document.id,
                                        latitude = lat,
                                        longitude = lng,
                                        type = type,
                                        floors = floors,
                                        isZone = isZone,
                                        latitude1 = lat1, longitude1 = lng1,
                                        latitude2 = lat2, longitude2 = lng2,
                                        latitude3 = lat3, longitude3 = lng3,
                                        latitude4 = lat4, longitude4 = lng4
                                    )
                                )
                            }
                        }

                        if (type == "indoor") {
                            _indoorList = parsedList
                        } else {
                            _outdoorList = parsedList
                        }

                        _locations.value = _indoorList + _outdoorList
                    }
                }
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

    fun fetchStoriesForLocation(locationId: String, floor: Int = 1, forceRefresh: Boolean = false) {
        // If we are already listening to this location/floor, do nothing (unless forced)
        // The active listener will handle real-time updates (like new posts)
        if (!forceRefresh && _currentLocationId.value == locationId && _loadedFloor == floor && _currentStories.value.isNotEmpty()) {
            return
        }

        // Clean up old listener before starting a new one
        storyListener?.remove()

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

        Log.d("StoryViewModel", "Fetching stories for location: $locationId, type: $locationType")

        // Store the registration so it stays active and can be cleaned up
        storyListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("StoryViewModel", "Error fetching stories for location", e)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                return@addSnapshotListener
            }
            // Real-time update: this runs whenever data changes (including new posts)
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
        // Use viewModelScope to handle threading automatically
        viewModelScope.launch {
            val storage = FirebaseStorage.getInstance()

            // 1. Process all documents in parallel using async
            val deferredStories = documents.map { doc ->
                async {
                    val extractedLoc = locationId ?: doc.reference.path.split("/").getOrNull(3) ?: ""

                    // [ADDED] Check if the document path contains "outdoor_locations"
                    val isOutdoor = doc.reference.path.contains("outdoor_locations")

                    // Safely convert document to object
                    val story = try {
                        // [MODIFIED] Logic to conditionally set floor to null
                        var model = doc.toObject(StoryModel::class.java)

                        if (isOutdoor && model != null) {
                            model = model.copy(floor = null)
                        }

                        // Apply ID and Location Name to the model
                        model?.copy(id = doc.id, locationName = extractedLoc)

                    } catch (e: Exception) {
                        Log.e("StoryViewModel", "Error parsing doc: ${doc.id}", e)
                        null
                    }

                    // If story exists, resolve the URL
                    if (story != null) {
                        if (story.audioUrl?.startsWith("gs://") == true) {
                            try {
                                // Convert Task to suspend function with .await()
                                val uri = storage.getReferenceFromUrl(story.audioUrl).downloadUrl.await()
                                story.playableUrl = uri.toString()
                            } catch (e: Exception) {
                                Log.e("StoryViewModel", "Error resolving audio URL for ${story.id}", e)
                                // Fallback: keep original or set empty
                                story.playableUrl = ""
                            }
                        } else {
                            story.playableUrl = story.audioUrl ?: ""
                        }
                    }

                    story
                }
            }

            // 2. Wait for all async operations to complete and filter out nulls
            val storiesList = deferredStories.awaitAll().filterNotNull()
            for (story in storiesList) {
                Log.d("StoryDebug", "Fetched Story ID: ${story.id}")
            }
            // 3. Update the state (this runs on the Main thread automatically in viewModelScope)
            if (isAllStories) {
                _allStories.value = storiesList
                Log.d("StoryViewModel", "Updated allStories with ${storiesList.size} items")
            } else {
                _currentStories.value = storiesList
                Log.d("StoryViewModel", "Updated currentStories with ${storiesList.size} items")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        storyListener?.remove()
    }
}