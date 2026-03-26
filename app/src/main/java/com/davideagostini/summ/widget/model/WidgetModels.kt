package com.davideagostini.summ.widget.model

// Small sealed state used by the spending widget.
// We keep this separate from screen state because widgets have a much narrower
// rendering contract: either we can show numbers, or we need to explain why we cannot.
sealed interface SpendingSummaryWidgetState {
    // No Firebase user on device.
    data object SignedOut : SpendingSummaryWidgetState

    // User exists, but the app user document is not attached to a household yet.
    data object NeedsHousehold : SpendingSummaryWidgetState

    // Happy path for the widget. We pre-compute all three windows so the composable
    // stays completely stateless.
    data class Ready(
        val todayAmount: Double,
        val weekAmount: Double,
        val monthAmount: Double,
        val previousMonthAmount: Double,
        val currency: String,
    ) : SpendingSummaryWidgetState

    // Any Firestore/parsing failure falls back to a safe generic error state.
    data object Error : SpendingSummaryWidgetState
}
