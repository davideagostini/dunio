package com.davideagostini.summ.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.format.formatCurrency
import com.davideagostini.summ.ui.theme.errorDark
import com.davideagostini.summ.ui.theme.errorLight
import com.davideagostini.summ.ui.theme.onSurfaceDark
import com.davideagostini.summ.ui.theme.onSurfaceLight
import com.davideagostini.summ.ui.theme.primaryDark
import com.davideagostini.summ.ui.theme.primaryLight
import com.davideagostini.summ.widget.components.SummWidgetScaffold
import com.davideagostini.summ.widget.components.WidgetInfoState
import com.davideagostini.summ.widget.components.openAppAction
import com.davideagostini.summ.widget.components.quickEntryAction
import com.davideagostini.summ.widget.data.SpendingSummaryWidgetDataSource
import com.davideagostini.summ.widget.model.SpendingDeltaTone
import com.davideagostini.summ.widget.model.SpendingSummaryWidgetState
import com.davideagostini.summ.widget.model.SpendingWidgetPresentation

private val positiveDeltaColor = ColorProvider(primaryLight, primaryDark)
private val negativeDeltaColor = ColorProvider(errorLight, errorDark)
private val neutralTextColor = ColorProvider(onSurfaceLight, onSurfaceDark)

// Real launcher widget that surfaces a compact monthly spending snapshot.
// The widget is intentionally limited to a few dense metrics so it stays legible
// at common launcher sizes and refreshes quickly after entry updates.
class SpendingSummaryWidget : GlanceAppWidget() {
    // Samsung launchers are more reliable with a single stable layout than with a
    // responsive Glance widget that negotiates multiple size buckets.
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Render from local cache first so the launcher never gets stuck on the static preview
        // while Firebase work is still in flight.
        val state = SpendingSummaryWidgetDataSource().loadCached(context)
            ?: SpendingSummaryWidgetState.Loading

        provideContent {
            SummWidgetScaffold(
                title = context.getString(R.string.widget_spending_title),
                contentAction = openAppAction(context),
                trailingAction = actionRunCallback<RefreshSpendingWidgetAction>(),
            ) {
                when (state) {
                    SpendingSummaryWidgetState.Loading -> {
                        WidgetInfoState(
                            message = context.getString(R.string.widget_loading),
                            action = quickEntryAction(context),
                        )
                    }
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
                            "${context.getString(R.string.widget_spending_today)} ${formatCurrency(state.todayAmount, state.currency)}",
                            "${context.getString(R.string.widget_spending_week)} ${formatCurrency(state.weekAmount, state.currency)}",
                        )
                        Column(modifier = GlanceModifier.fillMaxSize()) {
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = formatCurrency(state.monthAmount, state.currency),
                                style = TextStyle(
                                    color = neutralTextColor,
                                    fontSize = 35.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            if (deltaText != null) {
                                // The delta only appears once there is enough room to keep
                                // the primary monthly amount visually dominant.
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    text = deltaText.value,
                                    style = TextStyle(
                                        color = when (deltaText.tone) {
                                            SpendingDeltaTone.Positive -> positiveDeltaColor
                                            SpendingDeltaTone.Negative -> negativeDeltaColor
                                            SpendingDeltaTone.Neutral -> neutralTextColor
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                            }
                            Spacer(modifier = GlanceModifier.height(12.dp))
                            Text(
                                text = secondaryLine,
                                style = TextStyle(
                                    color = neutralTextColor,
                                    fontSize = 13.sp,
                                ),
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                        }
                    }
                }
            }
        }
    }
}
