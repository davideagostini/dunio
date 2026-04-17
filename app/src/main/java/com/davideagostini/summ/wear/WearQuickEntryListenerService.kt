package com.davideagostini.summ.wear
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
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
/**
 * Phone-side RPC bridge for Wear quick entry.
 *
 * The watch never touches Firebase directly in V1. It sends tiny RPC payloads to this listener,
 * and the listener delegates to the same repositories already used by the phone app.
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
        // Only handle the two quick-entry endpoints. Any other path should continue through the
        // normal Wear listener chain untouched.
        if (path != WearQuickEntryProtocol.PATH_CATEGORIES && path != WearQuickEntryProtocol.PATH_SAVE) {
            return null
        }

        val taskSource = TaskCompletionSource<ByteArray>()
        serviceScope.launch {
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

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun handleCategoriesRequest(request: ByteArray): JSONObject {
        // Categories stay phone-owned because the phone already knows the active household,
        // translations, and the local "most used" ranking.
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
        // V1 keeps the payload intentionally small: type + amount + category.
        val payload = JSONObject(request.decodeToString())
        val requestId = payload.optString("requestId").trim().ifBlank { "" }
        val type = payload.optString("type").ifBlank { "expense" }
        val amount = payload.optDouble("amount", Double.NaN)
        val categoryName = payload.optString("categoryName").trim()
        val categoryKey = payload.optString("categoryKey").trim().ifBlank { null }

        if (type != "expense" && type != "income") {
            return errorResponse("Unsupported entry type.")
        }
        if (!amount.isFinite() || amount <= 0.0) {
            return errorResponse("Enter a valid amount.")
        }
        if (categoryName.isBlank()) {
            return errorResponse("Pick a category first.")
        }

        entryRepository.insert(
            Entry(
                id = requestId,
                type = type,
                // The watch flow does not expose description editing yet, so we reuse the category
                // label as a minimal fallback description for the created entry.
                description = categoryName,
                price = amount,
                category = categoryName,
                categoryKey = categoryKey,
                date = System.currentTimeMillis(),
            ),
        )

        runCatching {
            // Ranking is a local convenience feature only; it must never make the actual save fail.
            val sessionState = sessionRepository.sessionState.first { state -> state !is SessionState.Loading }
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

        return JSONObject()
            .put("ok", true)
            .put("message", "Saved on phone")
    }

    private fun errorResponse(message: String): JSONObject =
        JSONObject()
            .put("ok", false)
            .put("message", message)
}
