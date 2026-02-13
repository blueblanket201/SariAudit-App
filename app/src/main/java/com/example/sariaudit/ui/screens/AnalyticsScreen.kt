package com.example.sariaudit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sariaudit.data.Sale
import com.example.sariaudit.ui.theme.DeepOrange
import com.example.sariaudit.ui.theme.OffWhite
import com.example.sariaudit.ui.theme.SuccessGreen
import com.example.sariaudit.ui.theme.TealGreen
import com.example.sariaudit.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(navController: NavController, viewModel: MainViewModel) {
    val sales by viewModel.allSales.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Daily, 1: Weekly

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Sales Analytics", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepOrange),
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                    }
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DeepOrange,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color.White
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Daily", color = Color.White) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Weekly", color = Color.White) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (selectedTab == 0) {
                DailyAnalyticsView(sales)
            } else {
                WeeklyAnalyticsView(sales)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyAnalyticsView(sales: List<Sale>) {
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val zoneId = ZoneId.systemDefault()
    val selectedLocalDate = Instant.ofEpochMilli(selectedDateMillis).atZone(zoneId).toLocalDate()

    val filteredSales = sales.filter { sale ->
        Instant.ofEpochMilli(sale.timestamp).atZone(zoneId).toLocalDate() == selectedLocalDate
    }

    val totalRevenue = filteredSales.sumOf { it.totalAmount }
    val totalProfit = filteredSales.sumOf { it.profit }
    val displayDateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Column(Modifier.fillMaxSize().background(OffWhite)) {
        // Date Selector Header
        Surface(color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(displayDateFormatter.format(Date(selectedDateMillis)), fontWeight = FontWeight.Bold, color = DeepOrange)
                IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.CalendarMonth, null, tint = DeepOrange) }
            }
        }

        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Revenue", "₱%.2f".format(totalRevenue), Icons.Default.AttachMoney, DeepOrange, Modifier.weight(1f))
                    MetricCard("Profit", "₱%.2f".format(totalProfit), Icons.Default.TrendingUp, TealGreen, Modifier.weight(1f))
                }
            }
            item { Text("Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (filteredSales.isEmpty()) {
                item { Text("No data for this day.", color = Color.Gray) }
            } else {
                items(filteredSales.sortedByDescending { it.timestamp }) { sale ->
                    TransactionRow(sale, timeFormatter)
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyAnalyticsView(sales: List<Sale>) {
    // 1. State for the selected range (Default: Last 7 days)
    var startDateMillis by remember { mutableStateOf(System.currentTimeMillis() - 6 * 24 * 60 * 60 * 1000L) }
    var endDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showRangePicker by remember { mutableStateOf(false) }

    // 2. Filter sales within the selected range
    val weeklySales = sales.filter { it.timestamp in startDateMillis..endDateMillis }
    val totalRevenue = weeklySales.sumOf { it.totalAmount }
    val totalProfit = weeklySales.sumOf { it.profit }

    val topProducts = weeklySales.groupBy { it.productName }
        .mapValues { it.value.sumOf { sale -> sale.quantitySold } }
        .entries.sortedByDescending { it.value }.take(5)

    val rangeFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Column(Modifier.fillMaxSize().background(OffWhite)) {
        // 3. Weekly Date Range Header
        Surface(color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Selected Audit Period", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text(
                        "${rangeFormatter.format(Date(startDateMillis))} - ${rangeFormatter.format(Date(endDateMillis))}",
                        fontWeight = FontWeight.Bold,
                        color = DeepOrange
                    )
                }
                IconButton(onClick = { showRangePicker = true }) {
                    Icon(Icons.Default.DateRange, null, tint = DeepOrange)
                }
            }
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DeepOrange)) {
                    Column(Modifier.padding(16.dp)) {
                        val diffDays = ((endDateMillis - startDateMillis) / (24 * 60 * 60 * 1000L)).toInt() + 1
                        Text("$diffDays-Day Performance", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Revenue", color = Color.White.copy(alpha = 0.7f))
                                Text("₱%.2f".format(totalRevenue), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Profit", color = Color.White.copy(alpha = 0.7f))
                                Text("₱%.2f".format(totalProfit), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item { Text("Top Selling Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

            if (topProducts.isEmpty()) {
                item { Text("No data available for this range.", color = Color.Gray) }
            } else {
                val maxQty = topProducts.first().value.toFloat()
                items(topProducts) { (name, qty) ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                            Text("$qty sold", fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = qty / maxQty,
                            modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 4.dp),
                            color = TealGreen,
                            trackColor = Color.LightGray
                        )
                    }
                }
            }
        }
    }

    // 4. Date Range Picker Dialog (Shows the 7-day span visually)
    if (showRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = startDateMillis,
            initialSelectedEndDateMillis = endDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showRangePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        startDateMillis = start
                        endDateMillis = end
                        showRangePicker = false
                    }
                }) { Text("Confirm Range") }
            },
            dismissButton = {
                TextButton(onClick = { showRangePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f).padding(16.dp),
                title = { Text("Select Audit Week", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false
            )
        }
    }
}

@Composable
fun TransactionRow(sale: Sale, formatter: SimpleDateFormat) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Icon
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    formatter.format(Date(sale.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(sale.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("Qty: ${sale.quantitySold}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "+ ₱${sale.totalAmount}",
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen,
                    style = MaterialTheme.typography.bodyMedium
                )
                // Optional: Show profit in small text
                Text(
                    "(₱${sale.profit.toInt()} profit)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
    }
}

