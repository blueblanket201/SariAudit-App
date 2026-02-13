package com.example.sariaudit.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sariaudit.data.Utang
import com.example.sariaudit.data.UtangTransaction
import com.example.sariaudit.ui.theme.DeepOrange
import com.example.sariaudit.ui.theme.ErrorRed
import com.example.sariaudit.ui.theme.SuccessGreen
import com.example.sariaudit.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class UtangSort {
    AmountHighLow, AmountLowHigh, NameAsc, NameDesc, NewestFirst, OldestFirst
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtangScreen(navController: NavController, viewModel: MainViewModel) {
    val utangList by viewModel.allUtang.collectAsState()
    val transactions by viewModel.allUtangTransactions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedUtangForEdit by remember { mutableStateOf<Utang?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Unpaid, 1: Paid
    var searchQuery by remember { mutableStateOf("") } // SEARCH STATE

    // Sort
    var sortOption by remember { mutableStateOf(UtangSort.AmountHighLow) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // FILTER LOGIC: Search + Tab + Sort
    val filteredList = utangList
        .filter {
            val matchesTab = if (selectedTab == 0) !it.isPaid else it.isPaid
            val matchesSearch = it.customerName.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesSearch
        }
        .let { list ->
            when (sortOption) {
                UtangSort.AmountHighLow -> list.sortedByDescending { it.amount }
                UtangSort.AmountLowHigh -> list.sortedBy { it.amount }
                UtangSort.NameAsc -> list.sortedBy { it.customerName }
                UtangSort.NameDesc -> list.sortedByDescending { it.customerName }
                UtangSort.NewestFirst -> list.sortedByDescending { it.dateCreated }
                UtangSort.OldestFirst -> list.sortedBy { it.dateCreated }
            }
        }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Utang Ledger", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepOrange),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { sortMenuExpanded = true }) { Icon(Icons.Default.Sort, null, tint = Color.White) }
                            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                                DropdownMenuItem(text = { Text("Amount (High to Low)") }, onClick = { sortOption = UtangSort.AmountHighLow; sortMenuExpanded = false })
                                DropdownMenuItem(text = { Text("Amount (Low to High)") }, onClick = { sortOption = UtangSort.AmountLowHigh; sortMenuExpanded = false })
                                Divider()
                                DropdownMenuItem(text = { Text("Name (A-Z)") }, onClick = { sortOption = UtangSort.NameAsc; sortMenuExpanded = false })
                                DropdownMenuItem(text = { Text("Date (Newest)") }, onClick = { sortOption = UtangSort.NewestFirst; sortMenuExpanded = false })
                            }
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab, containerColor = DeepOrange, contentColor = Color.White) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Unpaid") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Paid") })
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = DeepOrange) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {

            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search Customer Name...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            LazyColumn {
                items(filteredList) { item ->
                    UtangItem(item) { selectedUtangForEdit = item }
                }
            }
        }
    }

    if (showAddDialog) {
        AddUtangDialog(onDismiss = { showAddDialog = false }) { u -> viewModel.addUtang(u); showAddDialog = false }
    }

    selectedUtangForEdit?.let { utang ->
        val utangHistory = transactions.filter { it.utangId == utang.id }
        ManageUtangDialog(
            utang = utang,
            history = utangHistory,
            onDismiss = { selectedUtangForEdit = null },
            onModify = { amount, isPayment ->
                viewModel.modifyUtangBalance(utang, amount, isPayment)
                selectedUtangForEdit = null
            }
        )
    }
}

@Composable
fun UtangItem(utang: Utang, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(utang.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (utang.notes.isNotEmpty()) Text(utang.notes, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("₱${utang.amount}", color = if (utang.isPaid) SuccessGreen else ErrorRed, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (utang.isPaid) Text("PAID", color = SuccessGreen, style = MaterialTheme.typography.labelSmall)
                else Text("Tap to Manage", color = Color.Blue, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ManageUtangDialog(
    utang: Utang,
    history: List<UtangTransaction>,
    onDismiss: () -> Unit,
    onModify: (Double, Boolean) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(utang.customerName, fontWeight = FontWeight.Bold)
                Text("Current Balance: ₱${utang.amount}", style = MaterialTheme.typography.bodyMedium, color = if(utang.amount > 0) ErrorRed else SuccessGreen)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Enter Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) onModify(amt, false) // Add Debt
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Text("Add Debt")
                    }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0) onModify(amt, true) // Pay
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text("Pay")
                    }
                }

                Divider(Modifier.padding(vertical = 16.dp))

                Text("Transaction History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyColumn(Modifier.height(150.dp)) {
                    items(history) { trans ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(dateFormatter.format(Date(trans.date)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(
                                "${if(trans.type == "PAYMENT") "Paid" else "Added"} ₱${trans.amount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if(trans.type == "PAYMENT") SuccessGreen else ErrorRed
                            )
                        }
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun AddUtangDialog(onDismiss: () -> Unit, onConfirm: (Utang) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Debt Record") },
        text = {
            Column {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name") })
                TextField(value = amount, onValueChange = { amount = it }, label = { Text("Initial Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                TextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val u = Utang(customerName = name, amount = amount.toDoubleOrNull() ?: 0.0, notes = notes)
                onConfirm(u)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}