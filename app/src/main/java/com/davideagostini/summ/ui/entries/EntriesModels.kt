package com.davideagostini.summ.ui.entries

import androidx.compose.runtime.Immutable
import com.davideagostini.summ.domain.model.EntryDisplayItem
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Immutable
data class EntryDayGroup(
    val key: LocalDate,
    val expenseTotal: Double,
    val entries: List<EntryDisplayItem>,
)

@Immutable
sealed interface EntriesListItem {
    val key: String
}

@Immutable
data class EntriesDayHeaderItem(
    val date: LocalDate,
    val expenseTotal: Double,
) : EntriesListItem {
    override val key: String = "header:$date"
}

@Immutable
data class EntriesRowItem(
    val entry: EntryDisplayItem,
    val groupIndex: Int,
    val groupCount: Int,
) : EntriesListItem {
    override val key: String = "entry:${entry.id}"
}

@Immutable
data class UnusualSpendingInsight(
    val category: String,
    val currentAmount: Double,
    val averageAmount: Double,
    val percentChange: Int,
)

@Immutable
data class CategorySpendingBreakdownItem(
    val category: String,
    val emoji: String,
    val totalAmount: Double,
    val averageAmount: Double,
    val transactionCount: Int,
    val percentage: Double,
)

@Immutable
data class EntriesRenderState(
    val selectedMonth: String,
    val householdCurrency: String,
    val isMonthClosed: Boolean,
    val monthEntries: List<EntryDisplayItem>,
    val visibleEntries: List<EntryDisplayItem>,
    val listItems: List<EntriesListItem>,
    val dayGroups: List<EntryDayGroup>,
    val unusualSpendingInsights: List<UnusualSpendingInsight>,
    val categorySpendingBreakdown: List<CategorySpendingBreakdownItem>,
    val categorySpendingTotal: Double,
    val categorySpendingTransactionCount: Int,
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

internal fun buildEntriesListItems(dayGroups: List<EntryDayGroup>): List<EntriesListItem> =
    buildList {
        dayGroups.forEach { group ->
            add(
                EntriesDayHeaderItem(
                    date = group.key,
                    expenseTotal = group.expenseTotal,
                )
            )
            group.entries.forEachIndexed { index, entry ->
                add(
                    EntriesRowItem(
                        entry = entry,
                        groupIndex = index,
                        groupCount = group.entries.size,
                    )
                )
            }
        }
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

internal fun buildCategorySpendingBreakdown(
    entries: List<EntryDisplayItem>,
    uncategorizedLabel: String,
): List<CategorySpendingBreakdownItem> {
    val expenseEntries = entries.filter { it.type == "expense" }
    val totalExpenses = expenseEntries.sumOf { it.price }
    if (expenseEntries.isEmpty() || totalExpenses <= 0.0) return emptyList()

    return expenseEntries
        .groupBy { entry -> entry.category.ifBlank { uncategorizedLabel } }
        .map { (category, categoryEntries) ->
            val totalAmount = categoryEntries.sumOf { it.price }
            CategorySpendingBreakdownItem(
                category = category,
                emoji = categoryEntries.firstOrNull()?.emoji ?: "📦",
                totalAmount = totalAmount,
                averageAmount = totalAmount / categoryEntries.size,
                transactionCount = categoryEntries.size,
                percentage = totalAmount / totalExpenses,
            )
        }
        .sortedByDescending(CategorySpendingBreakdownItem::totalAmount)
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
    return month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
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
