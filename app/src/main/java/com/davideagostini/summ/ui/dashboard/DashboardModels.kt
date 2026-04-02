package com.davideagostini.summ.ui.dashboard

import androidx.compose.runtime.Immutable
import com.davideagostini.summ.data.entity.DashboardMonthlySummary
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
data class DashboardMetrics(
    val netWorth: Double,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val savingsRate: Double?,
    val monthlyCashFlow: Double,
    val financialRunway: Double?,
)

@Immutable
data class ChartPoint(
    val month: String,
    val label: String,
    val value: Double,
)

@Immutable
data class DashboardRenderState(
    val selectedMonth: String,
    val householdCurrency: String,
    val selectedRange: DashboardRange,
    val hasEntries: Boolean,
    val hasAssets: Boolean,
    val metrics: DashboardMetrics,
    val chartPoints: List<ChartPoint>,
    val monthlyChangePercent: Double?,
    val cashFlowChangePercent: Double?,
    val savingsRateDelta: Double?,
    val monthlyExpenses: Double,
    val monthlyExpensesChangePercent: Double?,
    val runwayChangePercent: Double?,
)

fun buildMonthlySeries(
    summariesByPeriod: Map<String, DashboardMonthlySummary>,
    selectedMonth: String,
    count: Int,
): List<DashboardMonthlySummary> =
    buildPreviousMonths(selectedMonth, count)
        .asReversed()
        .map { period -> summariesByPeriod[period] ?: DashboardMonthlySummary.empty(period) }

fun buildDashboardMetrics(
    selectedSummary: DashboardMonthlySummary,
    runwaySummaries: List<DashboardMonthlySummary>,
): DashboardMetrics {
    val averageMonthlyExpenses = runwaySummaries
        .map { it.monthlyExpenses }
        .average()
    val financialRunway = if (averageMonthlyExpenses > 0) {
        selectedSummary.liquidAssets / averageMonthlyExpenses
    } else {
        null
    }

    return DashboardMetrics(
        netWorth = selectedSummary.netWorth,
        totalAssets = selectedSummary.totalAssets,
        totalLiabilities = selectedSummary.totalLiabilities,
        savingsRate = selectedSummary.savingsRate,
        monthlyCashFlow = selectedSummary.cashFlow,
        financialRunway = financialRunway,
    )
}

fun calculateAverageSavingsRate(previousSummaries: List<DashboardMonthlySummary>): Double? {
    val validRates = previousSummaries.mapNotNull { summary ->
        if (summary.incomeTotal <= 0) {
            null
        } else {
            (summary.incomeTotal - summary.expenseTotal) / summary.incomeTotal
        }
    }

    return if (validRates.isEmpty()) null else validRates.average()
}

fun buildSeriesMonths(monthOptions: List<String>, selectedMonth: String, count: Int): List<String> {
    val index = monthOptions.indexOf(selectedMonth)
    val window = if (index == -1) monthOptions.take(count) else monthOptions.drop(index).take(count)
    return window.reversed()
}

fun buildPreviousMonths(selectedMonth: String, count: Int): List<String> {
    val base = YearMonth.parse(selectedMonth)
    return List(count) { index -> base.minusMonths(index.toLong()).toString() }
}

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
