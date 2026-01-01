package com.example.afinal.data.model

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afinal.data.network.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.collections.iterator

class StoryViewModel : ViewModel() {
    // --- STATE MANAGEMENT ---
    private val _locations = mutableStateOf<List<LocationModel>>(emptyList())
    val locations: State<List<LocationModel>> = _locations

    // Helper lists to store the latest data from each collection separately
    private var _indoorList = listOf<LocationModel>()
    private var _outdoorList = listOf<LocationModel>()

    private val _currentStories = mutableStateOf<List<Story>>(emptyList())
    val currentStories: State<List<Story>> = _currentStories

    private val _allStories = mutableStateOf<List<Story>>(emptyList())
    val allStories: State<List<Story>> = _allStories

    private val _recommendedStories = mutableStateOf<List<Story>>(emptyList())
    val recommendedStories: State<List<Story>> = _recommendedStories

    private val _comments = mutableStateOf<List<Comment>>(emptyList())
    val comments: State<List<Comment>> = _comments

    private val _reactions = mutableStateOf<List<Reaction>>(emptyList())
    val reactions: State<List<Reaction>> = _reactions

    private val _isIndoor = mutableStateOf(false)
    val isIndoor: State<Boolean> = _isIndoor

    private val _currentFloor = mutableStateOf(1)
    val currentFloor: State<Int> = _currentFloor

    private val _currentLocationId = mutableStateOf<String?>(null)
    val currentLocationId: State<String?> = _currentLocationId

    // --- DERIVED STATE ---
    val topTrendingStories = derivedStateOf {
        _allStories.value.sortedByDescending { story ->
            story.reactionsCount + story.commentsCount
        }.take(5)
    }

    val hotLocations = derivedStateOf {
        val stories = _allStories.value
        val locs = _locations.value
        val locationCounts = stories.groupingBy { it.locationName }.eachCount()

        locs.filter { locationCounts.containsKey(it.id) }
            .sortedByDescending { locationCounts[it.id] ?: 0 }
            .take(5)
    }

    val currentLocation = derivedStateOf {
        _locations.value.find { it.id == _currentLocationId.value }
    }

    // --- INTERNAL VARIABLES ---
    private var _loadedFloor = 0
    private var storyListener: ListenerRegistration? = null

    init {
        fetchLocations()
        fetchAllStories()

        viewModelScope.launch {
            snapshotFlow { currentLocation.value }
                .collectLatest {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "test_user"
                    fetchRecommendations(userId)
                }
        }
    }

    // --- API & DATA FETCHING ---

