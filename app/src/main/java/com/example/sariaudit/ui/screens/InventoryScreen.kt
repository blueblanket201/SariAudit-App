package com.example.sariaudit.ui.screens

//import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sariaudit.data.Product
import com.example.sariaudit.ui.theme.*
import com.example.sariaudit.viewmodel.MainViewModel
import com.example.sariaudit.viewmodel.UserRole

// Define Sort Options
enum class ProductSort {
    NameAsc, NameDesc, QtyHighLow, QtyLowHigh, PriceHighLow, PriceLowHigh
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(navController: NavController, viewModel: MainViewModel) {
    val products by viewModel.allProducts.collectAsState()
    val role by viewModel.userRole.collectAsState()

    // Dialog States
    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) } // NEW: Edit State
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var productToRestock by remember { mutableStateOf<Product?>(null) }

    // Search & Sort States
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(ProductSort.NameAsc) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Filter & Sort Logic
    val filteredProducts = products
        .filter { it.name.contains(searchQuery, ignoreCase = true) }
        .let { list ->
            when (sortOption) {
                ProductSort.NameAsc -> list.sortedBy { it.name }
                ProductSort.NameDesc -> list.sortedByDescending { it.name }
                ProductSort.QtyHighLow -> list.sortedByDescending { it.quantity }
                ProductSort.QtyLowHigh -> list.sortedBy { it.quantity }
                ProductSort.PriceHighLow -> list.sortedByDescending { it.sellingPrice }
                ProductSort.PriceLowHigh -> list.sortedBy { it.sellingPrice }
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepOrange),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    // SORT BUTTON
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("Name (A-Z)") }, onClick = { sortOption = ProductSort.NameAsc; sortMenuExpanded = false })
                            DropdownMenuItem(text = { Text("Name (Z-A)") }, onClick = { sortOption = ProductSort.NameDesc; sortMenuExpanded = false })
                            Divider()
                            DropdownMenuItem(text = { Text("Quantity (High to Low)") }, onClick = { sortOption = ProductSort.QtyHighLow; sortMenuExpanded = false })
                            DropdownMenuItem(text = { Text("Quantity (Low to High)") }, onClick = { sortOption = ProductSort.QtyLowHigh; sortMenuExpanded = false })
                            Divider()
                            DropdownMenuItem(text = { Text("Price (High to Low)") }, onClick = { sortOption = ProductSort.PriceHighLow; sortMenuExpanded = false })
                            DropdownMenuItem(text = { Text("Price (Low to High)") }, onClick = { sortOption = ProductSort.PriceLowHigh; sortMenuExpanded = false })
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            // Add Button (Owner/Admin Only usually, or allow Assistant to add? We'll allow all based on your previous code)
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = DeepOrange) {
                Icon(Icons.Default.Add, null, tint = Color.White)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search products...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            // List
            LazyColumn {
                items(filteredProducts) { product ->
                    ProductItem(
                        product = product,
                        role = role,
                        onDeleteClick = { productToDelete = product },
                        onRestockClick = { productToRestock = product },
                        onEditClick = { productToEdit = product } // Pass Edit Action
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. ADD DIALOG
    if (showAddDialog) {
        ProductFormDialog(
            title = "Add Product",
            initialProduct = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { newProduct ->
                viewModel.addProduct(newProduct)
                showAddDialog = false
            }
        )
    }

    // 2. EDIT DIALOG
    productToEdit?.let { product ->
        ProductFormDialog(
            title = "Edit Product Details",
            initialProduct = product,
            onDismiss = { productToEdit = null },
            onConfirm = { updatedProduct ->
                viewModel.updateProduct(updatedProduct)
                productToEdit = null
            }
        )
    }

    // 3. DELETE CONFIRMATION
    productToDelete?.let { product ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete '${product.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(product)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // 4. RESTOCK DIALOG
    productToRestock?.let { product ->
        var quantityToAdd by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { productToRestock = null },
            title = { Text("Restock ${product.name}") },
            text = {
                Column {
                    Text("Current Stock: ${product.quantity}")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quantityToAdd,
                        onValueChange = { quantityToAdd = it },
                        label = { Text("Quantity to Add") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = quantityToAdd.toIntOrNull() ?: 0
                        if (qty > 0) {
                            val updatedProduct = product.copy(quantity = product.quantity + qty)
                            viewModel.updateProduct(updatedProduct)
                        }
                        productToRestock = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Add Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToRestock = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProductItem(
    product: Product,
    role: UserRole,
    onDeleteClick: () -> Unit,
    onRestockClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val isLowStock = product.quantity <= product.lowStockThreshold
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = if (isLowStock) ErrorLight else Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Stock: ${product.quantity} ${product.unit}", color = if (isLowStock) ErrorRed else Color.Gray)
                Text("₱${product.sellingPrice}", color = DeepOrange, fontWeight = FontWeight.Bold)
            }

            // ACTION BUTTONS
            Row {
                // Restock Button (Visible to everyone)
                IconButton(onClick = onRestockClick) {
                    Icon(Icons.Default.AddBox, null, tint = SuccessGreen)
                }

                // Edit & Delete (Owner/Admin Only)
                if (role == UserRole.OWNER || role == UserRole.ADMIN) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, null, tint = DeepOrange)
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, null, tint = ErrorRed)
                    }
                }
            }
        }
    }
}

// Reusable Dialog for Adding AND Editing
@Composable
fun ProductFormDialog(
    title: String,
    initialProduct: Product?,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    // Pre-fill state if editing, otherwise empty
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var price by remember { mutableStateOf(initialProduct?.sellingPrice?.toString() ?: "") }
    var cost by remember { mutableStateOf(initialProduct?.costPrice?.toString() ?: "") }
    var qty by remember { mutableStateOf(initialProduct?.quantity?.toString() ?: "") }
    var threshold by remember { mutableStateOf(initialProduct?.lowStockThreshold?.toString() ?: "10") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Sell Price") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { cost = it },
                        label = { Text("Cost") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = threshold,
                        onValueChange = { threshold = it },
                        label = { Text("Low Alert") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // If editing, keep the old ID. If adding, ID is empty string (ViewModel handles generation)
                val p = Product(
                    id = initialProduct?.id ?: "",
                    name = name,
                    sellingPrice = price.toDoubleOrNull() ?: 0.0,
                    costPrice = cost.toDoubleOrNull() ?: 0.0,
                    quantity = qty.toIntOrNull() ?: 0,
                    lowStockThreshold = threshold.toIntOrNull() ?: 10
                )
                onConfirm(p)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}