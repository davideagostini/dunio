package com.davideagostini.summ.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis.Companion.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis.Companion.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.davideagostini.summ.data.entity.Asset
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.ui.components.FullScreenLoading
import com.davideagostini.summ.ui.components.MonthPickerField
import com.davideagostini.summ.ui.components.buildRecentMonthOptions
import com.davideagostini.summ.ui.components.preferredRecentMonth
import com.davideagostini.summ.ui.format.formatEuro
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private data class DashboardMetrics(
    val netWorth: Double,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val savingsRate: Double?,
    val monthlyCashFlow: Double,
    val financialRunway: Double?,
)

private data class ChartPoint(
    val month: String,
    val label: String,
    val value: Double,
)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val assetHistory by viewModel.assetHistory.collectAsStateWithLifecycle()
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (isLoading) {
        FullScreenLoading()
        return
    }

    DashboardContent(
        assetHistory = assetHistory,
        entries = entries,
        categories = categories,
        uiState = uiState,
        onSelectMonth = viewModel::selectMonth,
        onSelectRange = viewModel::selectRange,
    )
}

@Composable
private fun DashboardContent(
    assetHistory: List<AssetHistoryEntry>,
    entries: List<Entry>,
    categories: List<Category>,
    uiState: DashboardUiState,
    onSelectMonth: (String) -> Unit,
    onSelectRange: (DashboardRange) -> Unit,
) {
    val currentMonth = YearMonth.now().toString()
    val monthOptions = remember {
        buildRecentMonthOptions()
    }
    val defaultMonth = remember(monthOptions, currentMonth) {
        preferredRecentMonth(monthOptions, currentMonth)
    }
    val selectedMonth = uiState.selectedMonth
        ?.takeIf { it in monthOptions }
        ?: defaultMonth

    LaunchedEffect(Unit) {
        if (defaultMonth.isNotBlank()) {
            onSelectMonth(defaultMonth)
        }
    }

    val assetsForMonth = remember(assetHistory, selectedMonth) {
        buildAssetsSnapshotForMonth(assetHistory, selectedMonth)
    }
    val monthEntries = remember(entries, selectedMonth) {
        entries.filter { entry -> monthKey(entry.date) == selectedMonth }
    }
    val metrics = remember(assetsForMonth, monthEntries, entries, selectedMonth) {
        calculateDashboardMetrics(assetsForMonth, monthEntries, entries, selectedMonth)
    }
    val averageSavingsRate = remember(entries, selectedMonth) {
        calculateAverageSavingsRate(entries, selectedMonth, 3)
    }
    val savingsRateDelta = if (metrics.savingsRate != null && averageSavingsRate != null) {
        metrics.savingsRate - averageSavingsRate
    } else {
        null
    }
    val rangeMonths = remember(monthOptions, selectedMonth, uiState.selectedRange) {
        buildSeriesMonths(monthOptions, selectedMonth, uiState.selectedRange.months)
    }
    val chartPoints = remember(rangeMonths, assetHistory) {
        rangeMonths.map { month ->
            ChartPoint(
                month = month,
                label = formatShortMonth(month),
                value = calculateNetWorthForMonth(assetHistory, month),
            )
        }
    }
    val previousMonth = remember(selectedMonth) {
        YearMonth.parse(selectedMonth).minusMonths(1).toString()
    }
    val currentValue = metrics.netWorth
    val previousValue = remember(assetHistory, previousMonth) {
        calculateNetWorthForMonth(assetHistory, previousMonth)
            .takeIf { hasActiveSnapshotForMonth(assetHistory, previousMonth) }
    }
    val monthlyChange = previousValue?.let { currentValue - it }
    val monthlyChangePercent = if (previousValue != null && previousValue != 0.0) {
        monthlyChange?.div(abs(previousValue))
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }

            item {
                DashboardToolbar(
                    monthOptions = monthOptions,
                    selectedMonth = selectedMonth,
                    onSelectMonth = onSelectMonth,
                )
            }

            item {
                NetWorthCard(
                    month = selectedMonth,
                    netWorth = metrics.netWorth,
                    monthlyChangePercent = monthlyChangePercent,
                    chartPoints = chartPoints,
                    selectedRange = uiState.selectedRange,
                    onSelectRange = onSelectRange,
                )
            }

            item {
                MetricCard(
                    label = "Assets",
                    value = formatEuro(metrics.totalAssets),
                    note = "Current value",
                )
            }

            item {
                MetricCard(
                    label = "Liabilities",
                    value = formatEuro(metrics.totalLiabilities),
                    note = "Outstanding",
                )
            }

            item {
                MetricCard(
                    label = "Cash flow",
                    value = formatEuro(metrics.monthlyCashFlow),
                    note = "Income - expenses",
                    valueColor = if (metrics.monthlyCashFlow >= 0) IncomeGreen else ExpenseRed,
                )
            }

            item {
                MetricCard(
                    label = "Savings rate",
                    value = metrics.savingsRate?.let { formatPercent(it) } ?: "N/A",
                    note = "Saved from income",
                    trendLabel = savingsRateDelta?.let {
                        "${if (it >= 0) "↑" else "↓"} ${formatPercent(abs(it))} vs 3M avg"
                    },
                    trendColor = when {
                        savingsRateDelta == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        savingsRateDelta > 0 -> IncomeGreen
                        savingsRateDelta < 0 -> ExpenseRed
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            item {
                MetricCard(
                    label = "Runway",
                    value = metrics.financialRunway?.let { formatRunwayMonths(it) } ?: "N/A",
                    trailingValue = metrics.financialRunway?.let { "/ ${formatRunwayYears(it)}" },
                )
            }

            item {
                MetricCard(
                    label = "Monthly expenses",
                    value = formatEuro(monthEntries.filter { it.type == "expense" }.sumOf { it.price }),
                )
            }
        }
    }
}

@Composable
private fun DashboardToolbar(
    monthOptions: List<String>,
    selectedMonth: String,
    onSelectMonth: (String) -> Unit,
) {
    MonthPickerField(
        label = formatMonthOption(selectedMonth),
        options = monthOptions,
        optionLabel = ::formatMonthOption,
        onSelect = onSelectMonth,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

@Composable
private fun NetWorthCard(
    month: String,
    netWorth: Double,
    monthlyChangePercent: Double?,
    chartPoints: List<ChartPoint>,
    selectedRange: DashboardRange,
    onSelectRange: (DashboardRange) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatMonthOption(month),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatEuro(netWorth),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            if (monthlyChangePercent != null) {
                Text(
                    text = "${if (monthlyChangePercent >= 0) "↑" else "↓"} ${formatPercent(abs(monthlyChangePercent))} vs previous month",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (monthlyChangePercent >= 0) IncomeGreen else ExpenseRed,
                )
            } else {
                Text(
                    text = "No previous month to compare",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(20.dp))
            NetWorthChart(
                points = chartPoints,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            )
            Spacer(Modifier.height(18.dp))
            DashboardRangeGroup(
                selectedRange = selectedRange,
                onSelectRange = onSelectRange,
            )
        }
    }
}

@Composable
private fun NetWorthChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No history yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        modelProducer.runTransaction {
            lineSeries {
                series(points.map { it.value })
            }
        }
    }

    val labels = remember(points) { points.map(ChartPoint::label) }
    val bottomAxisFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ ->
            labels.getOrElse(value.toInt().coerceIn(0, labels.lastIndex)) { labels.first() }
        }
    }
    val startAxisFormatter = remember {
        CartesianValueFormatter.decimal(
            decimalCount = 2,
            thousandsSeparator = ",",
            prefix = "€",
        )
    }
    val chartTheme = rememberM3VicoTheme(
        lineCartesianLayerColors = listOf(MaterialTheme.colorScheme.onSurface),
        lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val chartLineColor = MaterialTheme.colorScheme.onSurface
    val chartAreaTopColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val chartAreaBottomColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)

    ProvideVicoTheme(theme = chartTheme) {
        val areaFill = remember(chartAreaTopColor, chartAreaBottomColor) {
            LineCartesianLayer.AreaFill.single(
                Fill(
                    Brush.verticalGradient(
                        listOf(
                            chartAreaTopColor,
                            chartAreaBottomColor,
                        ),
                    ),
                ),
            )
        }
        val line = remember(chartLineColor, areaFill) {
            LineCartesianLayer.Line(
                fill = LineCartesianLayer.LineFill.single(Fill(chartLineColor)),
                areaFill = areaFill,
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 4.dp),
            )
        }
        val lineLayer = rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(line),
        )
        val startAxis = rememberStart(valueFormatter = startAxisFormatter)
        val bottomAxis = rememberBottom(valueFormatter = bottomAxisFormatter)
        val chart = rememberCartesianChart(
            lineLayer,
            startAxis = startAxis,
            bottomAxis = bottomAxis,
        )
        CartesianChartHost(
            modifier = modifier,
            chart = chart,
            modelProducer = modelProducer,
        )
    }
}

