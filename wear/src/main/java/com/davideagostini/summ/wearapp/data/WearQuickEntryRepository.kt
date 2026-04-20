/**
 * Data layer for the Wear OS quick-entry feature.
 *
 * This file contains the main repository class and two transport-level
 * exception types used to communicate failure semantics to the ViewModel:
 * - [PhoneUnavailableException]: the watch could not reach the phone at all.
 * - [PhoneRejectedException]: the phone received the request but rejected it
 *   (e.g. due to a validation error or invalid session).
 *
 * The watch never talks to Firebase directly in V1. All data flows through
 * the Wear Data Layer to the companion phone app.
 */
package com.davideagostini.summ.wearapp.data

import android.content.Context
import android.net.Uri
import com.davideagostini.summ.wearapp.R
import com.davideagostini.summ.wearapp.data.WearQuickEntryRepository.Companion.backgroundScope
import com.davideagostini.summ.wearapp.model.PendingWearEntry
import com.davideagostini.summ.wearapp.model.WearCategoriesResponse
import com.davideagostini.summ.wearapp.model.WearCategory
import com.davideagostini.summ.wearapp.model.WearSaveResult
import com.davideagostini.summ.wearapp.protocol.WearQuickEntryProtocol
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

/**
 * Watch-side repository that mediates all communication between the Wear app
 * and the phone app for the quick-entry feature.
 *
 * The repository is responsible for three main operations:
 * 1. **Loading categories** – requests the category list from the phone and
 *    caches it locally via [WearQuickEntryLocalStore]. Falls back to the
 *    cache when the phone is unreachable.
 * 2. **Saving entries** – attempts to send the completed entry to the phone
 *    in real-time. If that fails, the entry is persisted as a pending Data
 *    Layer item for automatic platform-sync.
 * 3. **Observing pending entries** – exposes a [Flow] of the pending entry
 *    count so the UI can display a queue-status chip.
 *
 * The class is `internal` to the Wear app module.
 *
 * @param context Application context used for Data Layer clients and string resources.
 */
