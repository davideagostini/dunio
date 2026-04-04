package com.davideagostini.summ.widget.components

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.davideagostini.summ.MainActivity
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.entry.QuickEntryActivity
import com.davideagostini.summ.ui.theme.onSurfaceDark
import com.davideagostini.summ.ui.theme.onSurfaceLight
import com.davideagostini.summ.ui.theme.primaryDark
import com.davideagostini.summ.ui.theme.primaryLight
import com.davideagostini.summ.ui.theme.surfaceContainerDark
import com.davideagostini.summ.ui.theme.surfaceContainerLowDark
import com.davideagostini.summ.ui.theme.surfaceContainerLowestDark

private val widgetBackground = ColorProvider(Color(0xFFF7F7F7), surfaceContainerDark)
private val widgetSurface = ColorProvider(Color.White, surfaceContainerLowDark)
private val widgetPrimary = ColorProvider(primaryLight, primaryDark)
private val widgetOnSurface = ColorProvider(onSurfaceLight, onSurfaceDark)

// Reuse the existing quick-entry activity so the widget does not need a second
// entry flow or special navigation path.
fun quickEntryAction(context: Context): Action =
    actionStartActivity(Intent(context, QuickEntryActivity::class.java))

fun openAppAction(context: Context): Action =
    actionStartActivity(Intent(context, MainActivity::class.java))

@androidx.compose.runtime.Composable
fun SummWidgetScaffold(
    title: String,
    body: String? = null,
    contentAction: Action? = null,
    trailingAction: Action? = null,
    content: @androidx.compose.runtime.Composable () -> Unit,
) {
    // Shared shell used by both widgets so they stay visually aligned with the app:
    // outer soft container, inner rounded card, small branded header, then body.
    // Keeping this shell centralized means spacing/radius tweaks hit every widget at once.
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(widgetSurface)
                .then(
                    if (contentAction != null) {
                        GlanceModifier.clickable(contentAction)
                    } else {
                        GlanceModifier
                    },
                )
                .padding(12.dp),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Small brand mark in the header so the widget still reads as Summ
                    // even when the launcher crops or scales the preview aggressively.
                    Text(
                        text = title,
                        style = TextStyle(
                            color = widgetOnSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
                if (trailingAction != null) {
                    // Optional trailing action stays compact in the header so it does not steal
                    // too much room from small widgets.
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    WidgetIconAction(
                        symbol = "\u21BB",
                        action = trailingAction,
                    )
                }
            }

            if (body != null) {
                // Secondary intro text is optional and only used by more explanatory states
                // such as empty/error/widget guidance blocks.
                Spacer(modifier = GlanceModifier.height(6.dp))
                Text(
                    text = body,
                    style = TextStyle(
                        color = widgetOnSurface,
                        fontSize = 12.sp,
                    ),
                    maxLines = 3,
                )
            }

            Spacer(modifier = GlanceModifier.height(10.dp))
            content()
        }
    }
}

@androidx.compose.runtime.Composable
fun WidgetActionChip(action: Action) {
    val context = LocalContext.current

    // Tiny CTA used in the header. The label stays generic because both widgets
    // ultimately route to the same quick-entry destination.
    Box(
        modifier = GlanceModifier
            .cornerRadius(999.dp)
            .background(widgetPrimary)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = context.getString(R.string.widget_quick_access_action),
            style = TextStyle(
                color = ColorProvider(
                    androidx.compose.ui.graphics.Color.White,
                    androidx.compose.ui.graphics.Color.White,
                ),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@androidx.compose.runtime.Composable
fun WidgetIconAction(
    symbol: String,
    action: Action,
) {
    // Compact trailing control used for manual widget refreshes and similar utility actions.
    Box(
        modifier = GlanceModifier
            .width(28.dp)
            .height(28.dp)
            .cornerRadius(999.dp)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = TextStyle(
                color = widgetOnSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@androidx.compose.runtime.Composable
fun WidgetMetricCell(label: String, value: String) {
    // Single metric row used by the spending widget.
    // Label and amount stay on the same line to keep the widget compact enough
    // for common launcher sizes.
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(18.dp)
            .background(widgetBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = widgetOnSurface,
                fontSize = 12.sp,
            ),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = value,
            style = TextStyle(
                color = widgetOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@androidx.compose.runtime.Composable
fun WidgetInfoState(message: String, action: Action? = null) {
    // Shared fallback UI for signed-out / no-household / loading-error states.
    // The widget remains actionable instead of turning into a dead card.
    Column {
        Text(
            text = message,
            style = TextStyle(
                color = widgetOnSurface,
                fontSize = 13.sp,
            ),
        )
        if (action != null) {
            // Keep the CTA generic and low-friction: when widgets cannot load household
            // data, the fastest recovery path is simply reopening the app.
            Spacer(modifier = GlanceModifier.height(12.dp))
            Box(
                modifier = GlanceModifier
                    .cornerRadius(18.dp)
                    .background(widgetBackground)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .clickable(action),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = LocalContext.current.getString(R.string.widget_open_app),
                    style = TextStyle(
                        color = widgetOnSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}
