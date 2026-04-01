package com.davideagostini.summ.ui.dashboard

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DashboardGetStartedPrefs(
    val dismissed: Boolean = false,
)

object DashboardGetStartedManager {
    private const val PREFS_NAME = "summ_dashboard_get_started"
    private const val PREF_DISMISSED = "dismissed"

    private val _prefs = MutableStateFlow(DashboardGetStartedPrefs())
    val prefs: StateFlow<DashboardGetStartedPrefs> = _prefs.asStateFlow()

    fun init(context: Context) {
        _prefs.value = readPrefs(context)
    }

    fun dismiss(context: Context) {
        writePrefs(context, readPrefs(context).copy(dismissed = true))
    }

    private fun readPrefs(context: Context): DashboardGetStartedPrefs {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return DashboardGetStartedPrefs(
            dismissed = prefs.getBoolean(PREF_DISMISSED, false),
        )
    }

    private fun writePrefs(context: Context, state: DashboardGetStartedPrefs) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_DISMISSED, state.dismissed)
            .apply()
        _prefs.value = state
    }
}
