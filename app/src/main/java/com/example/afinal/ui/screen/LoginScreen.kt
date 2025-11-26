package com.example.afinal.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.afinal.AuthViewModel
import com.example.afinal.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val authViewModel: AuthViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }

    fun performLogin() {
        focusManager.clearFocus()
        coroutineScope.launch {
            val success = authViewModel.login(email, password)
            if (success) {
                navController.navigate(Routes.MAIN_APP) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            } else {
                errorMessage = "Login failed. Please check your credentials."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Student Stories", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        // 3. Cấu hình ô Email
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusRequester.requestFocus() }
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { performLogin() }
            )
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { performLogin() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = { navController.navigate(Routes.REGISTER) }
        ) {
            Text("Don't have an account? Register")
        }
    }
}