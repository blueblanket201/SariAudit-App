package com.example.sariaudit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

// Vico Imports for 2.4.3
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

// FIXED: Explicitly importing the Compose extension functions for the axes
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart

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
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Revenue vs Profit by Hour", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (filteredSales.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("No data for this day", color = Color.Gray)
                            }
                        } else {
                            HourlyRevenueChart(dailySales = filteredSales)
                        }
                    }
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
    var startDateMillis by remember { mutableLongStateOf(System.currentTimeMillis() - 6 * 24 * 60 * 60 * 1000L) }
    var endDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showRangePicker by remember { mutableStateOf(false) }

    val weeklySales = sales.filter { it.timestamp in startDateMillis..endDateMillis }
    val totalRevenue = weeklySales.sumOf { it.totalAmount }
    val totalProfit = weeklySales.sumOf { it.profit }

    val topProducts = weeklySales.groupBy { it.productName }
        .mapValues { it.value.sumOf { sale -> sale.quantitySold } }
        .entries.sortedByDescending { it.value }.take(5)

    val rangeFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Column(Modifier.fillMaxSize().background(OffWhite)) {
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

            // === VICO CHART INTEGRATION ===
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Revenue by Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        WeeklyRevenueChart(
                            weeklySales = weeklySales,
                            startDateMillis = startDateMillis,
                            endDateMillis = endDateMillis
                        )
                    }
                }
            }
            // ====================================

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

    if (showRangePicker) {
        CustomDateRangePicker(
            startDateMillis = startDateMillis,
            endDateMillis = endDateMillis,
            onDateRangeSelected = { start, end ->
                startDateMillis = start
                endDateMillis = end
                showRangePicker = false
            },
            onDismiss = { showRangePicker = false }
        )
    }
}

@Composable
fun CustomDateRangePicker(
    startDateMillis: Long,
    endDateMillis: Long,
    onDateRangeSelected: (start: Long, end: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var tempStartDate by remember { mutableStateOf<Long?>(startDateMillis) }
    var tempEndDate by remember { mutableStateOf<Long?>(endDateMillis) }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date Range") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.Default.ChevronLeft, "Previous Month")
                    }
                    Text(
                        text = currentMonth.format(monthYearFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.Default.ChevronRight, "Next Month")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                        Text(
                            text = day,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val firstDayOfMonth = currentMonth.atDay(1)
                val lastDayOfMonth = currentMonth.atEndOfMonth()
                val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

                val days = buildList {
                    repeat(firstDayOfWeek) { add(null) }
                    for (day in 1..lastDayOfMonth.dayOfMonth) {
                        add(day)
                    }
                }

                Column {
                    days.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { day ->
                                if (day == null) {
                                    Box(modifier = Modifier.weight(1f).height(36.dp))
                                } else {
                                    val date = currentMonth.atDay(day)
                                    val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                    val isSelected = (tempStartDate == dateMillis) || (tempEndDate == dateMillis)
                                    val isInRange = tempStartDate != null && tempEndDate != null &&
                                            dateMillis > tempStartDate!! && dateMillis < tempEndDate!!
                                    val isStart = tempStartDate == dateMillis
                                    val isEnd = tempEndDate == dateMillis

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> DeepOrange
                                                    isInRange -> DeepOrange.copy(alpha = 0.3f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                when {
                                                    tempStartDate == null || (tempStartDate != null && tempEndDate != null) -> {
                                                        tempStartDate = dateMillis
                                                        tempEndDate = null
                                                    }
                                                    dateMillis < tempStartDate!! -> {
                                                        tempStartDate = dateMillis
                                                    }
                                                    else -> {
                                                        tempEndDate = dateMillis
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            color = when {
                                                isSelected -> Color.White
                                                isInRange -> DeepOrange
                                                else -> Color.Black
                                            },
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                            repeat(7 - week.size) {
                                Box(modifier = Modifier.weight(1f).height(36.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Start", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            tempStartDate?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
                            } ?: "Select date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (tempStartDate != null) DeepOrange else Color.Gray
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("End", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(
                            tempEndDate?.let {
                                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
                            } ?: "Select date",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (tempEndDate != null) DeepOrange else Color.Gray
                        )
                    }
                }
            }
        },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val start = tempStartDate
                            val end = tempEndDate
                            if (start != null && end != null) {
                                val actualStart = minOf(start, end)
                                val actualEnd = maxOf(start, end)
                                onDateRangeSelected(actualStart, actualEnd)
                            } else {
                                onDismiss()
                            }
                        },
                        enabled = tempStartDate != null && tempEndDate != null
                    ) {
                        Text("Apply")
                    }
                },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    formatter.format(Date(sale.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(sale.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("Qty: ${sale.quantitySold}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "+ ₱${sale.totalAmount}",
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "(₱${sale.profit.toInt()} profit)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// === NEW VICO CHART COMPOSABLE (v2.4.3) ===
@Composable
fun WeeklyRevenueChart(
    weeklySales: List<Sale>,
    startDateMillis: Long,
    endDateMillis: Long
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val dayCount = ((endDateMillis - startDateMillis) / (24 * 60 * 60 * 1000)).toInt() + 1

    LaunchedEffect(weeklySales, startDateMillis, endDateMillis) {
        val salesByDay = FloatArray(dayCount) { 0f }
        val startCalendar = Calendar.getInstance().apply { timeInMillis = startDateMillis }
        val startDayOfYear = startCalendar.get(Calendar.DAY_OF_YEAR)
        val startYear = startCalendar.get(Calendar.YEAR)

        weeklySales.forEach { sale ->
            val saleCalendar = Calendar.getInstance().apply { timeInMillis = sale.timestamp }
            val saleYear = saleCalendar.get(Calendar.YEAR)
            val saleDayOfYear = saleCalendar.get(Calendar.DAY_OF_YEAR)

            val dayIndex = if (saleYear == startYear) {
                saleDayOfYear - startDayOfYear
            } else {
                val daysInStartYear = startCalendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                (daysInStartYear - startDayOfYear) + saleDayOfYear
            }

            if (dayIndex in 0 until dayCount) {
                salesByDay[dayIndex] += sale.totalAmount.toFloat()
            }
        }

        modelProducer.runTransaction {
            columnSeries {
                series(salesByDay.toList())
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = 16.dp)
    )
}

@Composable
fun HourlyRevenueChart(dailySales: List<Sale>) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(dailySales) {
        val hours = (0..23).toList()
        val revenueData = hours.map { hour ->
            dailySales.filter { sale ->
                Calendar.getInstance().apply { timeInMillis = sale.timestamp }.get(Calendar.HOUR_OF_DAY) == hour
            }.sumOf { it.totalAmount }.toFloat()
        }
        val profitData = hours.map { hour ->
            dailySales.filter { sale ->
                Calendar.getInstance().apply { timeInMillis = sale.timestamp }.get(Calendar.HOUR_OF_DAY) == hour
            }.sumOf { it.profit }.toFloat()
        }

        modelProducer.runTransaction {
            lineSeries {
                series(revenueData)
                series(profitData)
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(),
        ),
        modelProducer = modelProducer,
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}