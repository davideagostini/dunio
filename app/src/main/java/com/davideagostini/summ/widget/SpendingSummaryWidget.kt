package com.davideagostini.summ.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.format.formatEuro
import com.davideagostini.summ.ui.theme.errorDark
import com.davideagostini.summ.ui.theme.errorLight
import com.davideagostini.summ.ui.theme.onSurfaceDark
import com.davideagostini.summ.ui.theme.onSurfaceLight
import com.davideagostini.summ.ui.theme.primaryDark
import com.davideagostini.summ.ui.theme.primaryLight
import com.davideagostini.summ.widget.components.SummWidgetScaffold
import com.davideagostini.summ.widget.components.WidgetInfoState
import com.davideagostini.summ.widget.components.quickEntryAction
import com.davideagostini.summ.widget.data.SpendingSummaryWidgetDataSource
import com.davideagostini.summ.widget.model.SpendingDeltaTone
import com.davideagostini.summ.widget.model.SpendingSummaryWidgetState
import com.davideagostini.summ.widget.model.SpendingWidgetPresentation
import com.davideagostini.summ.widget.model.WidgetLayoutSpec

private val positiveDeltaColor = ColorProvider(primaryLight, primaryDark)
private val negativeDeltaColor = ColorProvider(errorLight, errorDark)
private val neutralTextColor = ColorProvider(onSurfaceLight, onSurfaceDark)

// Real launcher widget that surfaces a compact monthly spending snapshot.
// The widget is intentionally limited to a few dense metrics so it stays legible
// at common launcher sizes and refreshes quickly after entry updates.
class SpendingSummaryWidget : GlanceAppWidget() {
    // We provide a small set of stable widget sizes instead of a fully fluid layout.
    // This keeps the Glance implementation predictable across launchers while still
    // letting us tailor the content density.
    override val sizeMode = SizeMode.Responsive(WidgetLayoutSpec.spendingSizes)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load data before composition so the widget body stays a pure state -> UI mapping.
        val state = SpendingSummaryWidgetDataSource().load()

        provideContent {
            SummWidgetScaffold(
                title = context.getString(R.string.widget_spending_title),
                trailingAction = actionRunCallback<RefreshSpendingWidgetAction>(),
            ) {
                when (state) {
                    SpendingSummaryWidgetState.SignedOut -> {
                        WidgetInfoState(
                            message = context.getString(R.string.widget_sign_in_required),
                            action = quickEntryAction(context),
                        )
                    }

                    SpendingSummaryWidgetState.NeedsHousehold -> {
                        WidgetInfoState(
                            message = context.getString(R.string.widget_household_required),
                            action = quickEntryAction(context),
                        )
                    }

                    SpendingSummaryWidgetState.Error -> {
                        WidgetInfoState(
                            message = context.getString(R.string.widget_loading_error),
                            action = quickEntryAction(context),
                        )
                    }

                    is SpendingSummaryWidgetState.Ready -> {
                        // Prepare all presentation strings before building the layout.
                        // This keeps the Glance tree as close as possible to a pure
                        // "state in, UI out" renderer.
                        val deltaText = SpendingWidgetPresentation.buildDeltaText(
                            current = state.monthAmount,
                            previous = state.previousMonthAmount,
                            suffix = context.getString(R.string.widget_spending_vs_previous_month),
                        )
                        val secondaryLine = context.getString(
                            R.string.widget_spending_secondary,
                            "${context.getString(R.string.widget_spending_today)} ${formatEuro(state.todayAmount)}",
                            "${context.getString(R.string.widget_spending_week)} ${formatEuro(state.weekAmount)}",
                        )
                        val widgetSize = LocalSize.current
                        // The runtime variant is derived from the actual launcher-reported
                        // size so the widget can degrade gracefully when first inserted or
                        // when the user resizes it.
                        val variant = WidgetLayoutSpec.spendingVariant(
                            width = widgetSize.width,
                            height = widgetSize.height,
                        )
                        val isSmall = variant == com.davideagostini.summ.widget.model.SpendingWidgetVariant.Small
                        val isLarge = variant == com.davideagostini.summ.widget.model.SpendingWidgetVariant.Large

                        Column(modifier = GlanceModifier.fillMaxWidth()) {
                            Text(
                                text = context.getString(R.string.widget_spending_this_month),
                                style = TextStyle(
                                    color = neutralTextColor,
                                    fontSize = 12.sp,
                                ),
                            )
                            Spacer(modifier = GlanceModifier.height(6.dp))
                            Text(
                                text = formatEuro(state.monthAmount),
                                style = TextStyle(
                                    color = neutralTextColor,
                                    fontSize = if (isSmall) 24.sp else 26.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            if (!isSmall && deltaText != null) {
                                // The delta only appears once there is enough room to keep
                                // the primary monthly amount visually dominant.
                                Spacer(modifier = GlanceModifier.height(3.dp))
                                Text(
                                    text = deltaText.value,
                                    style = TextStyle(
                                        color = when (deltaText.tone) {
                                            SpendingDeltaTone.Positive -> positiveDeltaColor
                                            SpendingDeltaTone.Negative -> negativeDeltaColor
                                            SpendingDeltaTone.Neutral -> neutralTextColor
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                            }
                            if (isLarge) {
                                // Secondary breakdown is reserved for the largest size.
                                // This avoids the first-insert clipping issue seen on some
                                // launchers when they initially report an optimistic height.
                                Spacer(modifier = GlanceModifier.height(6.dp))
                                Text(
                                    text = secondaryLine,
                                    style = TextStyle(
                                        color = neutralTextColor,
                                        fontSize = 12.sp,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
