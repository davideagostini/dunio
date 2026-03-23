package com.davideagostini.summ.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

// Manifest-facing receiver for the spending widget.
// The launcher talks to this receiver, which then delegates rendering
// to the actual Glance widget implementation below.
class SpendingSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpendingSummaryWidget()
}
