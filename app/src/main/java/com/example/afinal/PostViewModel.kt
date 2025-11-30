package com.example.afinal

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afinal.data.model.Comment
import com.example.afinal.data.model.PostModel
import com.example.afinal.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostViewModel : ViewModel() {
    private val postRepository = PostRepository()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _reactions = MutableStateFlow<Map<String, Int>>(emptyMap())
    val reactions: StateFlow<Map<String, Int>> = _reactions.asStateFlow()

    fun addPost(post: PostModel, imageUris: List<Uri>, audioUri: Uri?): String {
        var postId = ""
        viewModelScope.launch {
            postId = postRepository.addPost(post, imageUris, audioUri)
        }
        return postId
    }

    fun loadComments(postId: String) {
        viewModelScope.launch {
            postRepository.getComments(postId).collect {
                _comments.value = it
            }
        }
    }

    fun addComment(postId: String, comment: Comment) {
        viewModelScope.launch {
            postRepository.addComment(postId, comment)
        }
    }

    fun loadReactions(postId: String) {
        viewModelScope.launch {
            postRepository.getReactions(postId).collect {
                _reactions.value = it
            }
        }
    }

    fun addReaction(postId: String, userId: String, reactionType: String) {
        viewModelScope.launch {
            postRepository.addReaction(postId, userId, reactionType)
        }
    }
}
