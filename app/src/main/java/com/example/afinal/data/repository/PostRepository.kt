package com.example.afinal.data.repository

import android.net.Uri
import com.example.afinal.data.model.Comment
import com.example.afinal.data.model.PostModel
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PostRepository {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val postsCollection = db.collection("posts")

    suspend fun addPost(
        post: PostModel,
        imageUris: List<Uri>,
        audioUri: Uri?,
        isIndoor: Boolean?,
        locationId: String?,
        floor: Int?
    ): String {
        Log.d("PostRepository", "addPost called with post: $post, isIndoor: $isIndoor, locationId: $locationId, floor: $floor")

        val locationType = when (isIndoor) {
            true -> "indoor_locations"
            false -> "outdoor_locations"
            else -> null
        }

        val storagePath = if (locationType != null && locationId != null) {
            if (isIndoor == true && floor != null) {
                "locations/locations/$locationType/$locationId/floor/$floor"
            } else {
                "locations/locations/$locationType/$locationId"
            }
        } else {
            "posts"
        }

        val imageUrls = imageUris.map { uploadFile(it, "$storagePath/images") }
        Log.d("PostRepository", "imageUrls: $imageUrls")
        val audioUrl = audioUri?.let { uploadFile(it, "$storagePath/audio") }
        Log.d("PostRepository", "audioUrl: $audioUrl")

        val newPost = post.copy(
            pictures = imageUrls,
            audioUrl = audioUrl
        )
        Log.d("PostRepository", "newPost: $newPost")

        val collection = if (locationType != null && locationId != null) {
            val locationsRoot = db.collection("locations").document("locations")
            if (isIndoor == true && floor != null) {
                locationsRoot.collection(locationType)
                    .document(locationId)
                    .collection("floor").document(floor.toString())
                    .collection("posts")
            } else {
                locationsRoot.collection(locationType)
                    .document(locationId)
                    .collection("posts")
            }
        } else {
            postsCollection
        }

        val documentReference = collection.add(newPost).await()
        Log.d("PostRepository", "documentReference id: ${documentReference.id}")
        return documentReference.id
    }

    private suspend fun uploadFile(fileUri: Uri, folder: String): String {
        val fileName = UUID.randomUUID().toString()
        val storageRef = storage.reference.child("$folder/$fileName")
        storageRef.putFile(fileUri).await()
        return storageRef.downloadUrl.await().toString()
    }

    suspend fun addComment(postId: String, comment: Comment) {
        postsCollection.document(postId).collection("comments").add(comment).await()
    }

    fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = postsCollection.document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.toObjects<Comment>())
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addReaction(postId: String, userId: String, reactionType: String) {
        val reactionDocRef = postsCollection.document(postId).collection("reactions").document(userId)
        reactionDocRef.set(mapOf("type" to reactionType)).await()
    }

    fun getReactions(postId: String): Flow<Map<String, Int>> = callbackFlow {
        val listener = postsCollection.document(postId).collection("reactions")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val reactionCounts = mutableMapOf<String, Int>()
                    for (document in it.documents) {
                        val type = document.getString("type") ?: ""
                        reactionCounts[type] = (reactionCounts[type] ?: 0) + 1
                    }
                    trySend(reactionCounts)
                }
            }
        awaitClose { listener.remove() }
    }
}
