package com.davideagostini.summ.widget.data

import android.content.Context
import com.davideagostini.summ.widget.model.SpendingSummaryWidgetState

/**
 * Tiny local cache for the spending widget.
 *
 * Launcher widgets can outlive app processes and do not always get an immediate second refresh
 * after installation, so we keep the last resolved widget state locally and render from it first.
 */
object SpendingSummaryWidgetCache {
    private const val PREFS_NAME = "spending_widget_cache"
    private const val KEY_KIND = "kind"
    private const val KEY_TODAY = "today"
    private const val KEY_WEEK = "week"
    private const val KEY_MONTH = "month"
    private const val KEY_PREVIOUS_MONTH = "previous_month"
    private const val KEY_CURRENCY = "currency"

    fun read(context: Context): SpendingSummaryWidgetState? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (prefs.getString(KEY_KIND, null)) {
            "loading" -> SpendingSummaryWidgetState.Loading
            "signed_out" -> SpendingSummaryWidgetState.SignedOut
            "needs_household" -> SpendingSummaryWidgetState.NeedsHousehold
            "error" -> SpendingSummaryWidgetState.Error
            "ready" -> SpendingSummaryWidgetState.Ready(
                todayAmount = prefs.getFloat(KEY_TODAY, 0f).toDouble(),
                weekAmount = prefs.getFloat(KEY_WEEK, 0f).toDouble(),
                monthAmount = prefs.getFloat(KEY_MONTH, 0f).toDouble(),
                previousMonthAmount = prefs.getFloat(KEY_PREVIOUS_MONTH, 0f).toDouble(),
                currency = prefs.getString(KEY_CURRENCY, null) ?: return null,
            )
            else -> null
        }
    }

    fun write(context: Context, state: SpendingSummaryWidgetState) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            when (state) {
                SpendingSummaryWidgetState.Loading -> {
                    putString(KEY_KIND, "loading")
                    remove(KEY_TODAY)
                    remove(KEY_WEEK)
                    remove(KEY_MONTH)
                    remove(KEY_PREVIOUS_MONTH)
                    remove(KEY_CURRENCY)
                }
                SpendingSummaryWidgetState.SignedOut -> putString(KEY_KIND, "signed_out")
                SpendingSummaryWidgetState.NeedsHousehold -> putString(KEY_KIND, "needs_household")
                SpendingSummaryWidgetState.Error -> putString(KEY_KIND, "error")
                is SpendingSummaryWidgetState.Ready -> {
                    putString(KEY_KIND, "ready")
                    putFloat(KEY_TODAY, state.todayAmount.toFloat())
                    putFloat(KEY_WEEK, state.weekAmount.toFloat())
                    putFloat(KEY_MONTH, state.monthAmount.toFloat())
                    putFloat(KEY_PREVIOUS_MONTH, state.previousMonthAmount.toFloat())
                    putString(KEY_CURRENCY, state.currency)
                }
            }
        }.apply()
    }
}
