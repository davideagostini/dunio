package com.davideagostini.summ.widget.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

// Discrete layout buckets used by both the real widget and the preview/lab tools.
// Android launchers do not behave like a fluid web layout, so we prefer a small
// number of stable variants instead of trying to adapt every pixel continuously.
enum class SpendingWidgetVariant {
    Small,
    Medium,
    Large,
}

object WidgetLayoutSpec {
    // Canonical spending widget sizes aligned to the standard launcher buckets used
    // by the project: 4x1, 4x2, and 4x3.
    val spendingSizes = setOf(
        DpSize(245.dp, 56.dp),
        DpSize(245.dp, 115.dp),
        DpSize(245.dp, 185.dp),
    )

    // Quick access is designed around the standard 2x2 widget bucket.
    val quickEntrySize = DpSize(109.dp, 115.dp)

    fun spendingVariant(width: Dp, height: Dp): SpendingWidgetVariant {
        return when {
            // 4x3 bucket: enough room for the secondary breakdown row.
            width >= 245.dp && height >= 185.dp -> SpendingWidgetVariant.Large
            // 4x2 bucket: enough room for the primary amount plus delta.
            width >= 245.dp && height >= 115.dp -> SpendingWidgetVariant.Medium
            // Anything smaller falls back to the compact 4x1-style presentation.
            else -> SpendingWidgetVariant.Small
        }
    }
}