    fun fetchRecommendations(userId: String) {
        viewModelScope.launch {
            try {
                val nameBuilding = currentLocation.value?.locationName ?: ""
                val request = RecommendRequest(userId = userId, nameBuilding = nameBuilding)
                val response = RetrofitClient.api.getRecommendations(request)
                if (response.isSuccessful) {
                    val audioItems = response.body()?.results ?: emptyList()
                    _recommendedStories.value = audioItems.map { it.toStory() }
                } else {
                    Log.e("StoryViewModel", "Failed to fetch recommendations: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("StoryViewModel", "Error fetching recommendations", e)
            }
        }
    }

    fun getStory(id: String): Story? {
        return _currentStories.value.find { it.id == id }
            ?: _allStories.value.find { it.id == id }
    }

    /**
     * Get DocumentReference of story from Firestore
     */
    fun getStoryDocumentReference(storyId: String, locationId: String? = null): DocumentReference? {
        val db = FirebaseFirestore.getInstance()
        val locId = locationId ?: _currentLocationId.value

        if (locId == null) {
            // If no locationId, find story in list to get locationName
            val story = getStory(storyId)
            if (story == null) {
                Log.e("StoryViewModel", "Cannot find story $storyId to get document reference")
                return null
            }
            // Find location from locationName
            val location = _locations.value.find { it.id == story.locationName }
            if (location == null) {
                Log.e("StoryViewModel", "Cannot find location ${story.locationName} for story $storyId")
                return null
            }
            // Default floor = 1 because Story class doesn't store floor
            return buildDocumentReference(db, storyId, location.id, location.type, 1)
        }

        val location = _locations.value.find { it.id == locId }
        if (location == null) {
            Log.e("StoryViewModel", "Cannot find location $locId")
            return null
        }

        val floor = _currentFloor.value
        return buildDocumentReference(db, storyId, locId, location.type, floor)
    }

    fun getStoryDocumentPath(storyId: String, locationId: String? = null): String? {
        return getStoryDocumentReference(storyId, locationId)?.path
    }

    private fun buildDocumentReference(
        db: FirebaseFirestore,
        storyId: String,
        locationId: String,
        locationType: String,
        floor: Int
    ): DocumentReference {
        return if (locationType == "indoor") {
            db.collection("locations").document("locations")
                .collection("indoor_locations").document(locationId)
                .collection("floor").document(floor.toString())
                .collection("posts").document(storyId)
        } else {
            db.collection("locations").document("locations")
                .collection("outdoor_locations").document(locationId)
                .collection("posts").document(storyId)
        }
    }

    // --- LOCATION MANAGEMENT ---

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

    // --- STORY FETCHING ---

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
        if (!forceRefresh && _currentLocationId.value == locationId && _loadedFloor == floor && _currentStories.value.isNotEmpty()) {
            return
        }

        // Clean up old listener before starting a new one
        storyListener?.remove()

        _currentLocationId.value = locationId
        _currentFloor.value = floor

        if (floor == 0) {
            val storiesInLocation = _allStories.value.filter { it.locationName == locationId }
            _currentStories.value = storiesInLocation
            _loadedFloor = floor
            val locationType = _locations.value.find { it.id == locationId }?.type ?: "outdoor"
            setIndoorStatus(locationType == "indoor")

            Log.d("StoryViewModel", "Loaded ${storiesInLocation.size} stories for All Floors in $locationId")
            return
        }

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
        viewModelScope.launch {
            val storage = FirebaseStorage.getInstance()

            val deferredStories = documents.map { doc ->
                async {
                    val extractedLoc = locationId ?: doc.reference.path.split("/").getOrNull(3) ?: ""

                    // Parse to Story class
                    val story = try {
                        var model = doc.toObject(Story::class.java)
                        // Assign ID and locationName through .copy()
                        model = model?.copy(id = doc.id, locationName = extractedLoc)
                        model
                    } catch (e: Exception) {
                        Log.e("StoryViewModel", "Error parsing doc: ${doc.id}", e)
                        null
                    }

                    // Process Audio URL (gs:// -> https://)
                    if (story != null) {
                        if (story.audioUrl.startsWith("gs://")) {
                            try {
                                val uri = storage.getReferenceFromUrl(story.audioUrl).downloadUrl.await()
                                story.audioUrl = uri.toString()
                            } catch (e: Exception) {
                                Log.e("StoryViewModel", "Error resolving audio URL for ${story.id}", e)
                            }
                        }

                        // Fetch comments count
                        val commentsSnapshot = doc.reference.collection("comments").get().await()
                        story.commentsCount = commentsSnapshot.size()

                        // Fetch reactions count
                        val reactionsSnapshot = doc.reference.collection("reactions").get().await()
                        story.reactionsCount = reactionsSnapshot.size()
                    }
                    story
                }
            }

            val storiesList = deferredStories.awaitAll().filterNotNull()
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

    // --- INTERACTION (Comments, Reactions) ---

    fun addComment(storyId: String, comment: Comment, locationId: String? = null) {
        val storyRef = getStoryDocumentReference(storyId, locationId)

        if (storyRef == null) {
            Log.e("StoryViewModel", "Cannot add comment, story reference not found for story $storyId")
            return
        }

        storyRef.collection("comments").add(comment)
            .addOnSuccessListener {
                Log.d("StoryViewModel", "Comment added successfully")
                viewModelScope.launch {
                    try {
                        val request = CommentRequest(
                            userId = comment.userId,
                            audioFirestoreId = storyId,
                            content = comment.comment
                        )
                        RetrofitClient.api.sendComment(request)
                        Log.d("StoryViewModel", "Successfully sent comment to API")
                    } catch (e: Exception) {
                        Log.e("StoryViewModel", "Failed to send comment to API", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("StoryViewModel", "Error adding comment", e)
            }
    }

    fun addReaction(storyId: String, reaction: Reaction, isNew: Boolean, locationId: String? = null) {
        val storyRef = getStoryDocumentReference(storyId, locationId)

        if (storyRef == null) {
            Log.e("StoryViewModel", "Cannot add reaction, story reference not found for story $storyId")
            return
        }

        storyRef.collection("reactions").document(reaction.userId).set(reaction)
            .addOnSuccessListener {
                Log.d("StoryViewModel", "Reaction added successfully")
                if (isNew) {
                    updateStoryReactionCount(storyId, 1)
                }
                viewModelScope.launch {
                    try {
                        val request = InteractRequest(
                            userId = reaction.userId,
                            audioFirestoreId = storyId,
                            action = reaction.type.lowercase()
                        )
                        RetrofitClient.api.sendInteraction(request)
                        Log.d("StoryViewModel", "Successfully sent interaction to API")
                    } catch (e: Exception) {
                        Log.e("StoryViewModel", "Failed to send interaction to API", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("StoryViewModel", "Error adding reaction", e)
            }
    }

    fun removeReaction(storyId: String, userId: String, locationId: String? = null) {
        val storyRef = getStoryDocumentReference(storyId, locationId)
        if (storyRef == null) {
            Log.e("StoryViewModel", "Cannot remove reaction, story reference not found for story $storyId")
            return
        }

        storyRef.collection("reactions").document(userId).delete()
            .addOnSuccessListener {
                Log.d("StoryViewModel", "Reaction removed successfully")
                updateStoryReactionCount(storyId, -1)
                viewModelScope.launch {
                    try {
                        val request = InteractRequest(
                            userId = userId,
                            audioFirestoreId = storyId,
                            action = "unreact"
                        )
                        RetrofitClient.api.sendInteraction(request)
                        Log.d("StoryViewModel", "Successfully sent un-reaction to API")
                    } catch (e: Exception) {
                        Log.e("StoryViewModel", "Failed to send un-reaction to API", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("StoryViewModel", "Error removing reaction", e)
            }
    }

    private fun updateStoryReactionCount(storyId: String, delta: Int) {
        val storiesToUpdate = listOf(_currentStories, _allStories)
        for (storyListState in storiesToUpdate) {
            val storyList = storyListState.value
            val storyIndex = storyList.indexOfFirst { it.id == storyId }
            if (storyIndex != -1) {
                val story = storyList[storyIndex]
                val updatedStory = story.copy(reactionsCount = story.reactionsCount + delta)
                val updatedList = storyList.toMutableList()
                updatedList[storyIndex] = updatedStory
                storyListState.value = updatedList
            }
        }
    }

    fun getComments(storyId: String, locationId: String? = null) {
        val storyRef = getStoryDocumentReference(storyId, locationId)
        if (storyRef == null) {
            Log.e("StoryViewModel", "Cannot get comments, story reference not found for story $storyId")
            return
        }

        storyRef.collection("comments").orderBy("timestamp")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("StoryViewModel", "Error fetching comments", e)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    _comments.value = it.toObjects(Comment::class.java)
                }
            }
    }

    fun getReactions(storyId: String, locationId: String? = null) {
        val storyRef = getStoryDocumentReference(storyId, locationId)
        if (storyRef == null) {
            Log.e("StoryViewModel", "Cannot get reactions, story reference not found for story $storyId")
            return
        }

        storyRef.collection("reactions")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("StoryViewModel", "Error fetching reactions", e)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    _reactions.value = it.toObjects(Reaction::class.java)
                }
            }
    }

    fun getStoryCountForLocation(locationId: String): Int {
        return _allStories.value.count { it.locationName == locationId }
    }
}