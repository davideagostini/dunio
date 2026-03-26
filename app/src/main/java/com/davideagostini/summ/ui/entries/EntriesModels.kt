package com.davideagostini.summ.ui.entries

import androidx.compose.runtime.Immutable
import com.davideagostini.summ.domain.model.EntryDisplayItem
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Immutable
data class EntryDayGroup(
    val key: LocalDate,
    val expenseTotal: Double,
    val entries: List<EntryDisplayItem>,
)

@Immutable
data class UnusualSpendingInsight(
    val category: String,
    val currentAmount: Double,
    val averageAmount: Double,
    val percentChange: Int,
)

@Immutable
data class EntriesRenderState(
    val selectedMonth: String,
    val isMonthClosed: Boolean,
    val monthEntries: List<EntryDisplayItem>,
    val visibleEntries: List<EntryDisplayItem>,
    val dayGroups: List<EntryDayGroup>,
    val unusualSpendingInsights: List<UnusualSpendingInsight>,
    val totalExpenses: Double,
    val totalIncome: Double,
    val monthLabel: String,
    val hasAnyEntries: Boolean,
)

internal fun matchesFilter(entry: EntryDisplayItem, filterType: EntriesFilterType): Boolean =
    when (filterType) {
        EntriesFilterType.All -> true
        EntriesFilterType.Expenses -> entry.type == "expense"
        EntriesFilterType.Income -> entry.type == "income"
    }

internal fun matchesSearch(entry: EntryDisplayItem, query: String): Boolean {
    if (query.isBlank()) return true
    val normalizedQuery = query.trim().lowercase(Locale.getDefault())
    return entry.description.lowercase(Locale.getDefault()).contains(normalizedQuery) ||
        entry.category.lowercase(Locale.getDefault()).contains(normalizedQuery)
}

internal fun buildDayGroups(entries: List<EntryDisplayItem>): List<EntryDayGroup> =
    entries
        .groupBy { toLocalDate(it.date) }
        .toSortedMap(compareByDescending { it })
        .map { (date, dayEntries) ->
            EntryDayGroup(
                key = date,
                expenseTotal = dayEntries.sumOf { entry -> if (entry.type == "expense") entry.price else 0.0 },
                entries = dayEntries.sortedByDescending { it.date },
            )
        }

internal fun buildUnusualSpendingInsights(
    entries: List<EntryDisplayItem>,
    selectedMonth: String,
): List<UnusualSpendingInsight> {
    val expenseEntries = entries.filter { it.type == "expense" }
    val previousMonths = previousMonthKeys(selectedMonth, 3)
    val previousMonthSet = previousMonths.toSet()

    val currentByCategory = expenseEntries
        .filter { monthKey(it.date) == selectedMonth }
        .groupBy { it.category }
        .mapValues { (_, categoryEntries) -> categoryEntries.sumOf { it.price } }

    val previousByCategoryMonth = expenseEntries
        .filter { monthKey(it.date) in previousMonthSet }
        .groupBy { it.category }
        .mapValues { (_, categoryEntries) ->
            categoryEntries.groupBy { monthKey(it.date) }
                .mapValues { (_, monthEntries) -> monthEntries.sumOf { it.price } }
        }

    return currentByCategory.entries
        .mapNotNull { (category, currentAmount) ->
            val previousValues = previousMonths.map { month ->
                previousByCategoryMonth[category]?.get(month) ?: 0.0
            }
            val activeHistory = previousValues.filter { it > 0.0 }
            if (activeHistory.size < 2) return@mapNotNull null

            val averageAmount = previousValues.sum() / previousMonths.size
            if (averageAmount < 20.0) return@mapNotNull null

            val delta = currentAmount - averageAmount
            val percentChange = (delta / averageAmount) * 100.0
            if (delta < 20.0 || percentChange < 25.0) return@mapNotNull null

            UnusualSpendingInsight(
                category = category,
                currentAmount = currentAmount,
                averageAmount = averageAmount,
                percentChange = percentChange.toInt().coerceAtLeast(0),
            )
        }
        .sortedByDescending { it.percentChange }
        .take(2)
}

internal fun previousMonthKeys(selectedMonth: String, count: Int): List<String> {
    val base = YearMonth.parse(selectedMonth)
    return List(count) { index ->
        base.minusMonths((index + 1).toLong()).toString()
    }
}

internal fun monthKey(epochMillis: Long): String =
    YearMonth.from(toLocalDate(epochMillis)).toString()

internal fun toLocalDate(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

internal fun formatMonthLabel(monthKey: String): String {
    val month = YearMonth.parse(monthKey)
    val locale = Locale.getDefault()
    return "${month.month.getDisplayName(TextStyle.SHORT, locale).replaceFirstChar { it.titlecase(locale) }} ${month.year}"
}

internal fun formatDayLabel(
    date: LocalDate,
    todayLabel: String,
    yesterdayLabel: String,
): String {
    val today = LocalDate.now()
    return when (date) {
        today -> todayLabel
        today.minusDays(1) -> yesterdayLabel
        else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    }
}
