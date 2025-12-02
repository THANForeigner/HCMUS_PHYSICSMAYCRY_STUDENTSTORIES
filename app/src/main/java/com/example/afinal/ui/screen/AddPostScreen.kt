package com.example.afinal.ui.screen

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.afinal.LocationViewModel
import com.example.afinal.PostViewModel
import com.example.afinal.StoryViewModel
import com.example.afinal.data.model.Position
import com.example.afinal.data.model.PostModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun AddPostScreen(
        navController: NavController,
        postViewModel: PostViewModel = viewModel(),
        locationViewModel: LocationViewModel = viewModel(),
        storyViewModel: StoryViewModel = viewModel()
) {
  var name by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var imageUri by remember { mutableStateOf<Uri?>(null) }
  var audioUri by remember { mutableStateOf<Uri?>(null) }
  var isLoading by remember { mutableStateOf(false) } // Loading state
  val coroutineScope = rememberCoroutineScope()
  val location by locationViewModel.location
  val isIndoor by storyViewModel.isIndoor

  val imagePickerLauncher =
          rememberLauncherForActivityResult(
                  contract = ActivityResultContracts.GetContent()
          ) { uri: Uri? -> imageUri = uri }

  val audioPickerLauncher =
          rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
                  uri: Uri? ->
            audioUri = uri
          }

  Box(modifier = Modifier.fillMaxSize()) { // Use Box to overlay loading indicator
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
      TextField(value = name, onValueChange = { name = it }, label = { Text("Post Name") })
      Spacer(modifier = Modifier.height(16.dp))
      TextField(
              value = description,
              onValueChange = { description = it },
              label = { Text("Post Description") }
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(
              onClick = { imagePickerLauncher.launch("image/*") },
              enabled = imageUri == null && !isLoading
      ) { Text(if (imageUri == null) "Select Image" else "Image Selected") }
      Spacer(modifier = Modifier.height(16.dp))
      Button(
              onClick = { audioPickerLauncher.launch("audio/*") },
              enabled = audioUri == null && !isLoading
      ) { Text(if (audioUri == null) "Select Audio" else "Audio Selected") }
      Spacer(modifier = Modifier.height(16.dp))
      val context = LocalContext.current
      val currentLocationId by storyViewModel.currentLocationId
      val currentFloor by storyViewModel.currentFloor
      Button(
              onClick = {
                if (Firebase.auth.currentUser == null) {
                  Toast.makeText(context, "Please log in to add a post", Toast.LENGTH_SHORT).show()
                  return@Button
                }

                // DEBUG: Assume outdoor location
                // val debugLocationId = "Park" // Example outdoor location ID
                // val debugIsIndoor = false
                // val debugFloor = 1 // Irrelevant for outdoor, but keeping for type consistency

                val newPost =
                        PostModel(
                                name = name,
                                description = description,
                                position = location?.let { Position(it.latitude, it.longitude) }
                        )
                isLoading = true // Start loading
                coroutineScope.launch {
                  try {
                    val postId =
                            postViewModel.addPost(
                                    newPost,
                                    imageUri?.let { listOf(it) } ?: emptyList(),
                                    audioUri,
                                    isIndoor,
                                    currentLocationId,
                                    1,
                                    // debugIsIndoor, // Use debug value
                                    // debugLocationId, // Use debug value
                                    // debugFloor // Use debug value
                                    )
                    if (postId.isNotBlank()) {
                      navController.popBackStack()
                    }
                  } catch (e: Exception) {
                    Log.e("AddPostScreen", "Error adding post", e)
                    Toast.makeText(context, "Error adding post: ${e.message}", Toast.LENGTH_LONG)
                            .show()
                  } finally {
                    isLoading = false // Stop loading
                  }
                }
              },
              enabled = !isLoading // Disable button while loading
      ) { Text("Add Post") }
    }

    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
  }
}
