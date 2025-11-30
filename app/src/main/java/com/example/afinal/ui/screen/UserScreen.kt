package com.example.afinal.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.afinal.AuthViewModel
import com.example.afinal.navigation.Routes

@Composable
fun UserScreen(mainNavController: NavController) {
  val authViewModel: AuthViewModel = viewModel()
  Column(
          modifier = Modifier.fillMaxSize().padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
  ) {
    Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Avatar",
            modifier = Modifier.size(120.dp),
            tint = Color.Gray
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text("user@example.com", style = MaterialTheme.typography.titleLarge)
    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = { mainNavController.navigate(Routes.ADD_POST) }
    ) {
        Text("Add Post")
    }

    Spacer(modifier = Modifier.height(16.dp))

    Button(
            onClick = {
              authViewModel.logout()
              mainNavController.navigate(Routes.LOGIN) {
                popUpTo(Routes.MAIN_APP) { inclusive = true }
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
    ) { Text("Logout") }
  }
}

