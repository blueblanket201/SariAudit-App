package com.example.sariaudit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sariaudit.data.Sale
import com.example.sariaudit.ui.theme.DeepOrange
import com.example.sariaudit.ui.theme.TealGreen
import com.example.sariaudit.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavController, viewModel: MainViewModel) {
    val sales by viewModel.allSales.collectAsState()

    // --- WEEKLY CALCULATIONS ---
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -7)
    val weekAgoTime = calendar.timeInMillis

    val weeklySales = sales.filter { it.timestamp >= weekAgoTime }
    val totalWeeklyRevenue = weeklySales.sumOf { it.totalAmount }
    val totalWeeklyTransactions = weeklySales.size

    // Group sales by day
    val dateFormatter = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    val dailySales = weeklySales.groupBy { dateFormatter.format(Date(it.timestamp)) }
        .mapValues { entry -> entry.value.sumOf { it.totalAmount } }
        .entries.toList().takeLast(7) // Ensure we only show last 7 days

    // Top Products
    val topProducts = weeklySales.groupBy { it.productName }
        .mapValues { entry -> entry.value.sumOf { it.totalAmount } }
        .entries.sortedByDescending { it.value }
        .take(5)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sales Analytics", color = Color.White) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepOrange),
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } })
        }
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {

            // 1. WEEKLY SUMMARY CARD
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DeepOrange)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Weekly Performance (Last 7 Days)", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Revenue", color = Color.White.copy(alpha = 0.8f))
                            Text("₱%.2f".format(totalWeeklyRevenue), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Transactions", color = Color.White.copy(alpha = 0.8f))
                            Text("$totalWeeklyTransactions", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 2. DAY-TO-DAY BREAKDOWN
            Text("Day-to-Day Metrics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (dailySales.isEmpty()) {
                Text("No data for the last 7 days.", color = Color.Gray)
            } else {
                dailySales.forEach { (date, amount) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(date, style = MaterialTheme.typography.bodyLarge)
                        Text("₱%.2f".format(amount), color = TealGreen, fontWeight = FontWeight.Bold)
                    }
                    Divider()
                }
            }

            Spacer(Modifier.height(24.dp))

            // 3. BEST SELLING ITEMS
            Text("Best Selling Items (This Week)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (topProducts.isNotEmpty()) {
                val maxVal = topProducts.first().value
                Column {
                    topProducts.forEach { (name, total) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(name, modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            Box(Modifier.weight(1f).height(20.dp)) {
                                val fraction = (total / maxVal).toFloat()
                                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).background(DeepOrange, androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("₱${total.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Text("No sales data yet.", color = Color.Gray)
            }
        }
    }
}