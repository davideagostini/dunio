/**
 * Local persistence layer for the Wear OS quick-entry flow.
 *
 * This store uses SharedPreferences to cache the most recent category list
 * received from the phone, keyed by transaction type ("expense" / "income").
 * The cached data allows the watch to show categories immediately on launch
 * without waiting for a network round-trip.
 *
 * Pending entries are **not** stored here; they live in the Wear Data Layer
 * so the platform can automatically synchronise them when the phone reconnects.
 *
 * The file also contains private extension functions for JSON serialization
 * and deserialization of [WearCategoriesResponse] and [WearCategory].
 */
package com.davideagostini.summ.wearapp.data

import android.content.Context
import com.davideagostini.summ.wearapp.model.WearCategoriesResponse
import com.davideagostini.summ.wearapp.model.WearCategory
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit

/**
 * Lightweight cache backed by SharedPreferences for Wear quick-entry data.
 *
 * SharedPreferences was chosen over a database because the data is tiny
 * (a few dozen categories at most) and entirely textual. The store is
 * scoped to the "wear_quick_entry_store" preferences file.
 *
 * @param context Application context used to obtain the SharedPreferences instance.
 */
internal class WearQuickEntryLocalStore(
    context: Context,
) {
    private val preferences = context.getSharedPreferences("wear_quick_entry_store", Context.MODE_PRIVATE)

    /**
     * Persists a category response for the given transaction type.
     *
     * The response is serialized to a JSON string and stored under a
     * type-specific key (e.g. "categories_expense"). Any previously
     * cached response for the same type is overwritten.
     *
     * @param type     Transaction type ("expense" or "income").
     * @param response The full [WearCategoriesResponse] to cache.
     */
    fun saveCategories(
        type: String,
        response: WearCategoriesResponse,
    ) {
        preferences.edit {
            putString(categoriesKey(type), response.toJson().toString())
        }
    }

    /**
     * Reads the cached category response for the given transaction type.
     *
     * Returns `null` if no cache exists or if the stored JSON is corrupt
     * (deserialization is wrapped in `runCatching` to handle parse errors
     * gracefully).
     *
     * @param type Transaction type ("expense" or "income").
     * @return The cached [WearCategoriesResponse], or null.
     */
    fun readCategories(type: String): WearCategoriesResponse? =
        preferences.getString(categoriesKey(type), null)
            ?.let { stored -> runCatching { JSONObject(stored).toCategoriesResponse() }.getOrNull() }

    /**
     * Builds the SharedPreferences key for the given transaction type.
     *
     * @param type Transaction type.
     * @return Key string, e.g. "categories_expense".
     */
    private fun categoriesKey(type: String): String = "categories_$type"
}

/**
 * Serializes a [WearCategoriesResponse] into a [JSONObject].
 *
 * The resulting JSON has the structure:
 * ```
 * {
 *   "currency": "EUR",
 *   "categories": [
 *     {"name": "Groceries", "emoji": "🛒", "systemKey": "groceries"},
 *     ...
 *   ]
 * }
 * ```
 *
 * @receiver The response to serialize.
 * @return A JSON representation suitable for writing to SharedPreferences.
 */
private fun WearCategoriesResponse.toJson(): JSONObject {
    val categoriesArray = JSONArray()
    categories.forEach { category ->
        categoriesArray.put(
            JSONObject()
                .put("name", category.name)
                .put("emoji", category.emoji)
                .put("systemKey", category.systemKey ?: JSONObject.NULL),
        )
    }

    return JSONObject()
        .put("currency", currency)
        .put("categories", categoriesArray)
}

/**
 * Deserializes a [JSONObject] into a [WearCategoriesResponse].
 *
 * Missing fields are given sensible defaults: "EUR" for currency and an
 * empty list for categories.
 *
 * @receiver The JSON object to parse.
 * @return The reconstructed [WearCategoriesResponse].
 */
private fun JSONObject.toCategoriesResponse(): WearCategoriesResponse =
    WearCategoriesResponse(
        currency = optString("currency", "EUR"),
        categories = optJSONArray("categories")?.toWearCategories().orEmpty(),
    )

/**
 * Converts a [JSONArray] of category objects into a list of [WearCategory].
 *
 * Each element in the array is expected to be a JSONObject with "name",
 * "emoji", and optionally "systemKey" fields. Null or malformed entries
 * are silently skipped.
 *
 * @receiver The JSON array to convert.
 * @return A list of parsed [WearCategory] instances.
 */
internal fun JSONArray.toWearCategories(): List<WearCategory> =
    buildList(length()) {
        for (index in 0 until length()) {
            val category = optJSONObject(index) ?: continue
            add(
                WearCategory(
                    name = category.optString("name"),
                    emoji = category.optString("emoji", "📦"),
                    systemKey = category.optString("systemKey").ifBlank { null },
                ),
            )
        }
    }
