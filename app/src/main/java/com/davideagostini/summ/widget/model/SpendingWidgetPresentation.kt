package com.davideagostini.summ.widget.model

import kotlin.math.abs

// Semantic tone for the monthly spending delta.
// We keep this separate from raw plus/minus direction because "expenses went down"
// is visually positive for this widget even though the arrow points downward.
enum class SpendingDeltaTone {
    Positive,
    Negative,
    Neutral,
}

// Tiny presentation model consumed by both the Glance widget and local previews.
// Sharing this avoids subtle wording/color drift between preview and runtime.
data class SpendingDeltaPresentation(
    val value: String,
    val tone: SpendingDeltaTone,
)

object SpendingWidgetPresentation {
    fun buildDeltaText(
        current: Double,
        previous: Double,
        suffix: String,
    ): SpendingDeltaPresentation? {
        // No previous month means no comparison. Returning null is better than inventing
        // a percentage that would look authoritative but actually be meaningless.
        if (previous <= 0.0) return null

        val change = ((current - previous) / previous) * 100
        if (abs(change) < 0.05) {
            // Treat near-zero drift as neutral so the widget does not flicker between
            // positive and negative on tiny rounding differences.
            return SpendingDeltaPresentation(
                value = "0% $suffix",
                tone = SpendingDeltaTone.Neutral,
            )
        }

        val rounded = abs(change).toInt()
        val isIncrease = change > 0
        val arrow = if (isIncrease) "↑" else "↓"
        return SpendingDeltaPresentation(
            value = "$arrow $rounded% $suffix",
            // Higher expenses are semantically worse, lower expenses are better.
            tone = if (isIncrease) SpendingDeltaTone.Negative else SpendingDeltaTone.Positive,
        )
    }
}
