package com.example.sariaudit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sariaudit.data.Product
import com.example.sariaudit.data.Utang
import com.example.sariaudit.ui.theme.*
import com.example.sariaudit.viewmodel.MainViewModel
import com.example.sariaudit.viewmodel.UserRole

enum class DashProdSort(val description: String) {
    NameAsc("Name (A-Z)"),
    NameDesc("Name (Z-A)"),
    PriceHigh("Price (High-Low)"),
    PriceLow("Price (Low-High)"),
    QuantityHigh("Quantity (High-Low)"),
    QuantityLow("Quantity (Low-High)")
}

enum class DashUtangSort(val description: String) {
    AmountHigh("Amount (High-Low)"),
    AmountLow("Amount (Low-High)"),
    NameAsc("Name (A-Z)"),
    NameDesc("Name (Z-A)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: MainViewModel) {
    val userRole by viewModel.userRole.collectAsState()
    val currentStore by viewModel.currentStore.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val sales by viewModel.allSales.collectAsState()
    val utang by viewModel.allUtang.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val lowStock by viewModel.lowStockProducts.collectAsState()

    // Dialog States
    var showProfileDialog by remember { mutableStateOf(false) }
    var showLowStockDialog by remember { mutableStateOf(false) }
    var showRoleManagerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // --- DASHBOARD FILTER & SORT STATES ---
    var invSearch by remember { mutableStateOf("") }
    var invSortExpanded by remember { mutableStateOf(false) }
    var invSort by remember { mutableStateOf(DashProdSort.NameAsc) }

    var utangSearch by remember { mutableStateOf("") }
    var utangSortExpanded by remember { mutableStateOf(false) }
    var utangSort by remember { mutableStateOf(DashUtangSort.AmountHigh) }

    // --- FILTER & SORT LOGIC ---
    val displayProducts = products
        .filter { it.name.contains(invSearch, ignoreCase = true) }
        .let {
            when (invSort) {
                DashProdSort.NameAsc -> it.sortedBy { p -> p.name }
                DashProdSort.NameDesc -> it.sortedByDescending { p -> p.name }
                DashProdSort.PriceHigh -> it.sortedByDescending { p -> p.sellingPrice }
                DashProdSort.PriceLow -> it.sortedBy { p -> p.sellingPrice }
                DashProdSort.QuantityHigh -> it.sortedByDescending { p -> p.quantity }
                DashProdSort.QuantityLow -> it.sortedBy { p -> p.quantity }
            }
        }
        .take(5)

    // FIX: Filter based on actual balance > 0.01 instead of isPaid
    val displayUtang = utang
        .filter { it.amount > 0.01 && it.customerName.contains(utangSearch, ignoreCase = true) }
        .let {
            when (utangSort) {
                DashUtangSort.AmountHigh -> it.sortedByDescending { u -> u.amount }
                DashUtangSort.AmountLow -> it.sortedBy { u -> u.amount }
                DashUtangSort.NameAsc -> it.sortedBy { u -> u.customerName }
                DashUtangSort.NameDesc -> it.sortedByDescending { u -> u.customerName }
            }
        }
        .take(3)

    val totalSales = sales.sumOf { it.totalAmount }
    val totalProfit = sales.sumOf { it.profit }

    // FIX: Consistent calculation for total active debt
    val totalUtang = utang.filter { it.amount > 0.01 }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentStore?.name ?: "SariAudit", color = Color.White)
                        Text(
                            text = "Role: ${userRole.name} | Code: ${currentStore?.accessCode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(Icons.Default.AccountCircle, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepOrange)
            )
        },
        bottomBar = { BottomNavBar(navController, userRole) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- METRICS TILES ---
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Total Sales", "₱%.2f".format(totalSales), Icons.Default.ShoppingCart, DeepOrange, Modifier.weight(1f))

                if (userRole != UserRole.ASSISTANT) {
                    MetricCard("Total Profit", "₱%.2f".format(totalProfit), Icons.Default.TrendingUp, TealGreen, Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Total Utang", "₱%.2f".format(totalUtang), Icons.Default.People, Color.Blue, Modifier.weight(1f))

                val hasLowStock = lowStock.isNotEmpty()
                val stockColor = if (hasLowStock) ErrorRed else SuccessGreen
                val stockIcon = if (hasLowStock) Icons.Default.Warning else Icons.Default.CheckCircle

                ClickableMetricCard(
                    title = "Low Stock Items",
                    value = "${lowStock.size}",
                    icon = stockIcon,
                    color = stockColor,
                    modifier = Modifier.weight(1f),
                    onClick = { if (hasLowStock) showLowStockDialog = true }
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- INVENTORY SNAPSHOT ---
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Inventory Snapshot", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Box {
                    IconButton(onClick = { invSortExpanded = true }) { Icon(Icons.Default.Sort, null) }
                    DropdownMenu(expanded = invSortExpanded, onDismissRequest = { invSortExpanded = false }) {
                        DashProdSort.entries.forEach { sortOption ->
                            DropdownMenuItem(
                                text = { Text(sortOption.description) },
                                onClick = { invSort = sortOption; invSortExpanded = false },
                                trailingIcon = { if (invSort == sortOption) Icon(Icons.Default.Check, "Selected") }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = invSearch,
                onValueChange = { invSearch = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                placeholder = { Text("Search items...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
            )

            if (displayProducts.isEmpty()) {
                Text("No items found.", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                displayProducts.forEach { MiniProductRow(it) }
            }
            TextButton(onClick = { navController.navigate("inventory") }, modifier = Modifier.align(Alignment.End)) {
                Text("View All Inventory")
            }

            Spacer(Modifier.height(16.dp))

            // --- ACTIVE DEBTORS ---
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Active Debtors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Box {
                    IconButton(onClick = { utangSortExpanded = true }) { Icon(Icons.Default.Sort, null) }
                    DropdownMenu(expanded = utangSortExpanded, onDismissRequest = { utangSortExpanded = false }) {
                        DashUtangSort.entries.forEach { sortOption ->
                            DropdownMenuItem(
                                text = { Text(sortOption.description) },
                                onClick = { utangSort = sortOption; utangSortExpanded = false },
                                trailingIcon = { if (utangSort == sortOption) Icon(Icons.Default.Check, "Selected") }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = utangSearch,
                onValueChange = { utangSearch = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                placeholder = { Text("Search debtor...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
            )

            if (displayUtang.isEmpty()) {
                Text("No active debts.", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
            } else {
                displayUtang.forEach { MiniUtangRow(it) }
            }
            TextButton(onClick = { navController.navigate("utang") }, modifier = Modifier.align(Alignment.End)) {
                Text("View All Debtors")
            }

            Spacer(Modifier.height(50.dp))
        }
    }

    // --- Profile & Manager Dialogs (Logic unchanged) ---
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Hello, ${currentUser?.displayName ?: "User"}") },
            text = {
                Column {
                    Text("Currently managing: ${currentStore?.name}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { showProfileDialog = false; navController.navigate("store_select") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Store, null); Spacer(Modifier.width(8.dp)); Text("Switch Store")
                    }
                    if (userRole == UserRole.OWNER) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.fetchStoreMembers(); showRoleManagerDialog = true; showProfileDialog = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DeepOrange)) {
                            Icon(Icons.Default.People, null); Spacer(Modifier.width(8.dp)); Text("Manage Team")
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { showDeleteConfirmDialog = true; showProfileDialog = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) {
                            Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Delete Store")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.logout(); navController.navigate("login") { popUpTo(0) } }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) { Text("Logout") }
            },
            dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text("Close") } }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Store?") },
            text = { Text("Are you sure you want to delete '${currentStore?.name}'? \n\nThis action cannot be undone.", color = Color.Black) },
            confirmButton = {
                Button(onClick = { viewModel.deleteStore { navController.navigate("store_select") { popUpTo(0) } }; showDeleteConfirmDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)) { Text("Delete Forever") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") } }
        )
    }

    // Low Stock Dialog (Logic unchanged)
    if (showLowStockDialog) {
        AlertDialog(
            onDismissRequest = { showLowStockDialog = false },
            title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Warning, null, tint = ErrorRed); Spacer(Modifier.width(8.dp)); Text("Low Stock Alerts", color = ErrorRed) } },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(lowStock) { product ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column { Text(product.name, fontWeight = FontWeight.Bold); Text("Threshold: ${product.lowStockThreshold}", style = MaterialTheme.typography.labelSmall, color = Color.Gray) }
                            Text("${product.quantity} ${product.unit}", color = ErrorRed, fontWeight = FontWeight.Bold)
                        }
                        Divider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLowStockDialog = false }) { Text("Close") } }
        )
    }
}

// --- DASHBOARD COMPONENTS ---

@Composable
fun MetricCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(modifier = modifier.height(100.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ClickableMetricCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(100.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MiniProductRow(product: Product) {
    val isLow = product.quantity <= product.lowStockThreshold
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold)
                Text("₱${product.sellingPrice}", color = DeepOrange, style = MaterialTheme.typography.bodySmall)
            }
            Text("${product.quantity} ${product.unit}", color = if(isLow) ErrorRed else Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MiniUtangRow(utang: Utang) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(utang.customerName, fontWeight = FontWeight.Bold) }
            Text("₱%.2f".format(utang.amount), color = ErrorRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, role: UserRole) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(selected = false, onClick = { navController.navigate("dashboard") }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
        NavigationBarItem(selected = false, onClick = { navController.navigate("inventory") }, icon = { Icon(Icons.Default.Inventory, null) }, label = { Text("Stock") })
        NavigationBarItem(selected = false, onClick = { navController.navigate("quicksale") }, icon = { Icon(Icons.Default.FlashOn, null) }, label = { Text("Sale") })
        NavigationBarItem(selected = false, onClick = { navController.navigate("utang") }, icon = { Icon(Icons.Default.Receipt, null) }, label = { Text("Utang") })
        if (role == UserRole.ADMIN || role == UserRole.OWNER) {
            NavigationBarItem(selected = false, onClick = { navController.navigate("analytics") }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("Data") })
        }
        NavigationBarItem(selected = false, onClick = { navController.navigate("about") }, icon = { Icon(Icons.Default.Info, null) }, label = { Text("About") })
    }
}