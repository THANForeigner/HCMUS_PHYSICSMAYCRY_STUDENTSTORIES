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
import androidx.compose.ui.platform.LocalContext
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
fun RegisterScreen(navController: NavController) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    val authViewModel: AuthViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 1. Khởi tạo FocusManager và các FocusRequester
    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }

    // 2. Tách logic Register ra hàm riêng
    fun performRegister() {
        focusManager.clearFocus() // Ẩn bàn phím
        if (password != confirmPassword) {
            errorMessage = "Passwords do not match"
        } else {
            coroutineScope.launch {
                val success = authViewModel.register(email, password)
                if (success) {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                } else {
                    errorMessage = "Registration failed. Please try again."
                }
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
        Text("Create an account", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        // 3. Email -> Next -> Password
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

        // 4. Password -> Next -> Confirm Password
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
                .focusRequester(passwordFocusRequester), // Gắn requester 1
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next // Vẫn là Next để xuống Confirm
            ),
            keyboardActions = KeyboardActions(
                onNext = { confirmPasswordFocusRequester.requestFocus() }
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 5. Confirm Password -> Done -> Register
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = null
            },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(confirmPasswordFocusRequester), // Gắn requester 2
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done // Kết thúc tại đây
            ),
            keyboardActions = KeyboardActions(
                onDone = { performRegister() }
            )
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { performRegister() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign up")
        }
        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = { navController.popBackStack() }
        ) {
            Text("Already have an account? Login")
        }
    }
}