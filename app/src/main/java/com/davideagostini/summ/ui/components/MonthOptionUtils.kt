package com.davideagostini.summ.ui.components

import java.time.YearMonth

fun buildRecentMonthOptions(
    currentMonth: YearMonth = YearMonth.now(),
    count: Int = 12,
): List<String> = List(count) { index ->
    currentMonth.minusMonths(index.toLong()).toString()
}

fun preferredRecentMonth(
    monthOptions: List<String>,
    currentMonth: String = YearMonth.now().toString(),
): String = if (currentMonth in monthOptions) currentMonth else monthOptions.firstOrNull() ?: currentMonth
