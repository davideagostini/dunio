package com.davideagostini.summ.wearapp.data

import android.content.Context
import android.content.SharedPreferences
import com.davideagostini.summ.wearapp.model.PendingWearEntry
import com.davideagostini.summ.wearapp.model.WearCategoriesResponse
import com.davideagostini.summ.wearapp.model.WearCategory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.json.JSONArray
import org.json.JSONObject

/**
 * Small local store for the Wear quick-entry flow.
 *
 * It keeps:
 * - the latest category list returned by the phone, grouped by type
 * - a queue of pending entries that the watch should push to the phone later
 *
 * SharedPreferences is enough here because the payload is intentionally tiny and fully textual.
 */
internal class WearQuickEntryLocalStore(
    context: Context,
) {
    private val preferences = context.getSharedPreferences("wear_quick_entry_store", Context.MODE_PRIVATE)

    fun saveCategories(
        type: String,
        response: WearCategoriesResponse,
    ) {
        preferences.edit()
            .putString(categoriesKey(type), response.toJson().toString())
            .apply()
    }

    fun readCategories(type: String): WearCategoriesResponse? =
        preferences.getString(categoriesKey(type), null)
            ?.let { stored -> runCatching { JSONObject(stored).toCategoriesResponse() }.getOrNull() }

    fun enqueue(entry: PendingWearEntry) {
        val queue = readQueue().toMutableList()
        queue.add(entry)
        writeQueue(queue)
    }

    fun readQueue(): List<PendingWearEntry> =
        preferences.getString(PENDING_QUEUE_KEY, null)
            ?.let { stored ->
                runCatching {
                    JSONArray(stored).toPendingEntries()
                }.getOrDefault(emptyList())
            }
            ?: emptyList()

    fun replaceQueue(entries: List<PendingWearEntry>) {
        writeQueue(entries)
    }

    /**
     * Small convenience accessor used by the UI to surface how many entries are still waiting for
     * the phone to come back online.
     */
    fun pendingCount(): Int = readQueue().size

    /**
     * Watches the queue size so the UI stays aligned even when a background service flushes the
     * pending entries while the app is already open.
     */
    fun observePendingCount(): Flow<Int> = callbackFlow {
        trySend(pendingCount())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PENDING_QUEUE_KEY) {
                trySend(pendingCount())
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.distinctUntilChanged()

    private fun writeQueue(entries: List<PendingWearEntry>) {
        val array = JSONArray()
        entries.forEach { entry -> array.put(entry.toJson()) }
        preferences.edit()
            .putString(PENDING_QUEUE_KEY, array.toString())
            .commit()
    }

    private fun categoriesKey(type: String): String = "categories_$type"

    private companion object {
        const val PENDING_QUEUE_KEY = "pending_entries"
    }
}

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

private fun JSONObject.toCategoriesResponse(): WearCategoriesResponse =
    WearCategoriesResponse(
        currency = optString("currency", "EUR"),
        categories = optJSONArray("categories")?.toWearCategories().orEmpty(),
    )

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

private fun PendingWearEntry.toJson(): JSONObject =
    JSONObject()
        .put("requestId", requestId)
        .put("type", type)
        .put("amount", amount)
        .put("categoryName", categoryName)
        .put("categoryEmoji", categoryEmoji)
        .put("categoryKey", categoryKey ?: JSONObject.NULL)

private fun JSONArray.toPendingEntries(): List<PendingWearEntry> =
    buildList(length()) {
        for (index in 0 until length()) {
            val entry = optJSONObject(index) ?: continue
            add(
                PendingWearEntry(
                    requestId = entry.optString("requestId").ifBlank { java.util.UUID.randomUUID().toString() },
                    type = entry.optString("type", "expense"),
                    amount = entry.optDouble("amount", 0.0),
                    categoryName = entry.optString("categoryName"),
                    categoryEmoji = entry.optString("categoryEmoji", "📦"),
                    categoryKey = entry.optString("categoryKey").ifBlank { null },
                ),
            )
        }
    }