@Composable
private fun DashboardRangeGroup(
    selectedRange: DashboardRange,
    onSelectRange: (DashboardRange) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(999.dp),
            )
            .padding(4.dp),
    ) {
        listOf(
            DashboardRange.ThreeMonths to "3M",
            DashboardRange.SixMonths to "6M",
            DashboardRange.OneYear to "1Y",
        ).forEach { (range, label) ->
            val selected = range == selectedRange
            Button(
                onClick = { onSelectRange(range) },
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent,
                    contentColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 2.dp else 0.dp),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    note: String? = null,
    trailingValue: String? = null,
    trendLabel: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    trendColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                )
                if (!trailingValue.isNullOrBlank()) {
                    Text(
                        text = " $trailingValue",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!note.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!trendLabel.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = trendLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = trendColor,
                )
            }
        }
    }
}

private fun buildAssetsSnapshotForMonth(entries: List<AssetHistoryEntry>, month: String): List<Asset> =
    entries
        .filter { it.period == month }
        .sortedWith(compareByDescending<AssetHistoryEntry> { it.period }.thenByDescending { it.snapshotDate })
        .associateBy { it.name.trim().lowercase(Locale.getDefault()) }
        .values
        .filter { it.action != "deleted" }
        .map {
            Asset(
                id = it.assetId,
                householdId = it.householdId,
                name = it.name,
                type = it.type,
                category = it.category,
                value = it.value,
                currency = it.currency,
                liquid = it.liquid,
                period = it.period,
                snapshotDate = it.snapshotDate,
            )
        }
        .sortedByDescending { it.value }

