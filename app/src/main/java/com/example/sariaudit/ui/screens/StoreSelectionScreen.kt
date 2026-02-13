package com.example.sariaudit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sariaudit.data.Store
import com.example.sariaudit.ui.theme.DeepOrange
import com.example.sariaudit.ui.theme.ErrorRed
import com.example.sariaudit.ui.theme.OffWhite
import com.example.sariaudit.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreSelectionScreen(navController: NavController, viewModel: MainViewModel) {
    val userStores by viewModel.userStores.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState() // Needed for the greeting

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) } // New State

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select a Store", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepOrange),
                actions = {
                    // PROFILE BUTTON
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(Icons.Default.AccountCircle, null, tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(OffWhite)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Icon
            Icon(Icons.Default.Store, null, tint = DeepOrange, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(24.dp))

            // Store List
            if (userStores.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("You are not a member of any store yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(userStores) { store ->
                        StoreItem(store) {
                            viewModel.selectStore(store)
                            navController.navigate("dashboard") { popUpTo("store_select") { inclusive = true } }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            if (errorMsg != null) {
                Text(errorMsg!!, color = Color.Red)
                Spacer(Modifier.height(8.dp))
            }

            // Action Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { showJoinDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Join Store")
                }
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepOrange)
                ) {
                    Text("Create Store")
                }
            }
        }
    }

    // DIALOGS
    if (showCreateDialog) CreateStoreDialog(onDismiss = { showCreateDialog = false }, onCreate = { viewModel.createStore(it); showCreateDialog = false })
    if (showJoinDialog) JoinStoreDialog(onDismiss = { showJoinDialog = false }, onJoin = { viewModel.joinStore(it); showJoinDialog = false })

    // PROFILE / LOGOUT DIALOG
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Hello, ${currentUser?.displayName ?: "User"}") },
            text = {
                Column {
                    Text("You can create a new store or join an existing one using a 6-digit code.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        navController.navigate("login") { popUpTo(0) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text("Close") } }
        )
    }

    // LOADING OVERLAY
    if (isLoading) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.3f)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DeepOrange)
        }
    }
}

@Composable
fun StoreItem(store: Store, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(store.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Code: ${store.accessCode}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun CreateStoreDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Store") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Store Name") }, singleLine = true) },
        confirmButton = { Button(onClick = { onCreate(name) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun JoinStoreDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Existing Store") },
        text = { OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("6-Digit Store Code") }, singleLine = true) },
        confirmButton = { Button(onClick = { onJoin(code) }) { Text("Join") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}