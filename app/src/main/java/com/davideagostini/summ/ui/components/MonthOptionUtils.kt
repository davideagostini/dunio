package com.davideagostini.summ.ui.components

import java.time.YearMonth

fun buildRecentMonthOptions(
    currentMonth: YearMonth = YearMonth.now(),
    count: Int = 12,
): List<String> = List(count) { index ->
    currentMonth.minusMonths(index.toLong()).toString()
}

fun buildMonthOptions(
    startMonthInclusive: String?,
    endMonth: YearMonth = YearMonth.now(),
    fallbackCount: Int = 12,
): List<String> {
    val startMonth = runCatching { startMonthInclusive?.let(YearMonth::parse) }.getOrNull()
        ?: return buildRecentMonthOptions(currentMonth = endMonth, count = fallbackCount)

    if (endMonth.isBefore(startMonth)) {
        return listOf(endMonth.toString())
    }

    val options = mutableListOf<String>()
    var cursor = endMonth
    while (!cursor.isBefore(startMonth)) {
        options += cursor.toString()
        cursor = cursor.minusMonths(1)
    }
    return options
}

fun preferredRecentMonth(
    monthOptions: List<String>,
    currentMonth: String = YearMonth.now().toString(),
): String = if (currentMonth in monthOptions) currentMonth else monthOptions.firstOrNull() ?: currentMonth