private fun calculateDashboardMetrics(
    assets: List<Asset>,
    monthEntries: List<Entry>,
    allEntries: List<Entry>,
    selectedMonth: String,
): DashboardMetrics {
    val totalAssets = assets.filter { it.type == "asset" }.sumOf { it.value }
    val totalLiabilities = assets.filter { it.type == "liability" }.sumOf { it.value }
    val income = monthEntries.filter { it.type == "income" }.sumOf { it.price }
    val expenses = monthEntries.filter { it.type == "expense" }.sumOf { it.price }
    val runwayMonths = buildPreviousMonths(selectedMonth, 3)
    val averageMonthlyExpenses = runwayMonths
        .map { month ->
            allEntries.filter { it.type == "expense" && monthKey(it.date) == month }.sumOf { it.price }
        }
        .average()
    val liquidAssets = assets.filter { it.type == "asset" && it.liquid }.sumOf { it.value }

    return DashboardMetrics(
        netWorth = totalAssets - totalLiabilities,
        totalAssets = totalAssets,
        totalLiabilities = totalLiabilities,
        savingsRate = if (income > 0) (income - expenses) / income else null,
        monthlyCashFlow = income - expenses,
        financialRunway = if (averageMonthlyExpenses > 0) liquidAssets / averageMonthlyExpenses else null,
    )
}

private fun calculateAverageSavingsRate(
    allEntries: List<Entry>,
    selectedMonth: String,
    count: Int,
): Double? {
    val validRates = buildPreviousMonths(selectedMonth, count + 1)
        .drop(1)
        .mapNotNull { month ->
            val income = allEntries.filter { it.type == "income" && monthKey(it.date) == month }.sumOf { it.price }
            if (income <= 0) return@mapNotNull null
            val expenses = allEntries.filter { it.type == "expense" && monthKey(it.date) == month }.sumOf { it.price }
            (income - expenses) / income
        }

    return if (validRates.isEmpty()) null else validRates.average()
}

private fun calculateNetWorthForMonth(entries: List<AssetHistoryEntry>, month: String): Double {
    val assets = buildAssetsSnapshotForMonth(entries, month)
    val totals = assets.fold(0.0 to 0.0) { acc, asset ->
        if (asset.type == "asset") {
            acc.first + asset.value to acc.second
        } else {
            acc.first to acc.second + asset.value
        }
    }
    return totals.first - totals.second
}

private fun hasActiveSnapshotForMonth(entries: List<AssetHistoryEntry>, month: String): Boolean =
    entries.any { it.period == month && it.action != "deleted" }

private fun buildSeriesMonths(monthOptions: List<String>, selectedMonth: String, count: Int): List<String> {
    val index = monthOptions.indexOf(selectedMonth)
    val window = if (index == -1) monthOptions.take(count) else monthOptions.drop(index).take(count)
    return window.reversed()
}

private fun buildPreviousMonths(selectedMonth: String, count: Int): List<String> {
    val base = YearMonth.parse(selectedMonth)
    return List(count) { index -> base.minusMonths(index.toLong()).toString() }
}

private fun monthKey(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("yyyy-MM"))

private fun formatMonthOption(monthValue: String): String =
    YearMonth.parse(monthValue).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

private fun formatShortMonth(monthValue: String): String =
    YearMonth.parse(monthValue).format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))

private fun formatPercent(value: Double): String = "${(value * 100).roundToInt()}%"

private fun formatRunwayMonths(months: Double): String = "${"%.1f".format(months)} mo"

private fun formatRunwayYears(months: Double): String {
    val years = months / 12
    return if (years < 1) {
        "${"%.1f".format(years)} yr"
    } else {
        val rounded = if (years % 1.0 == 0.0) years.toInt().toString() else "%.1f".format(years)
        "$rounded yrs"
    }
}
