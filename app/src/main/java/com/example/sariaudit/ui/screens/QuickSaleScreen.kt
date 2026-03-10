package com.example.sariaudit.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSaleScreen(navController: NavController, viewModel: MainViewModel) {
    val products by viewModel.allProducts.collectAsState()
    val cartItems by viewModel.cart.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filteredProducts = products.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Sale", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = TealGreen),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Item...") },
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductGridItem(product) {
                        val success = viewModel.addToCart(product)
                        if (!success) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Cannot add more ${product.name}. Only ${product.quantity} in stock.")
                            }
                        }
                    }
                }
            }

            // Cart Section
            Surface(
                modifier = Modifier.fillMaxWidth().height(350.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Cart (${cartItems.sumOf { it.quantity }})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Divider(Modifier.padding(vertical = 8.dp))

                    LazyColumn(Modifier.weight(1f)) {
                        items(cartItems) { item ->
                            CartRowItem(
                                item = item,
                                onUpdateQty = { q -> viewModel.updateCartQuantity(item.product, q) },
                                onRemove = { viewModel.removeFromCart(item.product) },
                                snackbarHostState = snackbarHostState,
                                scope = scope
                            )
                        }
                    }

                    Divider(Modifier.padding(vertical = 8.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total", style = MaterialTheme.typography.bodySmall)
                            Text("₱%.2f".format(cartTotal), style = MaterialTheme.typography.headlineMedium, color = TealGreen, fontWeight = FontWeight.Bold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { viewModel.clearCart() }, enabled = cartItems.isNotEmpty()) { Text("Clear") }
                            Button(onClick = { showConfirmDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = TealGreen), enabled = cartItems.isNotEmpty()) {
                                Text("CHECKOUT")
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
            title = { Text("Confirm Sale") },
            text = { Text("Complete transaction for ₱%.2f?".format(cartTotal)) },
            confirmButton = {
                Button(onClick = { viewModel.checkoutCart(); showConfirmDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = TealGreen)) {
                    Text("Confirm")
                }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ProductGridItem(product: Product, onClick: () -> Unit) {
    val outOfStock = product.quantity <= 0
    Card(
        modifier = Modifier.height(110.dp).clickable(enabled = !outOfStock, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (outOfStock) Color.LightGray else OffWhite),
        border = BorderStroke(1.dp, Color.Gray)
    ) {
        Column(Modifier.fillMaxSize().padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(painter = androidx.compose.ui.res.painterResource(id = com.example.sariaudit.R.drawable.ic_package_2), contentDescription = null, tint = TealGreen, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(product.name, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, maxLines = 2, minLines = 2)
            Text("₱${product.sellingPrice}", color = DeepOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text("${product.quantity} left", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
fun CartRowItem(item: CartItem, onUpdateQty: (Int) -> Unit, onRemove: () -> Unit, snackbarHostState: SnackbarHostState? = null, scope: CoroutineScope? = null) {
    // Keep a local state of the text so the user can easily clear and type
    var qtyText by remember(item.quantity) { mutableStateOf(item.quantity.toString()) }

    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.product.name, fontWeight = FontWeight.Bold)
            Text("₱${item.product.sellingPrice}", style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onUpdateQty(item.quantity - 1) }) { Icon(Icons.Default.Remove, null) }

            // Editable Quantity Field
            BasicTextField(
                value = qtyText,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        qtyText = newValue
                        // Only update the cart when there's an actual number to commit
                        val parsedQty = newValue.toIntOrNull()
                        if (parsedQty != null && parsedQty > 0) {
                            onUpdateQty(parsedQty)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                modifier = Modifier
                    .width(40.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp))
                    .padding(vertical = 4.dp)
            )

            IconButton(onClick = { 
                val newQty = item.quantity + 1
                if (newQty > item.product.quantity) {
                    scope?.launch {
                        snackbarHostState?.showSnackbar("Cannot add more ${item.product.name}. Only ${item.product.quantity} in stock.")
                    }
                } else {
                    onUpdateQty(newQty)
                }
            }) { Icon(Icons.Default.Add, null) }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = ErrorRed) }
        }
    }
}