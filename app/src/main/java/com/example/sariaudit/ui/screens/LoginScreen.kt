package com.example.sariaudit.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.sariaudit.ui.theme.DeepOrange
import com.example.sariaudit.ui.theme.DeepBlue
import com.example.sariaudit.ui.theme.OffWhite
import com.example.sariaudit.viewmodel.MainViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun LoginScreen(viewModel: MainViewModel, onLoginSuccess: () -> Unit) {
    // --- STATE VARIABLES ---
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Name States
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }

    val loginError by viewModel.errorMsg.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val registrationSuccess by viewModel.registrationSuccess.collectAsState()
    val context = LocalContext.current

    val modeColor = if (isRegistering) DeepBlue else DeepOrange

    // --- LISTENERS ---
    LaunchedEffect(currentUser) {
        if (currentUser != null && !isRegistering) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(registrationSuccess) {
        if (registrationSuccess) {
            Toast.makeText(context, "Registration Successful! Please Sign In.", Toast.LENGTH_LONG).show()
            isRegistering = false
            email = ""
            password = ""
            confirmPassword = ""
            firstName = ""
            lastName = ""
            viewModel.resetRegistrationState()
        }
    }

    // --- Google Sign-In Setup ---
    val token = "1011818955917-l3vokc8s7sgqkqdne845t99np407dii8.apps.googleusercontent.com"
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(token)
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(Exception::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            Firebase.auth.signInWithCredential(credential)
                .addOnSuccessListener {
                    // IMPORTANT: Manually save Google User to Database now that auto-sync is off
                    viewModel.finalizeGoogleLogin()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Google Sign-In Error", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(OffWhite),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Store,
            contentDescription = null,
            tint = modeColor,
            modifier = Modifier.size(80.dp)
        )

        Text(
            text = if (isRegistering) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // --- Name Fields (Only visible when Registering) ---
                if (isRegistering) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it; viewModel.clearError() },
                            label = { Text("First Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = modeColor,
                                focusedLabelColor = modeColor
                            )
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it; viewModel.clearError() },
                            label = { Text("Last Name") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = modeColor,
                                focusedLabelColor = modeColor
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; viewModel.clearError() },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = modeColor) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = modeColor,
                        focusedLabelColor = modeColor
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; viewModel.clearError() },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = modeColor) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = modeColor,
                        focusedLabelColor = modeColor
                    )
                )

                // Confirm Password Field
                if (isRegistering) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; viewModel.clearError() },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.LockClock, null, tint = modeColor) },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = modeColor,
                            focusedLabelColor = modeColor
                        )
                    )
                }

                // Error Message
                if (loginError != null) {
                    Text(text = loginError!!, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Button
                Button(
                    onClick = {
                        if (isRegistering) {
                            if (password != confirmPassword) {
                                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.registerWithEmail(email, password, firstName, lastName)
                            }
                        } else {
                            viewModel.loginWithEmail(email, password)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = modeColor),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(if (isRegistering) "Register" else "Sign In")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google Button
                OutlinedButton(
                    onClick = { launcher.launch(googleSignInClient.signInIntent) },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Sign in with Google", color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle Text
        Text(
            text = if (isRegistering) "Already have an account? Sign In" else "Don't have an account? Register",
            color = modeColor,
            modifier = Modifier.clickable {
                isRegistering = !isRegistering
                viewModel.clearError()
                password = ""
                confirmPassword = ""
                firstName = ""
                lastName = ""
            }
        )
    }
}