package com.davideagostini.summ.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.format.formatCurrency
import com.davideagostini.summ.ui.theme.onSurfaceDark
import com.davideagostini.summ.ui.theme.onSurfaceLight
import com.davideagostini.summ.ui.theme.primaryDark
import com.davideagostini.summ.ui.theme.primaryLight
import com.davideagostini.summ.widget.components.SummWidgetScaffold
import com.davideagostini.summ.widget.components.WidgetInfoState
import com.davideagostini.summ.widget.components.openAppAction
import com.davideagostini.summ.widget.components.quickEntryAction
import com.davideagostini.summ.widget.data.TopCategoriesWidgetDataSource
import com.davideagostini.summ.widget.model.TopCategoriesWidgetState
import com.davideagostini.summ.widget.model.TopCategoryWidgetItem

private val categoryTextColor = ColorProvider(onSurfaceLight, onSurfaceDark)
private val categoryMeterTrackColor = ColorProvider(Color(0xFFE5E9EF), Color(0xFF343B46))
private val categoryMeterLightColors = listOf(
    Color(0xFF0E8B80),
    Color(0xFF4F7CF7),
    Color(0xFFF2A93B),
)
private val categoryMeterDarkColors = listOf(
    Color(0xFF2EC4B6),
    Color(0xFF7FA2FF),
    Color(0xFFFFC867),
)

class TopCategoriesWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = TopCategoriesWidgetDataSource().loadCached(context)
            ?: TopCategoriesWidgetState.Loading

        provideContent {
            SummWidgetScaffold(
                title = context.getString(R.string.widget_top_categories_title),
                contentAction = openAppAction(context),
                trailingAction = actionRunCallback<RefreshSpendingWidgetAction>(),
            ) {
                when (state) {
                    TopCategoriesWidgetState.Loading -> {
                        WidgetInfoState(
                            message = context.getString(R.string.widget_loading),
                            action = quickEntryAction(context),
                        )
                    }
                    TopCategoriesWidgetState.SignedOut -> {
                        WidgetInfoState(
                            message = context.getString(R.string.widget_sign_in_required),
                            action = quickEntryAction(context),
                        )
                    }
                    TopCategoriesWidgetState.NeedsHousehold -> {
                        WidgetInfoState(
                            message = context.getString(R.string.widget_household_required),
                            action = quickEntryAction(context),
                        )
                    }
                    TopCategoriesWidgetState.Error -> {
                        WidgetInfoState(
                            message = context.getString(R.string.widget_loading_error),
                            action = quickEntryAction(context),
                        )
                    }
                    is TopCategoriesWidgetState.Ready -> {
                        if (state.categories.isEmpty()) {
                            WidgetInfoState(
                                message = context.getString(R.string.widget_top_categories_empty),
                                action = quickEntryAction(context),
                            )
                        } else {
                            Column {
                                state.categories.forEachIndexed { index, item ->
                                    if (index > 0) {
                                        Spacer(modifier = GlanceModifier.height(4.dp))
                                    }
                                    TopCategoryRow(
                                        index = index,
                                        item = item,
                                        currency = state.currency,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun TopCategoryRow(
    index: Int,
    item: TopCategoryWidgetItem,
    currency: String,
) {
    val context = LocalContext.current

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Spacer(modifier = GlanceModifier.height(8.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${item.emoji} ${item.label}",
                style = TextStyle(
                    color = categoryTextColor,
                    fontSize = 15.sp,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Spacer(modifier = GlanceModifier.width(10.dp))
            Text(
                text = formatCurrency(item.amount, currency),
                style = TextStyle(
                    color = categoryTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
        Spacer(modifier = GlanceModifier.height(6.dp))
        SquareMeterRow(
            context = context,
            fillRatio = item.shareOfMonth,
            colorIndex = index,
        )
    }
}

@androidx.compose.runtime.Composable
private fun SquareMeterRow(
    context: Context,
    fillRatio: Float,
    colorIndex: Int,
) {
    val isDark =
        context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
    val accentColor = if (isDark) {
        categoryMeterDarkColors[colorIndex % categoryMeterDarkColors.size]
    } else {
        categoryMeterLightColors[colorIndex % categoryMeterLightColors.size]
    }
    val trackColor = if (isDark) Color(0xFF343B46) else Color(0xFFE5E9EF)
    val trackWidth = (LocalSize.current.width - 40.dp).coerceAtLeast(120.dp)
    val bitmap = createMeterBitmap(
        context = context,
        widthDp = trackWidth,
        fillRatio = fillRatio,
        accentColor = accentColor,
    )

    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(8.dp)
            .cornerRadius(999.dp)
            .background(ColorProvider(trackColor, trackColor)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = null,
            modifier = GlanceModifier
                .width(trackWidth)
                .height(8.dp)
                .cornerRadius(999.dp),
        )
    }
}

private fun createMeterBitmap(
    context: Context,
    widthDp: androidx.compose.ui.unit.Dp,
    fillRatio: Float,
    accentColor: Color,
): Bitmap {
    val density = context.resources.displayMetrics.density
    val widthPx = (widthDp.value * density).toInt().coerceAtLeast(120)
    val heightPx = (8f * density).toInt().coerceAtLeast(8)
    val radius = heightPx / 2f
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accentColor.toArgb()
    }

    val clampedRatio = fillRatio.coerceIn(0f, 1f)
    val fillWidth = (widthPx * clampedRatio).coerceAtLeast(if (clampedRatio > 0f) radius else 0f)
    if (fillWidth > 0f) {
        val fillRect = RectF(0f, 0f, fillWidth, heightPx.toFloat())
        canvas.drawRoundRect(fillRect, radius, radius, fillPaint)
    }

    return bitmap
}
