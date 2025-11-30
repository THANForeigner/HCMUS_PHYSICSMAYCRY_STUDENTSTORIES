package com.example.afinal.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.afinal.PostViewModel
import com.example.afinal.data.model.Comment
import com.example.afinal.data.model.PostModel
import com.example.afinal.data.model.Position
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun AddPostScreen(postViewModel: PostViewModel = viewModel()) {
    var name by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var postId by remember { mutableStateOf<String?>(null) }
    val comments by postViewModel.comments.collectAsState()
    val reactions by postViewModel.reactions.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        imageUris = uris
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        audioUri = uri
    }

    if (postId == null) {
        Column(modifier = Modifier.padding(16.dp)) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Post Name") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("Select Images")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { audioPickerLauncher.launch("audio/*") }) {
                Text("Select Audio")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val newPost = PostModel(
                    name = name,
                    position = Position(37.422, -122.084) // Example position
                )
                postId = postViewModel.addPost(newPost, imageUris, audioUri)
            }) {
                Text("Add Post")
            }
        }
    } else {
        postId?.let {
            LaunchedEffect(it) {
                postViewModel.loadComments(it)
                postViewModel.loadReactions(it)
            }
            PostDetailScreen(
                postId = it,
                comments = comments,
                reactions = reactions,
                postViewModel = postViewModel
            )
        }
    }
}

@Composable
fun PostDetailScreen(
    postId: String,
    comments: List<Comment>,
    reactions: Map<String, Int>,
    postViewModel: PostViewModel
) {
    var commentText by remember { mutableStateOf("") }
    val userId = Firebase.auth.currentUser?.uid ?: ""

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Post ID: $postId")
        Spacer(modifier = Modifier.height(16.dp))
        Text("Reactions: $reactions")
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = { postViewModel.addReaction(postId, userId, "like") }) {
                Text("Like")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { postViewModel.addReaction(postId, userId, "love") }) {
                Text("Love")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Comments:")
        LazyColumn {
            items(comments) { comment ->
                Text("${comment.userId}: ${comment.comment}")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = commentText,
            onValueChange = { commentText = it },
            label = { Text("Add a comment") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val newComment = Comment(
                userId = userId,
                comment = commentText,
                timestamp = System.currentTimeMillis()
            )
            postViewModel.addComment(postId, newComment)
            commentText = ""
        }) {
            Text("Post Comment")
        }
    }
}
