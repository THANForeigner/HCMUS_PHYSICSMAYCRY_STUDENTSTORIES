package com.example.afinal.ui.screen

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.afinal.models.LocationViewModel
import com.example.afinal.models.PostViewModel
import com.example.afinal.models.StoryViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object DataRepository {
    suspend fun uploadData(
        context: Context,
        ngrokUrl: String,
        collectionName: String,
        title: String,
        description: String,
        tags: String,
        userId : String,
        userEmail : String,
        userName: String,
        latitude: String,
        longitude: String,
        textInput: String?,
        audioUri: Uri?,
        imageUri: Uri?
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                var finalUrl = ngrokUrl.trim()
                if (finalUrl.endsWith("/")) finalUrl = finalUrl.dropLast(1)
                if (!finalUrl.endsWith("/api/process")) finalUrl += "/api/process"

                val client = OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.MINUTES)
                    .writeTimeout(10, TimeUnit.MINUTES)
                    .readTimeout(10, TimeUnit.MINUTES)
                    .build()

                val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
                multipart.addFormDataPart("collection_name", collectionName)
                multipart.addFormDataPart("title", title)
                multipart.addFormDataPart("description", description)
                multipart.addFormDataPart("list_tags", tags)
                multipart.addFormDataPart("user_id", userId)
                multipart.addFormDataPart("user_email", userEmail)
                multipart.addFormDataPart("user_name", userName)
                multipart.addFormDataPart("latitude", latitude)
                multipart.addFormDataPart("longitude", longitude)
                if (imageUri != null) {
                    val file = uriToFile(context, imageUri)
                    if (file != null) {
                        multipart.addFormDataPart("image_file", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                    }
                }

                if (textInput != null) {
                    multipart.addFormDataPart("text_input", textInput)
                } else if (audioUri != null) {
                    val file = uriToFile(context, audioUri)
                    if(file != null) multipart.addFormDataPart("audio_file", file.name, file.asRequestBody("audio/mpeg".toMediaTypeOrNull()))
                }

                val request = Request.Builder().url(finalUrl).post(multipart.build()).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) "" else "Server Error: ${response.code}"
            } catch (e: Exception) {
                "Connection Error: ${e.message}"
            }
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.mp3")
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) { e.printStackTrace(); null }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(
    navController: NavController,
    postViewModel: PostViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel(),
    storyViewModel: StoryViewModel = viewModel()
) {
    val location by locationViewModel.location
    val currentLocationId by storyViewModel.currentLocationId
    val currentLocation by storyViewModel.currentLocation
    val currentFloor by storyViewModel.currentFloor
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isAnonymous by remember { mutableStateOf(false) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var ngrokUrl by remember { mutableStateOf("https://emergently-basipetal-marge.ngrok-free.dev") }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var textInput by remember { mutableStateOf("") }
    var tagsInput by remember {
        mutableStateOf("Romance, Pet, Mysteries, Facilities information, Health, Food and drink, Social and communities, Personal experience, Warning, Study Hacks, Library Vibes, Confessions, Motivation, Gaming, Burnout, Emotional support, Important Announcement, Academic Contest, Clubs, Sport, Achievement, Academic Collaboration, Amazing Individuals, Cyber problems warning, Social Activities, Seminar, After-class Activities, Volunteer Campaigns, Online Activities, Club Recruitment, Questions & Answers, Scholarship")
    }

    var selectedFloor by remember { mutableStateOf<Int?>(null) }
    var floorDropdownExpanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> imageUri = uri }
    val audioPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> audioUri = uri }

    fun handleUploadToColab() {
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }
        if (name.isBlank()) {
            Toast.makeText(context, "Please enter Title", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedTabIndex == 0 && audioUri == null) {
            Toast.makeText(context, "Please select Audio", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedTabIndex == 1 && textInput.isBlank()) {
            Toast.makeText(context, "Please enter Text", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        coroutineScope.launch {
            val latStr = location?.latitude?.toString() ?: ""
            val lngStr = location?.longitude?.toString() ?: ""

            val collectionPath = if (currentLocationId != null && currentLocation != null) {
                if (currentLocation!!.type == "indoor") {
                    "locations/locations/indoor_locations/${currentLocationId!!}/floor/${currentFloor}/posts"
                } else {
                    "locations/locations/outdoor_locations/${currentLocationId!!}/posts"
                }
            } else {
                "records"
            }

            val error = DataRepository.uploadData(
                context = context,
                ngrokUrl = ngrokUrl,
                collectionName = collectionPath,
                title = name,
                description = description,
                tags = tagsInput,
                userId = currentUser.uid,
                userEmail = currentUser.email ?: "No Email",
                userName = if (isAnonymous) "Anonymous" else (currentUser.displayName ?: "Anonymous"),
                latitude = latStr,
                longitude = lngStr,
                textInput = if (selectedTabIndex == 1) textInput else null,
                audioUri = if (selectedTabIndex == 0) audioUri else null,
                imageUri = imageUri
            )
            isLoading = false
            if (error.isEmpty()) {
                Toast.makeText(context, "Your post has been uploaded!", Toast.LENGTH_LONG).show()
                navController.popBackStack()
            } else {
                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create AI Story", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Button(
                onClick = { handleUploadToColab() },
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload to AI")
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri, contentDescription = "Selected Image",
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Add an image (optional)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Title") },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isAnonymous = !isAnonymous },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isAnonymous,
                    onCheckedChange = { isAnonymous = it }
                )
                Text(
                    text = "Post anonymously",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Row { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(4.dp)); Text("Audio File") } }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Row { Icon(Icons.Default.TextFields, null); Spacer(Modifier.width(4.dp)); Text("Text Input") } }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            if (selectedTabIndex == 0) {
                Card(
                    onClick = { if (audioUri == null) audioPickerLauncher.launch("audio/*") else audioUri = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (audioUri != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (audioUri != null) MaterialTheme.colorScheme.primary else Color.Gray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (audioUri != null) Icons.Default.AudioFile else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (audioUri != null) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (audioUri != null) "Selected Audio" else "Select audio file",
                                fontWeight = FontWeight.Bold,
                                color = if (audioUri != null) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified
                            )
                            if (audioUri != null) {
                                Text(text = "Tap to remove", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (audioUri != null) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Type your content here...") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}