package com.davideagostini.summ.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Single coordination point for widget refreshes.
// Repositories and session flows call into this object instead of knowing
// which concrete widgets exist or how Glance update APIs are invoked.
object SummWidgetsUpdater {
    // Centralized refresh entry point so repositories/session logic do not need to know
    // which concrete widgets exist.
    suspend fun refreshAll(context: Context) {
        // Always jump to the application context so refreshes cannot accidentally hold
        // onto a short-lived Activity or Service reference.
        val appContext = context.applicationContext
        QuickAccessWidget().updateAll(appContext)
        SpendingSummaryWidget().updateAll(appContext)
    }

    // Convenience wrapper for call sites that are already on a suspend boundary later or
    // should not care about widget update threading.
    fun refreshAllAsync(context: Context) {
        // Use a tiny isolated scope: widget updates are fire-and-forget side effects and
        // should not cancel each other or be tied to a ViewModel lifecycle.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            refreshAll(context)
        }
    }
}
