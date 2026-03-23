package com.davideagostini.summ.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/**
 * Manual refresh entry point exposed by the spending widget.
 *
 * Widgets do not keep a live Firestore listener, so a manual refresh remains the fastest way
 * to re-query shared household data on demand without reopening the app.
 */
class RefreshSpendingWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        SummWidgetsUpdater.refreshAll(context)
    }
}
