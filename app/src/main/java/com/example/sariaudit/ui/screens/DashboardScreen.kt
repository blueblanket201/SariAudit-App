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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: MainViewModel) {
    val userRole by viewModel.userRole.collectAsState()
    val sales by viewModel.allSales.collectAsState()
    val utang by viewModel.allUtang.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val lowStock by viewModel.lowStockProducts.collectAsState()

    // --- DASHBOARD STATE ---
    var inventorySearch by remember { mutableStateOf("") }
    var showLowStockDialog by remember { mutableStateOf(false) } // State for the popup

    // Sort States
    var invSortOption by remember { mutableStateOf(ProductSort.NameAsc) }
    var invSortExpanded by remember { mutableStateOf(false) }

    var utangSortOption by remember { mutableStateOf(UtangSort.AmountHighLow) }
    var utangSortExpanded by remember { mutableStateOf(false) }

    // --- FILTER LOGIC ---
    val displayProducts = products
        .filter { it.name.contains(inventorySearch, ignoreCase = true) }
        .let { list ->
            when (invSortOption) {
                ProductSort.NameAsc -> list.sortedBy { it.name }
                ProductSort.NameDesc -> list.sortedByDescending { it.name }
                ProductSort.QtyHighLow -> list.sortedByDescending { it.quantity }
                ProductSort.QtyLowHigh -> list.sortedBy { it.quantity }
                ProductSort.PriceHighLow -> list.sortedByDescending { it.sellingPrice }
                ProductSort.PriceLowHigh -> list.sortedBy { it.sellingPrice }
            }
        }
        .take(5)

    val displayUtang = utang
        .filter { !it.isPaid }
        .let { list ->
            when (utangSortOption) {
                UtangSort.AmountHighLow -> list.sortedByDescending { it.amount }
                UtangSort.AmountLowHigh -> list.sortedBy { it.amount }
                UtangSort.NameAsc -> list.sortedBy { it.customerName }
                UtangSort.NameDesc -> list.sortedByDescending { it.customerName }
                UtangSort.NewestFirst -> list.sortedByDescending { it.dateCreated }
                UtangSort.OldestFirst -> list.sortedBy { it.dateCreated }
            }
        }
        .take(3)

    val totalSales = sales.sumOf { it.totalAmount }
    val totalProfit = sales.sumOf { it.profit }
    val totalUtang = utang.filter { !it.isPaid }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SariAudit Dashboard", color = Color.White)
                        Text(if (userRole == UserRole.ADMIN) "Store Owner" else "Employee", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.logout(); navController.navigate("login") }) {
                        Icon(Icons.Default.ExitToApp, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepOrange)
            )
        },
        bottomBar = { BottomNavBar(navController, userRole) }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            // --- METRICS SECTION ---
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Total Sales", "₱%.2f".format(totalSales), Icons.Default.ShoppingCart, DeepOrange, Modifier.weight(1f))
                if (userRole == UserRole.ADMIN) {
                    MetricCard("Total Profit", "₱%.2f".format(totalProfit), Icons.Default.TrendingUp, TealGreen, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Total Utang", "₱%.2f".format(totalUtang), Icons.Default.People, Color.Blue, Modifier.weight(1f))

                // LOW STOCK ALERT CARD
                if (lowStock.isNotEmpty()) {
                    Card(
                        // CHANGED: Click triggers dialog instead of navigation
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable { showLowStockDialog = true },
                        colors = CardDefaults.cardColors(containerColor = ErrorLight),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Warning, null, tint = ErrorRed)
                            Text("Low Stock", color = ErrorRed, style = MaterialTheme.typography.labelMedium)
                            Text("${lowStock.size} Items", style = MaterialTheme.typography.titleLarge, color = ErrorRed)
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- INVENTORY SECTION ---
            Text("Inventory Snapshot (Top 5)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // Search & Sort Row
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = inventorySearch,
                    onValueChange = { inventorySearch = it },
                    placeholder = { Text("Search...") },
                    modifier = Modifier.weight(1f).height(50.dp),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))

                // INVENTORY SORT DROPDOWN
                Box {
                    IconButton(onClick = { invSortExpanded = true }) {
                        Icon(Icons.Default.SortByAlpha, null, tint = DeepOrange)
                    }
                    DropdownMenu(
                        expanded = invSortExpanded,
                        onDismissRequest = { invSortExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Name (A-Z)") }, onClick = { invSortOption = ProductSort.NameAsc; invSortExpanded = false })
                        DropdownMenuItem(text = { Text("Name (Z-A)") }, onClick = { invSortOption = ProductSort.NameDesc; invSortExpanded = false })
                        Divider()
                        DropdownMenuItem(text = { Text("Stock (Low-High)") }, onClick = { invSortOption = ProductSort.QtyLowHigh; invSortExpanded = false })
                        DropdownMenuItem(text = { Text("Stock (High-Low)") }, onClick = { invSortOption = ProductSort.QtyHighLow; invSortExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            displayProducts.forEach { product ->
                MiniProductRow(product)
            }
            TextButton(onClick = { navController.navigate("inventory") }, modifier = Modifier.align(Alignment.End)) {
                Text("View All Inventory")
            }

            Spacer(Modifier.height(16.dp))

            // --- DEBTORS SECTION ---
            Text("Active Debtors (Top 3)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // Sort Row for Utang
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Sort Debtors", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.weight(1f))

                // UTANG SORT DROPDOWN
                Box {
                    IconButton(onClick = { utangSortExpanded = true }) {
                        Icon(Icons.Default.Sort, null, tint = Color.Blue)
                    }
                    DropdownMenu(
                        expanded = utangSortExpanded,
                        onDismissRequest = { utangSortExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Amount (High-Low)") }, onClick = { utangSortOption = UtangSort.AmountHighLow; utangSortExpanded = false })
                        DropdownMenuItem(text = { Text("Amount (Low-High)") }, onClick = { utangSortOption = UtangSort.AmountLowHigh; utangSortExpanded = false })
                        Divider()
                        DropdownMenuItem(text = { Text("Name (A-Z)") }, onClick = { utangSortOption = UtangSort.NameAsc; utangSortExpanded = false })
                        DropdownMenuItem(text = { Text("Newest First") }, onClick = { utangSortOption = UtangSort.NewestFirst; utangSortExpanded = false })
                    }
                }
            }

            displayUtang.forEach { debtor ->
                MiniUtangRow(debtor)
            }
            TextButton(onClick = { navController.navigate("utang") }, modifier = Modifier.align(Alignment.End)) {
                Text("View All Debtors")
            }

            // Spacer for bottom nav
            Spacer(Modifier.height(50.dp))
        }
    }

    // --- LOW STOCK DIALOG ---
    if (showLowStockDialog) {
        AlertDialog(
            onDismissRequest = { showLowStockDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = ErrorRed)
                    Spacer(Modifier.width(8.dp))
                    Text("Low Stock Items", color = ErrorRed)
                }
            },
            text = {
                // Scrollable list of low stock items
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp).fillMaxWidth()) {
                    items(lowStock) { product ->
                        Column(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold)
                                    Text("Threshold: ${product.lowStockThreshold}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                                Text(
                                    "${product.quantity} ${product.unit}",
                                    color = ErrorRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLowStockDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Close")
                }
            }
        )
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
            Column(Modifier.weight(1f)) {
                Text(utang.customerName, fontWeight = FontWeight.Bold)
            }
            Text("₱${utang.amount}", color = ErrorRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, role: UserRole) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("dashboard") },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("inventory") },
            icon = { Icon(Icons.Default.Inventory, null) },
            label = { Text("Stock") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("quicksale") },
            icon = { Icon(Icons.Default.FlashOn, null) },
            label = { Text("Sale") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("utang") },
            icon = { Icon(Icons.Default.Receipt, null) },
            label = { Text("Utang") }
        )
        if (role == UserRole.ADMIN) {
            NavigationBarItem(
                selected = false,
                onClick = { navController.navigate("analytics") },
                icon = { Icon(Icons.Default.BarChart, null) },
                label = { Text("Data") }
            )
        }
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("about") },
            icon = { Icon(Icons.Default.Info, null) },
            label = { Text("About") }
        )
    }
}