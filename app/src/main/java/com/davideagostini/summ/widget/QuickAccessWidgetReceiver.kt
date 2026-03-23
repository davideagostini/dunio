package com.davideagostini.summ.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

// Android registers receivers, not GlanceAppWidget classes directly.
// This receiver is the manifest entry point that tells the system which
// widget implementation to inflate for the "Quick entry" widget.
class QuickAccessWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAccessWidget()
}
