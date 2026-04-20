package com.davideagostini.summ.wear

import android.net.Uri
import com.davideagostini.summ.data.category.stableUsageId
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.data.repository.CategoryRepository
import com.davideagostini.summ.data.repository.CategoryUsageRepository
import com.davideagostini.summ.data.repository.EntryRepository
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.data.session.SessionState
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
/**
 * Phone-side bridge for Wear quick entry.
 *
 * Immediate saves still use request/response for fast UI feedback on the watch. Offline saves use
 * a DataItem under `/wear/quick-entry/pending/...`, which the platform syncs automatically when
 * the phone becomes reachable again.
 */
class WearQuickEntryListenerService : WearableListenerService() {

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var categoryUsageRepository: CategoryUsageRepository
    @Inject lateinit var entryRepository: EntryRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onRequest(
        nodeId: String,
        path: String,
        request: ByteArray,
    ): Task<ByteArray>? {
        if (path != WearQuickEntryProtocol.PATH_CATEGORIES && path != WearQuickEntryProtocol.PATH_SAVE) {
            return null
        }

        val taskSource = TaskCompletionSource<ByteArray>()
        requestScope.launch {
            runCatching {
                when (path) {
                    WearQuickEntryProtocol.PATH_CATEGORIES -> handleCategoriesRequest(request)
                    WearQuickEntryProtocol.PATH_SAVE -> handleSaveRequest(request)
                    else -> error("Unsupported path: $path")
                }
            }.onSuccess { response ->
                taskSource.setResult(response.toString().toByteArray(Charsets.UTF_8))
            }.onFailure { throwable ->
                val payload = errorResponse(
                    throwable.message ?: "Wear quick entry failed.",
                ).toString().toByteArray(Charsets.UTF_8)
                taskSource.setResult(payload)
            }
        }
        return taskSource.task
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val pendingUris = mutableListOf<Uri>()
        try {
            dataEvents.forEach { event ->
                if (event.type != DataEvent.TYPE_CHANGED) return@forEach
                val path = event.dataItem.uri.path ?: return@forEach
                if (!path.startsWith(WearQuickEntryProtocol.PATH_PENDING_PREFIX)) return@forEach
                pendingUris += event.dataItem.uri
            }
        } finally {
            dataEvents.release()
        }

        pendingUris.forEach { uri ->
            requestScope.launch {
                runCatching { processPendingEntry(uri) }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun handleCategoriesRequest(request: ByteArray): JSONObject {
        val payload = JSONObject(request.decodeToString())
        val type = payload.optString("type").ifBlank { "expense" }
        val sessionState = sessionRepository.sessionState.first { state -> state !is SessionState.Loading }
        val readyState = sessionState as? SessionState.Ready
            ?: return errorResponse("Open Dunio on your phone and sign in first.")

        val allCategories = categoryRepository.allCategories
            .first()
            .filter { category -> category.type == type }
        val mostUsed = categoryUsageRepository.observeMostUsedCategories(
            householdId = readyState.household.id,
            type = type,
            categories = allCategories,
        ).first()
        val mostUsedIds = mostUsed.map { category -> category.stableUsageId() }.toSet()
        val orderedCategories = buildList {
            addAll(mostUsed)
            addAll(allCategories.filterNot { category -> category.stableUsageId() in mostUsedIds })
        }

        val categoriesJson = JSONArray()
        orderedCategories.forEach { category ->
            categoriesJson.put(
                JSONObject()
                    .put("name", category.name)
                    .put("emoji", category.emoji)
                    .put("systemKey", category.systemKey ?: JSONObject.NULL),
            )
        }

        return JSONObject()
            .put("ok", true)
            .put("currency", readyState.household.currency)
            .put("categories", categoriesJson)
    }

    private suspend fun handleSaveRequest(request: ByteArray): JSONObject {
        savePayload(JSONObject(request.decodeToString()))
        return JSONObject()
            .put("ok", true)
            .put("message", "Saved on phone")
    }

    private suspend fun processPendingEntry(uri: Uri) {
        val dataMap = readPendingDataMap(uri) ?: return
        val payload = JSONObject()
            .put("requestId", dataMap.getString("requestId"))
            .put("type", dataMap.getString("type"))
            .put("amount", dataMap.getDouble("amount"))
            .put("categoryName", dataMap.getString("categoryName"))
            .put("categoryEmoji", dataMap.getString("categoryEmoji"))
            .put("categoryKey", dataMap.getString("categoryKey"))

        savePayload(payload)
        Wearable.getDataClient(this).deleteDataItems(uri).await()
    }

    private suspend fun readPendingDataMap(uri: Uri) =
        Wearable.getDataClient(this).getDataItems(uri).await().let { buffer ->
            try {
                val item = buffer.firstOrNull()
                item?.let { DataMapItem.fromDataItem(it).dataMap }
            } finally {
                buffer.release()
            }
        }

    private suspend fun savePayload(payload: JSONObject) {
        val requestId = payload.optString("requestId").trim().ifBlank { "" }
        val type = payload.optString("type").ifBlank { "expense" }
        val amount = payload.optDouble("amount", Double.NaN)
        val categoryName = payload.optString("categoryName").trim()
        val categoryKey = payload.optString("categoryKey").trim().ifBlank { null }

        if (type != "expense" && type != "income") {
            throw IllegalArgumentException("Unsupported entry type.")
        }
        if (!amount.isFinite() || amount <= 0.0) {
            throw IllegalArgumentException("Enter a valid amount.")
        }
        if (categoryName.isBlank()) {
            throw IllegalArgumentException("Pick a category first.")
        }

        entryRepository.insert(
            Entry(
                id = requestId,
                type = type,
                description = categoryName,
                price = amount,
                category = categoryName,
                categoryKey = categoryKey,
                date = System.currentTimeMillis(),
            ),
        )

        serviceScope.launch {
            runCatching {
                val sessionState = withTimeoutOrNull(1_000L) {
                    sessionRepository.sessionState.first { state -> state !is SessionState.Loading }
                }
                val readyState = sessionState as? SessionState.Ready ?: return@runCatching
                categoryUsageRepository.markUsed(
                    householdId = readyState.household.id,
                    type = type,
                    category = Category(
                        name = categoryName,
                        emoji = payload.optString("categoryEmoji").ifBlank { "📦" },
                        type = type,
                        systemKey = categoryKey,
                    ),
                )
            }
        }
    }

    private fun errorResponse(message: String): JSONObject =
        JSONObject()
            .put("ok", false)
            .put("message", message)

    private companion object {
        val requestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