internal class WearQuickEntryRepository(
    private val context: Context,
) {
    /** Local cache for category lists grouped by transaction type. */
    private val localStore = WearQuickEntryLocalStore(context)

    /** Lazy reference to the Wear Data Client for reading/writing Data Items. */
    private val dataClient by lazy { Wearable.getDataClient(context) }

    /**
     * Reads the locally cached category list for the given transaction type.
     *
     * This is a synchronous call that returns immediately from SharedPreferences.
     * Used as a fast path to populate the UI before the phone round-trip completes.
     *
     * @param type Transaction type ("expense" or "income").
     * @return Cached categories, or an empty list if none are available.
     */
    fun readCachedCategories(type: String): List<WearCategory> =
        localStore.readCategories(type)?.categories.orEmpty()

    /**
     * Returns the current number of pending entries stored in the Wear Data Layer.
     *
     * This is a suspending function because it queries the Data Client asynchronously.
     *
     * @return Count of pending Data Items under the pending-prefix path.
     */
    suspend fun pendingCount(): Int = queryPendingCount()

    /**
     * Observes the pending entry count as a reactive [Flow].
     *
     * The flow emits a new count every time a Data Item under the pending-prefix
     * path is created, changed, or deleted. This allows the UI to keep the
     * queue-status chip up-to-date in real time without polling.
     *
     * Uses [callbackFlow] to bridge the [DataClient.OnDataChangedListener]
     * callback into a cold flow. The listener is automatically removed when
     * the flow collector is cancelled.
     *
     * @return A [Flow] that emits the current pending entry count, deduplicated
     *         with [distinctUntilChanged].
     */
    fun observePendingCount(): Flow<Int> = callbackFlow {
        /**
         * Queries the current pending count and sends it to the flow collector.
         * Runs on [backgroundScope] because Data Client operations perform I/O.
         */
        suspend fun emitPendingCount() {
            trySend(queryPendingCount())
        }

        val listener = DataClient.OnDataChangedListener { events ->
            val affectsPendingEntries = events.any { event ->
                event.dataItem.uri.path?.startsWith(WearQuickEntryProtocol.PATH_PENDING_PREFIX) == true
            }
            if (affectsPendingEntries) {
                backgroundScope.launch {
                    emitPendingCount()
                }
            }
        }

        dataClient.addListener(listener)
        backgroundScope.launch {
            emitPendingCount()
        }
        awaitClose {
            dataClient.removeListener(listener)
        }
    }.distinctUntilChanged()

    /**
     * Loads categories from the phone, falling back to the local cache on failure.
     *
     * On success, the response is persisted to the local store for future offline use.
     * On failure (phone unreachable, timeout, etc.), the cached version is returned
     * if available; otherwise the original exception is re-thrown.
     *
     * @param type Transaction type ("expense" or "income").
     * @return The category response (fresh or cached).
     * @throws PhoneUnavailableException if the phone is unreachable and no cache exists.
     */
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

    /**
     * Attempts to save a quick entry by sending it to the phone.
     *
     * The method first tries a real-time request. If the phone is reachable,
     * the entry is saved immediately and [WearSaveResult.Saved] is returned.
     * If the phone cannot be reached (timeout, network error), the entry is
     * persisted as a pending Data Layer item and [WearSaveResult.Queued] is
     * returned.
     *
     * A [PhoneRejectedException] is **not** queued — it indicates a logical
     * rejection by the phone (e.g. invalid session) and is propagated to the
     * caller so the user sees an error message.
     *
     * @param type     Transaction type ("expense" or "income").
     * @param amount   Monetary amount entered by the user.
     * @param category The selected category.
     * @return [WearSaveResult.Saved] or [WearSaveResult.Queued].
     * @throws PhoneRejectedException if the phone explicitly rejected the request.
     */
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
            sendEntryToPhone(
                entry = pendingEntry,
                nodeLookupTimeoutMillis = INTERACTIVE_NODE_LOOKUP_TIMEOUT_MILLIS,
            )
            WearSaveResult.Saved
        } catch (throwable: Throwable) {
            if (throwable is PhoneRejectedException) throw throwable
            enqueuePendingEntry(pendingEntry)
            val queuedCount = queryPendingCount()
            WearSaveResult.Queued(queuedCount)
        }
    }

    /**
     * Sends a category-request message to the phone and parses the response.
     *
     * The request payload is a JSON object with a single "type" field.
     * The response is expected to contain "currency" and "categories" fields.
     *
     * @param type Transaction type to request categories for.
     * @return Parsed [WearCategoriesResponse] from the phone.
     * @throws PhoneUnavailableException if the phone cannot be reached.
     */
    private suspend fun requestCategoriesFromPhone(type: String): WearCategoriesResponse {
        val nodeId = findPhoneNodeId(SYNC_NODE_LOOKUP_TIMEOUT_MILLIS)
        val payload = JSONObject()
            .put("type", type)
            .toString()
            .toByteArray(Charsets.UTF_8)
        val response = request(
            nodeId = nodeId,
            path = WearQuickEntryProtocol.PATH_CATEGORIES,
            payload = payload,
            requestTimeoutMillis = CATEGORY_REQUEST_TIMEOUT_MILLIS,
        )
        return WearCategoriesResponse(
            currency = response.optString("currency", "EUR"),
            categories = response.optJSONArray("categories")?.toWearCategories().orEmpty(),
        )
    }

    /**
     * Persists a pending entry as a Data Item in the Wear Data Layer.
     *
     * Each entry is stored under a unique path that includes its request ID,
     * so multiple pending entries can coexist. The Wear platform will
     * automatically synchronize these items when the phone becomes reachable.
     *
     * @param entry The pending entry to persist.
     */
    private suspend fun enqueuePendingEntry(entry: PendingWearEntry) {
        val path = WearQuickEntryProtocol.pendingEntryPath(entry.requestId)
        val request = PutDataMapRequest.create(path)
            .apply {
                dataMap.putString("requestId", entry.requestId)
                dataMap.putString("type", entry.type)
                dataMap.putDouble("amount", entry.amount)
                dataMap.putString("categoryName", entry.categoryName)
                dataMap.putString("categoryEmoji", entry.categoryEmoji)
                dataMap.putString("categoryKey", entry.categoryKey ?: "")
            }
            .asPutDataRequest()

        dataClient.putDataItem(request).await()
    }

    /**
     * Counts the number of Data Items currently stored under the pending-prefix path.
     *
     * Uses [DataClient.FILTER_PREFIX] to match all items whose path starts
     * with the pending prefix. The buffer is always released in a `finally`
     * block to avoid memory leaks.
     *
     * @return The number of pending entries.
     */
    private suspend fun queryPendingCount(): Int {
        val buffer = dataClient.getDataItems(pendingEntriesUri, DataClient.FILTER_PREFIX).await()
        return try {
            buffer.count
        } finally {
            buffer.release()
        }
    }

    /**
     * Sends a completed entry to the phone via a synchronous MessageClient request.
     *
     * The entry data is serialized as a JSON byte array and sent on the
     * [WearQuickEntryProtocol.PATH_SAVE] path. The phone is expected to
     * return a JSON response with an "ok" boolean.
     *
     * @param entry                   The entry to send.
     * @param nodeLookupTimeoutMillis Timeout for discovering the phone node.
     */
    private suspend fun sendEntryToPhone(
        entry: PendingWearEntry,
        nodeLookupTimeoutMillis: Long = SYNC_NODE_LOOKUP_TIMEOUT_MILLIS,
    ) {
        val nodeId = findPhoneNodeId(nodeLookupTimeoutMillis)
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

    /**
     * Sends a synchronous request/response message to the phone and parses the JSON response.
     *
     * This is the core transport method used by both category-loading and entry-saving
     * operations. It wraps [MessageClient.sendRequest] with a timeout and converts
     * transport errors into [PhoneUnavailableException]. If the phone returns a
     * response with `"ok": false`, a [PhoneRejectedException] is thrown.
     *
     * @param nodeId  The phone node to send the request to.
     * @param path    The Data Layer path for the request.
     * @param payload The JSON-encoded request body.
     * @return The parsed JSON response from the phone.
     * @throws PhoneUnavailableException on transport failure or timeout.
     * @throws PhoneRejectedException if the phone responded with an error.
     */
    private suspend fun request(
        nodeId: String,
        path: String,
        payload: ByteArray,
        requestTimeoutMillis: Long = REQUEST_TIMEOUT_MILLIS,
    ): JSONObject {
        val responseBytes = try {
            withTimeout(requestTimeoutMillis) {
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

    /**
     * Discovers the phone node ID using the Wear capability and node APIs.
     *
     * The discovery proceeds in two stages:
     * 1. Query the capability advertised by the phone ([PHONE_CAPABILITY]) and
     *    pick the nearest reachable node.
     * 2. If no capable node is found, fall back to the connected-nodes list
     *    and pick the nearest one.
     *
     * If neither query produces a node, a [PhoneUnavailableException] is thrown.
     *
     * @param nodeLookupTimeoutMillis Maximum time to wait for each discovery stage.
     * @return The node ID of the phone.
     * @throws PhoneUnavailableException if no phone node can be found.
     */
    private suspend fun findPhoneNodeId(nodeLookupTimeoutMillis: Long): String {
        val reachableCapability = try {
            withTimeout(nodeLookupTimeoutMillis) {
                Wearable.getCapabilityClient(context)
                    .getCapability(
                        WearQuickEntryProtocol.PHONE_CAPABILITY,
                        CapabilityClient.FILTER_REACHABLE,
                    )
                    .await()
            }
        } catch (throwable: Throwable) {
            throw PhoneUnavailableException(
                throwable.message ?: context.getString(R.string.wear_phone_unavailable),
                throwable,
            )
        }
        val capableNode = reachableCapability.nodes.firstOrNull { node -> node.isNearby }
            ?: reachableCapability.nodes.firstOrNull()
        if (capableNode != null) {
            return capableNode.id
        }

        val connectedNodes = try {
            withTimeout(nodeLookupTimeoutMillis) {
                Wearable.getNodeClient(context).connectedNodes.await()
            }
        } catch (throwable: Throwable) {
            throw PhoneUnavailableException(
                throwable.message ?: context.getString(R.string.wear_phone_unavailable),
                throwable,
            )
        }
        val nearbyNode = connectedNodes.firstOrNull { node -> node.isNearby }
            ?: connectedNodes.firstOrNull()

        return nearbyNode?.id ?: throw PhoneUnavailableException(context.getString(R.string.wear_phone_unavailable))
    }

    /**
     * Companion object holding timeout constants and shared infrastructure.
     */
    private companion object {
        /** Maximum time to wait for a response after a request has been sent to the phone. */
        const val REQUEST_TIMEOUT_MILLIS = 8_000L

        /** Categories should fail fast so the user sees cached content or a retry state quickly. */
        const val CATEGORY_REQUEST_TIMEOUT_MILLIS = 2_000L

        /** Shorter timeout used when the user is actively interacting (save button tap). */
        const val INTERACTIVE_NODE_LOOKUP_TIMEOUT_MILLIS = 2_000L

        /** Longer timeout used for background sync operations (category loading). */
        const val SYNC_NODE_LOOKUP_TIMEOUT_MILLIS = 8_000L

        /** URI used to query pending Data Items with [DataClient.FILTER_PREFIX]. */
        val pendingEntriesUri: Uri = Uri.Builder()
            .scheme("wear")
            .path(WearQuickEntryProtocol.PATH_PENDING_PREFIX)
            .build()

        /**
         * Background [CoroutineScope] used for Data Layer listener callbacks.
         * Uses [SupervisorJob] so a failure in one child coroutine does not
         * cancel siblings, and [Dispatchers.IO] for blocking Data Client calls.
         */
        val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}

/**
 * Transport-level failure indicating that the watch could not establish
 * communication with the phone (timeout, network error, no paired node, etc.).
 *
 * @param message Human-readable description of the failure.
 * @param cause   Optional underlying exception.
 */
internal class PhoneUnavailableException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * Logical failure returned by the phone app itself.
 *
 * This indicates that the phone received the request but rejected it,
 * for example because the user's session is invalid or the entry failed
 * server-side validation. These errors should be shown to the user rather
 * than silently queued.
 *
 * @param message Human-readable description of the rejection reason.
 */
internal class PhoneRejectedException(
    message: String,
) : RuntimeException(message)
