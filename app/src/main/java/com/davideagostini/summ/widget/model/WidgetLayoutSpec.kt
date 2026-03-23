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
    // Canonical spending widget sizes. These values drive Glance's responsive mode
    // and are also reused by the preview gallery and Widget Lab screen.
    val spendingSizes = setOf(
        DpSize(180.dp, 96.dp),
        DpSize(240.dp, 112.dp),
        DpSize(240.dp, 152.dp),
    )

    // Canonical square size for the quick-entry widget.
    val quickEntrySize = DpSize(96.dp, 96.dp)

    fun spendingVariant(width: Dp, height: Dp): SpendingWidgetVariant {
        // The variant thresholds are intentionally conservative. In practice many launchers
        // report slightly awkward sizes on first insertion, so we only promote to the large
        // layout when there is clearly enough room for the secondary line.
        val isSmall = height < 108.dp || width < 200.dp
        val isLarge = height >= 150.dp && width >= 220.dp
        return when {
            isSmall -> SpendingWidgetVariant.Small
            isLarge -> SpendingWidgetVariant.Large
            else -> SpendingWidgetVariant.Medium
        }
    }
}
