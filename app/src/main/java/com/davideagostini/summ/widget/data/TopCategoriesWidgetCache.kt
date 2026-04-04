package com.davideagostini.summ.widget.data

import android.content.Context
import com.davideagostini.summ.widget.model.TopCategoriesWidgetState
import com.davideagostini.summ.widget.model.TopCategoryWidgetItem

/**
 * Small local cache for the top-categories widget.
 *
 * As with the spending widget, the launcher may show the static preview first if the process
 * needs to resolve Firebase data from scratch. Persisting the latest resolved widget state lets
 * us render real content immediately on the next bind.
 */
object TopCategoriesWidgetCache {
    private const val PREFS_NAME = "top_categories_widget_cache"
    private const val KEY_KIND = "kind"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_COUNT = "count"

    fun read(context: Context): TopCategoriesWidgetState? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (prefs.getString(KEY_KIND, null)) {
            "loading" -> TopCategoriesWidgetState.Loading
            "signed_out" -> TopCategoriesWidgetState.SignedOut
            "needs_household" -> TopCategoriesWidgetState.NeedsHousehold
            "error" -> TopCategoriesWidgetState.Error
            "ready" -> {
                val count = prefs.getInt(KEY_COUNT, 0)
                TopCategoriesWidgetState.Ready(
                    categories = List(count) { index ->
                        TopCategoryWidgetItem(
                            label = prefs.getString("label_$index", "").orEmpty(),
                            emoji = prefs.getString("emoji_$index", "📦").orEmpty().ifBlank { "📦" },
                            amount = Double.fromBits(prefs.getLong("amount_$index", 0L)),
                            shareOfMonth = Float.fromBits(prefs.getInt("share_$index", 0)),
                        )
                    },
                    currency = prefs.getString(KEY_CURRENCY, "EUR").orEmpty(),
                )
            }

            else -> null
        }
    }

    fun write(context: Context, state: TopCategoriesWidgetState) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            clear()
            when (state) {
                TopCategoriesWidgetState.Loading -> putString(KEY_KIND, "loading")
                TopCategoriesWidgetState.SignedOut -> putString(KEY_KIND, "signed_out")
                TopCategoriesWidgetState.NeedsHousehold -> putString(KEY_KIND, "needs_household")
                TopCategoriesWidgetState.Error -> putString(KEY_KIND, "error")
                is TopCategoriesWidgetState.Ready -> {
                    putString(KEY_KIND, "ready")
                    putString(KEY_CURRENCY, state.currency)
                    putInt(KEY_COUNT, state.categories.size)
                    state.categories.forEachIndexed { index, item ->
                        putString("label_$index", item.label)
                        putString("emoji_$index", item.emoji)
                        putLong("amount_$index", item.amount.toBits())
                        putInt("share_$index", item.shareOfMonth.toBits())
                    }
                }
            }
        }.apply()
    }
}
