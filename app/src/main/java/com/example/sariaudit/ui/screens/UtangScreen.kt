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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtangScreen(navController: NavController, viewModel: MainViewModel) {
    val utangList by viewModel.allUtang.collectAsState()
    val transactions by viewModel.allUtangTransactions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedUtangForEdit by remember { mutableStateOf<Utang?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Logic: Unpaid (amount > 0) at the top, Paid (amount <= 0) at the bottom
    val unifiedList = utangList
        .filter { it.customerName.contains(searchQuery, ignoreCase = true) }
        .sortedWith(compareByDescending<Utang> { it.amount > 0.01 }.thenBy { it.customerName })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Utang Ledger", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepOrange),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = DeepOrange) {
                Icon(Icons.Default.PersonAdd, null)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search Customer...") },
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )

            Spacer(Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(unifiedList) { item ->
                    UtangItem(item) { selectedUtangForEdit = item }
                }
            }
        }
    }

    if (showAddDialog) {
        AddUtangDialog(onDismiss = { showAddDialog = false }) { u ->
            viewModel.addUtang(u)
            showAddDialog = false
        }
    }

    selectedUtangForEdit?.let { utang ->
        val utangHistory = transactions.filter { it.utangId == utang.id }
        ManageUtangDialog(
            utang = utang,
            history = utangHistory,
            onDismiss = { selectedUtangForEdit = null },
            onModify = { amount, isPayment, notes ->
                viewModel.modifyUtangBalance(utang, amount, isPayment, notes)
                selectedUtangForEdit = null
            }
        )
    }
}

@Composable
fun UtangItem(utang: Utang, onClick: () -> Unit) {
    // Check if balance is essentially zero (handling floating point issues)
    val isPaid = utang.amount <= 0.01

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        // Removed dynamic background; using standard surface color
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(utang.customerName, fontWeight = FontWeight.Bold)
                if (utang.notes.isNotEmpty()) {
                    Text(utang.notes, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                // Currency color changes based on amount
                Text(
                    "₱%.2f".format(utang.amount),
                    color = if (isPaid) SuccessGreen else ErrorRed,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isPaid) "PAID" else "UNPAID",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPaid) SuccessGreen else ErrorRed
                )
            }
        }
    }
}

@Composable
fun ManageUtangDialog(utang: Utang, history: List<UtangTransaction>, onDismiss: () -> Unit, onModify: (Double, Boolean, String) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isAmountValid = parsedAmount > 0

    // Safety check: Cannot pay more than what is owed
    val isOverpaying = parsedAmount > (utang.amount + 0.01)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(utang.customerName) },
        text = {
            Column {
                Text(
                    "Current Balance: ₱%.2f".format(utang.amount),
                    color = if(utang.amount > 0.01) ErrorRed else SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || (it.toDoubleOrNull() ?: -1.0) >= 0) amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isOverpaying,
                    supportingText = {
                        if (isOverpaying) Text("Cannot pay more than balance", color = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onModify(parsedAmount, false, notesText) },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        modifier = Modifier.weight(1f),
                        enabled = isAmountValid
                    ) { Text("Add Debt") }

                    Button(
                        onClick = { onModify(parsedAmount, true, notesText) },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier.weight(1f),
                        // Pay button disabled if overpaying
                        enabled = isAmountValid && !isOverpaying
                    ) { Text("Pay") }
                }

                Divider(Modifier.padding(vertical = 16.dp))
                Text("Recent Transactions", style = MaterialTheme.typography.labelMedium, color = Color.Gray)

                LazyColumn(Modifier.height(120.dp).padding(top = 8.dp)) {
                    items(history) { trans ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dateFormatter.format(Date(trans.date)), style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${if(trans.type == "PAYMENT") "Paid" else "Added"} ₱%.2f".format(trans.amount),
                                color = if(trans.type == "PAYMENT") SuccessGreen else ErrorRed,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
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

    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val isValid = name.isNotBlank() && parsedAmount >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Customer Entry") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || (it.toDoubleOrNull() ?: -1.0) >= 0) amount = it },
                    label = { Text("Initial Debt") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes (Optional)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(Utang(customerName = name, amount = parsedAmount, notes = notes)) }, enabled = isValid) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}