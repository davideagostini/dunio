package com.davideagostini.summ.ui.dashboard

import com.davideagostini.summ.data.entity.Asset
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.data.entity.Entry
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class DashboardMetrics(
    val netWorth: Double,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val savingsRate: Double?,
    val monthlyCashFlow: Double,
    val financialRunway: Double?,
)

data class ChartPoint(
    val month: String,
    val label: String,
    val value: Double,
)

fun buildAssetsSnapshotForMonth(entries: List<AssetHistoryEntry>, month: String): List<Asset> =
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

fun calculateDashboardMetrics(
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

fun calculateAverageSavingsRate(
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

fun calculateNetWorthForMonth(entries: List<AssetHistoryEntry>, month: String): Double {
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

fun hasActiveSnapshotForMonth(entries: List<AssetHistoryEntry>, month: String): Boolean =
    entries.any { it.period == month && it.action != "deleted" }

fun buildSeriesMonths(monthOptions: List<String>, selectedMonth: String, count: Int): List<String> {
    val index = monthOptions.indexOf(selectedMonth)
    val window = if (index == -1) monthOptions.take(count) else monthOptions.drop(index).take(count)
    return window.reversed()
}

fun buildPreviousMonths(selectedMonth: String, count: Int): List<String> {
    val base = YearMonth.parse(selectedMonth)
    return List(count) { index -> base.minusMonths(index.toLong()).toString() }
}

fun monthKey(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("yyyy-MM"))

fun formatMonthOption(monthValue: String): String =
    YearMonth.parse(monthValue).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

fun formatShortMonth(monthValue: String): String =
    YearMonth.parse(monthValue).format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))

fun calculateRelativeChange(currentValue: Double, previousValue: Double?): Double? =
    if (previousValue == null || previousValue == 0.0) {
        null
    } else {
        (currentValue - previousValue) / abs(previousValue)
    }

fun formatPercent(value: Double): String = "${(value * 100).roundToInt()}%"

fun formatRunwayMonths(months: Double): String = "${"%.1f".format(months)} mo"

fun formatRunwayYears(months: Double): String {
    val years = months / 12
    return if (years < 1) {
        "${"%.1f".format(years)} yr"
    } else {
        val rounded = if (years % 1.0 == 0.0) years.toInt().toString() else "%.1f".format(years)
        "$rounded yrs"
    }
}
