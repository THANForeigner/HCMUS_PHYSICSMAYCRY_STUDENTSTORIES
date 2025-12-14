package com.example.afinal.ui.screen

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.*
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
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.afinal.navigation.Routes
import com.example.afinal.ui.theme.AppGradients
import com.example.afinal.models.StoryViewModel
import com.example.afinal.navigation.AppNavigation
import com.example.afinal.ui.component.StoryCard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

// Giữ nguyên Data Class
data class UserProfile(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val avatarUrl: String? = ""
)

@Composable
fun UserScreen(
    mainNavController: NavController,
    storyViewModel: StoryViewModel
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    val database = remember { FirebaseDatabase.getInstance().getReference("users") }
    val storageRef = remember { FirebaseStorage.getInstance().reference }
    val context = LocalContext.current

    val allStories by storyViewModel.allStories
    val myStories = remember(allStories, currentUser) {
        if (currentUser != null) {
            allStories.filter { it.user_id == currentUser.uid }
                .sortedByDescending { it.created_at }
        } else {
            emptyList()
        }
    }

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            database.child(user.uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    userProfile = snapshot.getValue(UserProfile::class.java)
                }
                isFetching = false
            }.addOnFailureListener { isFetching = false }
        }
    }

    fun uploadAvatar(uri: Uri) {
        val uid = currentUser?.uid ?: return
        isLoading = true
        val avatarRef = storageRef.child("avatars/$uid.jpg")

        avatarRef.putFile(uri).addOnSuccessListener {
            avatarRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                val url = downloadUrl.toString()
                database.child(uid).child("avatarUrl").setValue(url).addOnSuccessListener {
                    userProfile = userProfile?.copy(avatarUrl = url)
                    isLoading = false
                    Toast.makeText(context, "Avatar updated", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            isLoading = false
            Toast.makeText(context, "Upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { uploadAvatar(it) } }
    )

    fun logout() {
        auth.signOut()
        mainNavController.navigate(Routes.LOGIN) {
            popUpTo(Routes.LOGIN) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // 1. (Header Background)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = AppGradients.userScreen
                )
        )

        // 2. Nội dung chính (Scrollable)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header: Title "Profile" và Icon Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.Transparent
                )

                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.clickable { /* Handle Settings click */ }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Card 1: User Info (Avatar, Name, Email) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar Box
                    Box(
                        contentAlignment = Alignment.BottomEnd,
                        modifier = Modifier
                            .size(100.dp)
                            .clickable {
                                if (!isLoading) photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        if (userProfile?.avatarUrl.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp)) // Bo góc vuông mềm (Squircle) giống mẫu
                                    .background(Color(0xFFE0E0E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp),
                                    tint = Color.White
                                )
                            }
                        } else {
                            Image(
                                painter = rememberAsyncImagePainter(userProfile?.avatarUrl),
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color.LightGray),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Loading Indicator
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }

                        // Edit Icon nhỏ
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Avatar",
                            modifier = Modifier
                                .offset(x = 6.dp, y = 6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .padding(4.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name
                    Text(
                        text = userProfile?.name ?: "No Name",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    // Email
                    Text(
                        text = userProfile?.email ?: currentUser?.email ?: "",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Section: Your Stories ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Stories (${myStories.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                if (myStories.isEmpty()) {
                    // Empty State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PostAdd, null, tint = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You haven't posted any stories yet.",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(myStories) { story ->
                            Box(modifier = Modifier.fillParentMaxWidth()) {
                                StoryCard(
                                    story = story,
                                    onClick = { mainNavController.navigate("${Routes.AUDIO_PLAYER}/${story.id}") }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Card: Sign Out ---
            Card(
                onClick = { logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign Out",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}