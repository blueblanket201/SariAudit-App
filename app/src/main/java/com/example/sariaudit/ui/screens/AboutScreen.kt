package com.example.sariaudit.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sariaudit.ui.theme.DeepOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("About SariAudit", color = Color.White) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepOrange),
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } })
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Info, null, tint = DeepOrange, modifier = Modifier.size(64.dp))
            Text("SariAudit", style = MaterialTheme.typography.headlineMedium)
            Text("Version 1.0.0", color = Color.Gray)
            Spacer(Modifier.height(24.dp))
            Text("SariAudit is a comprehensive management system designed for sari-sari stores in the Philippines.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))
            Text("Developers", style = MaterialTheme.typography.titleMedium)
            Text("Rowvi Mar Juan", style = MaterialTheme.typography.bodyMedium)
            Text("Mille Gifford Paner", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.weight(1f))
            Text("© 2026 SariAudit. All rights reserved.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}