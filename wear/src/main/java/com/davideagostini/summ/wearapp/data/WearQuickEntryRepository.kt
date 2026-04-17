package com.davideagostini.summ.wearapp.data

import android.content.Context
import com.davideagostini.summ.wearapp.R
import com.davideagostini.summ.wearapp.model.PendingWearEntry
import com.davideagostini.summ.wearapp.model.WearCategoriesResponse
import com.davideagostini.summ.wearapp.model.WearCategory
import com.davideagostini.summ.wearapp.model.WearSaveResult
import com.davideagostini.summ.wearapp.protocol.WearQuickEntryProtocol
import com.davideagostini.summ.wearapp.sync.WearQuickEntryQueueSignal
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

/**
 * Watch-side repository for quick entry.
 *
 * The watch never talks to Firebase directly in V1. It either:
 * - loads categories from the phone and caches them locally
 * - saves immediately on the phone
 * - or queues the payload locally for later automatic sync
 */
internal class WearQuickEntryRepository(
    private val context: Context,
) {
    private val localStore = WearQuickEntryLocalStore(context)

    fun pendingCount(): Int = localStore.pendingCount()

    fun observePendingCount(): Flow<Int> = localStore.observePendingCount()

    /**
     * The watch reuses the locally cached category list as a light-weight source for first-screen
     * shortcuts. The phone already orders the payload with the most relevant categories first.
     */
    fun readCachedCategories(type: String): List<WearCategory> =
        localStore.readCategories(type)?.categories.orEmpty()

    suspend fun loadCategories(type: String): WearCategoriesResponse {
        return try {
            val response = requestCategoriesFromPhone(type)
            localStore.saveCategories(type, response)
            response
        } catch (throwable: Throwable) {
            localStore.readCategories(type)
                ?: throw throwable
        }
    }

    suspend fun saveEntry(
        type: String,
        amount: Double,
        category: WearCategory,
    ): WearSaveResult {
        val pendingEntry = PendingWearEntry(
            requestId = UUID.randomUUID().toString(),
            type = type,
            amount = amount,
            categoryName = category.name,
            categoryEmoji = category.emoji,
            categoryKey = category.systemKey,
        )

        return try {
            sendEntryToPhone(pendingEntry)
            flushPendingEntries()
            WearSaveResult.Saved
        } catch (throwable: Throwable) {
            if (throwable is PhoneRejectedException) throw throwable
            queueMutex.withLock {
                localStore.enqueue(pendingEntry)
                val queuedCount = localStore.readQueue().size
                WearQuickEntryQueueSignal.publish(queuedCount)
                WearSaveResult.Queued(queuedCount)
            }
        }
    }

    suspend fun flushPendingEntries(): Int {
        return queueMutex.withLock {
            val queuedEntries = localStore.readQueue()
            if (queuedEntries.isEmpty()) {
                WearQuickEntryQueueSignal.publish(0)
                return@withLock 0
            }

            val remainingEntries = mutableListOf<PendingWearEntry>()
            queuedEntries.forEach { entry ->
                val result = runCatching { sendEntryToPhone(entry) }
                if (result.isFailure) {
                    val throwable = result.exceptionOrNull()
                    remainingEntries.add(entry)
                    if (throwable !is PhoneRejectedException) {
                        remainingEntries.addAll(queuedEntries.drop(queuedEntries.indexOf(entry) + 1))
                        localStore.replaceQueue(remainingEntries)
                        WearQuickEntryQueueSignal.publish(remainingEntries.size)
                        return@withLock remainingEntries.size
                    }
                }
            }

            localStore.replaceQueue(remainingEntries)
            WearQuickEntryQueueSignal.publish(remainingEntries.size)
            remainingEntries.size
        }
    }

    suspend fun flushPendingEntriesWithRetries(
        maxAttempts: Int = 2,
        retryDelayMillis: Long = 1_000L,
    ): Int {
        var remaining = pendingCount()
        repeat(maxAttempts) { attempt ->
            if (remaining <= 0) return 0
            remaining = flushPendingEntries()
            if (remaining <= 0) return 0
            if (attempt < maxAttempts - 1) {
                delay(retryDelayMillis)
            }
        }
        return remaining
    }

    private suspend fun requestCategoriesFromPhone(type: String): WearCategoriesResponse {
        val nodeId = findPhoneNodeId()
        val payload = JSONObject()
            .put("type", type)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val response = request(nodeId, WearQuickEntryProtocol.PATH_CATEGORIES, payload)
        return WearCategoriesResponse(
            currency = response.optString("currency", "EUR"),
            categories = response.optJSONArray("categories")?.toWearCategories().orEmpty(),
        )
    }

    private suspend fun sendEntryToPhone(entry: PendingWearEntry) {
        val nodeId = findPhoneNodeId()
        val payload = JSONObject()
            .put("requestId", entry.requestId)
            .put("type", entry.type)
            .put("amount", entry.amount)
            .put("categoryName", entry.categoryName)
            .put("categoryEmoji", entry.categoryEmoji)
            .put("categoryKey", entry.categoryKey ?: JSONObject.NULL)
            .toString()
            .toByteArray(Charsets.UTF_8)
        request(nodeId, WearQuickEntryProtocol.PATH_SAVE, payload)
    }

    private suspend fun request(
        nodeId: String,
        path: String,
        payload: ByteArray,
    ): JSONObject {
        val responseBytes = try {
            withTimeout(REQUEST_TIMEOUT_MILLIS) {
                Wearable.getMessageClient(context)
                    .sendRequest(nodeId, path, payload)
                    .await()
            }
        } catch (throwable: Throwable) {
            throw PhoneUnavailableException(
                throwable.message ?: context.getString(R.string.wear_phone_unavailable),
                throwable,
            )
        }

        val response = JSONObject(responseBytes.decodeToString())
        if (!response.optBoolean("ok")) {
            throw PhoneRejectedException(
                response.optString("message", "Wear quick entry request was rejected."),
            )
        }
        return response
    }

    private suspend fun findPhoneNodeId(): String {
        val reachableCapability = Wearable.getCapabilityClient(context)
            .getCapability(
                WearQuickEntryProtocol.PHONE_CAPABILITY,
                CapabilityClient.FILTER_REACHABLE,
            )
            .await()
        val capableNode = reachableCapability.nodes.firstOrNull { node -> node.isNearby }
            ?: reachableCapability.nodes.firstOrNull()
        if (capableNode != null) {
            return capableNode.id
        }

        val connectedNodes = Wearable.getNodeClient(context).connectedNodes.await()
        val nearbyNode = connectedNodes.firstOrNull { node -> node.isNearby }
            ?: connectedNodes.firstOrNull()

        return nearbyNode?.id ?: throw PhoneUnavailableException(context.getString(R.string.wear_phone_unavailable))
    }

    private companion object {
        const val REQUEST_TIMEOUT_MILLIS = 8_000L
        val queueMutex = Mutex()
    }
}

/**
 * Transport-level failure: the watch couldn't reach the phone right now.
 */
internal class PhoneUnavailableException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Logical failure returned by the phone app itself, for example invalid session or validation.
 */
internal class PhoneRejectedException(
    message: String,
) : RuntimeException(message)
