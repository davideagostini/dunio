package com.davideagostini.summ.ui.dashboard

import androidx.compose.runtime.Immutable

enum class DashboardRange(val months: Int) {
    ThreeMonths(3),
    SixMonths(6),
    OneYear(12),
}

@Immutable
data class DashboardUiState(
    val selectedMonth: String? = null,
    val selectedRange: DashboardRange = DashboardRange.SixMonths,
    val showGetStarted: Boolean = false,
)
