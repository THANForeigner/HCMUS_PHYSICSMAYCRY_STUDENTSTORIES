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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

data class UserProfile(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val avatarUrl: String? = ""
)

@Composable
fun UserScreen(mainNavController: NavController) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = auth.currentUser
    val database = remember { FirebaseDatabase.getInstance().getReference("users") }
    val storageRef = remember { FirebaseStorage.getInstance().reference }
    val context = LocalContext.current

    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(true) }

    // Fetch user data on init
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

    // Upload avatar logic
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

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "My Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
        )

        if (isFetching) {
            CircularProgressIndicator()
        } else {
            // Avatar Section
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(120.dp)
                    .clickable {
                        if (!isLoading) photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
            ) {
                if (userProfile?.avatarUrl.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color.Gray
                        )
                    }
                } else {
                    Image(
                        painter = rememberAsyncImagePainter(userProfile?.avatarUrl),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(6.dp)
                        .size(16.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // User Info
            Text(
                text = userProfile?.name ?: "No Name",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = userProfile?.email ?: currentUser?.email ?: "No Email",
                fontSize = 16.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = { logout() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Log Out", color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
