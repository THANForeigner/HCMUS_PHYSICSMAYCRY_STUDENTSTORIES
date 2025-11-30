package com.example.afinal.data.repository

import android.net.Uri
import com.example.afinal.data.model.Comment
import com.example.afinal.data.model.PostModel
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

    suspend fun addPost(post: PostModel, imageUris: List<Uri>, audioUri: Uri?): String {
        val imageUrls = imageUris.map { uploadFile(it, "images") }
        val audioUrl = audioUri?.let { uploadFile(it, "audio") }

        val newPost = post.copy(
            pictures = imageUrls,
            audioUrl = audioUrl
        )

        val documentReference = postsCollection.add(newPost).await()
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
