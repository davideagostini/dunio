package com.davideagostini.summ.ui.settings.monthclose

import androidx.compose.runtime.Immutable

@Immutable
data class MonthCloseUiState(
    val month: String = java.time.YearMonth.now().toString(),
    val status: String = "draft",
    val assetSnapshotCount: Int = 0,
    val transactionCount: Int = 0,
    val recurringMissingCount: Int = 0,
    val recurringMissingLabels: List<String> = emptyList(),
    val canClose: Boolean = false,
)
