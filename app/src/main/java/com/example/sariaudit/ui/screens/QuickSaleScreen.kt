package com.example.sariaudit.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sariaudit.data.Product
import com.example.sariaudit.ui.theme.*
import com.example.sariaudit.viewmodel.CartItem
import com.example.sariaudit.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSaleScreen(navController: NavController, viewModel: MainViewModel) {
    val products by viewModel.allProducts.collectAsState()
    val cartItems by viewModel.cart.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val filteredProducts = products.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Sale", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealGreen),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // 1. SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Item...") },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            // 2. PRODUCT GRID
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductGridItem(product) { viewModel.addToCart(product) }
                }
            }

            // 3. CART SECTION
            Surface(
                modifier = Modifier.fillMaxWidth().height(350.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Current Cart (${cartItems.sumOf { it.quantity }} items)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Divider(Modifier.padding(vertical = 8.dp))

                    // Cart List
                    LazyColumn(Modifier.weight(1f)) {
                        items(cartItems) { item ->
                            CartRowItem(
                                item = item,
                                onUpdateQty = { q -> viewModel.updateCartQuantity(item.product, q) },
                                onRemove = { viewModel.removeFromCart(item.product) }
                            )
                        }
                    }

                    Divider(Modifier.padding(vertical = 8.dp))

                    // TOTAL & BUTTONS ROW
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Amount", style = MaterialTheme.typography.bodySmall)
                            Text("₱%.2f".format(cartTotal), style = MaterialTheme.typography.headlineMedium, color = TealGreen, fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // CANCEL BUTTON
                            OutlinedButton(
                                onClick = { viewModel.clearCart() },
                                enabled = cartItems.isNotEmpty(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                border = BorderStroke(1.dp, if(cartItems.isNotEmpty()) ErrorRed else Color.LightGray),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel")
                            }

                            // CHECKOUT BUTTON
                            Button(
                                onClick = { showConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = TealGreen),
                                enabled = cartItems.isNotEmpty(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                            ) {
                                Text("CHECK OUT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Transaction") },
            text = {
                Column {
                    Text("You are about to complete this sale.")
                    Spacer(Modifier.height(8.dp))
                    Text("Total Items: ${cartItems.sumOf { it.quantity }}")
                    Text("Total Amount: ₱%.2f".format(cartTotal), fontWeight = FontWeight.Bold, color = TealGreen)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.checkoutCart()
                        showConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealGreen)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProductGridItem(product: Product, onClick: () -> Unit) {
    val outOfStock = product.quantity <= 0

    // CHANGED: Increased height to 110.dp to ensure all text fits
    Card(
        modifier = Modifier
            .height(110.dp)
            .clickable(enabled = !outOfStock, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (outOfStock) Color.LightGray else OffWhite),
        border = BorderStroke(1.dp, Color.Gray),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(6.dp), // Increased padding slightly for breathing room
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Keep centered vertically
        ) {
            // ICON
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.example.sariaudit.R.drawable.ic_package_2),
                contentDescription = null,
                tint = TealGreen,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.height(4.dp))

            // NAME: Added 'minLines' to reserve space for 2 lines even if text is short,
            // ensuring alignment across the grid
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.1 // Slightly tighter line height if needed
            )

            // PRICE
            Text(
                text = "₱${product.sellingPrice}",
                color = DeepOrange,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )

            // QUANTITY
            Text(
                text = "${product.quantity} left",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun CartRowItem(item: CartItem, onUpdateQty: (Int) -> Unit, onRemove: () -> Unit) {
    var qtyText by remember(item.quantity) { mutableStateOf(item.quantity.toString()) }

    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        // Name & Price
        Column(Modifier.weight(1.2f)) {
            Text(item.product.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text("₱${item.product.sellingPrice}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }

        // Quantity Controls
        Row(Modifier.weight(1.5f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = { onUpdateQty(item.quantity - 1) }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Remove, null)
            }

            OutlinedTextField(
                value = qtyText,
                onValueChange = {
                    qtyText = it
                    val newQty = it.toIntOrNull()
                    if (newQty != null) onUpdateQty(newQty)
                },
                modifier = Modifier.width(60.dp).height(50.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                singleLine = true
            )

            IconButton(onClick = { onUpdateQty(item.quantity + 1) }, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Add, null)
            }
        }

        // Trash
        IconButton(onClick = onRemove, modifier = Modifier.weight(0.3f)) {
            Icon(Icons.Default.Delete, null, tint = ErrorRed)
        }
    }
}