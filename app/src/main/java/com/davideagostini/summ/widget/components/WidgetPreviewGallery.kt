package com.davideagostini.summ.widget.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.theme.SummTheme
import com.davideagostini.summ.ui.theme.primaryLight
import com.davideagostini.summ.ui.theme.surfaceContainerLowLight
import com.davideagostini.summ.ui.theme.surfaceContainerLowestLight
import com.davideagostini.summ.widget.model.SpendingDeltaTone
import com.davideagostini.summ.widget.model.SpendingWidgetPresentation
import com.davideagostini.summ.widget.model.SpendingWidgetVariant
import com.davideagostini.summ.widget.model.WidgetLayoutSpec

// Android Studio preview gallery for fast visual iteration on widget layouts.
// This file does not render the launcher widget directly, but it reuses the same
// size rules and delta presentation logic so it stays close to runtime behavior.
@Composable
internal fun WidgetPreviewFrame(
    width: Dp,
    height: Dp,
    content: @Composable BoxScope.() -> Unit,
) {
    // Simulate the launcher's outer cell padding and rounded outer container.
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(26.dp))
            .background(surfaceContainerLowestLight)
            .padding(8.dp),
        content = content,
    )
}

@Composable
internal fun SpendingWidgetPreviewCard(variant: SpendingWidgetVariant) {
    val showDelta = variant != SpendingWidgetVariant.Small
    val showSecondary = variant == SpendingWidgetVariant.Large
    val amountFontSize = if (variant == SpendingWidgetVariant.Small) 24.sp else 26.sp
    val deltaText = SpendingWidgetPresentation.buildDeltaText(
        current = 428.90,
        previous = 465.00,
        suffix = stringResource(R.string.widget_spending_vs_previous_month),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceContainerLowLight)
            .padding(12.dp),
    ) {
        // Preview mirrors the branded header used by the real widget shell.
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.widget_spending_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.widget_spending_this_month),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "€428.90",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = amountFontSize,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )

        if (showDelta && deltaText != null) {
            // Use the same semantic color rules as the runtime widget.
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = deltaText.value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    color = when (deltaText.tone) {
                        SpendingDeltaTone.Positive -> primaryLight
                        SpendingDeltaTone.Negative -> MaterialTheme.colorScheme.error
                        SpendingDeltaTone.Neutral -> MaterialTheme.colorScheme.onSurface
                    },
                ),
            )
        }

        if (showSecondary) {
            // Secondary line is only shown in the large variant, same as runtime.
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.widget_spending_secondary,
                    "${stringResource(R.string.widget_spending_today)} €10.00",
                    "${stringResource(R.string.widget_spending_week)} €10.00",
                ),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

@Composable
internal fun QuickEntryWidgetPreviewCard() {
    // Preview version of the square quick-entry action surface.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(44.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.widget_quick_entry_label),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        )
    }
}

@Preview(name = "Widget Spending Small", widthDp = 180, heightDp = 96, showBackground = true)
@Composable
private fun SpendingWidgetSmallPreview() {
    SummTheme(dynamicColor = false) {
        // Pull size from WidgetLayoutSpec so preview and runtime stay in sync.
        WidgetPreviewFrame(
            width = WidgetLayoutSpec.spendingSizes.elementAt(0).width,
            height = WidgetLayoutSpec.spendingSizes.elementAt(0).height,
        ) {
            SpendingWidgetPreviewCard(
                variant = WidgetLayoutSpec.spendingVariant(
                    width = WidgetLayoutSpec.spendingSizes.elementAt(0).width,
                    height = WidgetLayoutSpec.spendingSizes.elementAt(0).height,
                ),
            )
        }
    }
}

@Preview(name = "Widget Spending Medium", widthDp = 240, heightDp = 112, showBackground = true)
@Composable
private fun SpendingWidgetMediumPreview() {
    SummTheme(dynamicColor = false) {
        WidgetPreviewFrame(
            width = WidgetLayoutSpec.spendingSizes.elementAt(1).width,
            height = WidgetLayoutSpec.spendingSizes.elementAt(1).height,
        ) {
            SpendingWidgetPreviewCard(
                variant = WidgetLayoutSpec.spendingVariant(
                    width = WidgetLayoutSpec.spendingSizes.elementAt(1).width,
                    height = WidgetLayoutSpec.spendingSizes.elementAt(1).height,
                ),
            )
        }
    }
}

@Preview(name = "Widget Spending Large", widthDp = 240, heightDp = 152, showBackground = true)
@Composable
private fun SpendingWidgetLargePreview() {
    SummTheme(dynamicColor = false) {
        WidgetPreviewFrame(
            width = WidgetLayoutSpec.spendingSizes.elementAt(2).width,
            height = WidgetLayoutSpec.spendingSizes.elementAt(2).height,
        ) {
            SpendingWidgetPreviewCard(
                variant = WidgetLayoutSpec.spendingVariant(
                    width = WidgetLayoutSpec.spendingSizes.elementAt(2).width,
                    height = WidgetLayoutSpec.spendingSizes.elementAt(2).height,
                ),
            )
        }
    }
}

@Preview(name = "Widget Quick Entry", widthDp = 96, heightDp = 96, showBackground = true)
@Composable
private fun QuickEntryWidgetPreview() {
    SummTheme(dynamicColor = false) {
        WidgetPreviewFrame(
            width = WidgetLayoutSpec.quickEntrySize.width,
            height = WidgetLayoutSpec.quickEntrySize.height,
        ) {
            QuickEntryWidgetPreviewCard()
        }
    }
}
