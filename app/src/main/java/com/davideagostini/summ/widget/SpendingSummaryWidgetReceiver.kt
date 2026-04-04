package com.davideagostini.summ.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

// Manifest-facing receiver for the spending widget.
// The launcher talks to this receiver, which then delegates rendering
// to the actual Glance widget implementation below.
class SpendingSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpendingSummaryWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SummWidgetsUpdater.refreshAllAsync(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        SummWidgetsUpdater.refreshAllAsync(context)
    }
}
