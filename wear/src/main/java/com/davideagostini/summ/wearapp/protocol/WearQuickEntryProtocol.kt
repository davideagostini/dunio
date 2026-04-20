/**
 * Wire protocol constants for the Wear-to-Phone quick-entry communication.
 *
 * This object centralises every path, capability name and URL fragment used by
 * the Wear Data Layer messaging. Both the watch-side [WearQuickEntryRepository]
 * and the phone-side listener must reference the same constants so that requests
 * and responses are routed correctly.
 *
 * The protocol supports two operations in V1:
 * 1. **Categories request** – the watch asks the phone for the category list.
 * 2. **Save request** – the watch sends a completed entry to the phone.
 *
 * Pending entries that cannot be delivered immediately are stored under
 * [PATH_PENDING_PREFIX] and automatically synchronised by the Wear platform
 * when the phone becomes reachable again.
 *
 * All constants are `const val` so they are inlined at compile time with no
 * runtime allocation overhead.
 */
package com.davideagostini.summ.wearapp.protocol

/**
 * Singleton holding the RPC contract between the Wear app and the phone app.
 *
 * The phone app advertises itself with the capability [PHONE_CAPABILITY].
 * The watch uses Wearable [MessageClient] to send synchronous request/response
 * calls, and [DataClient] to persist pending entries that the platform will
 * deliver later.
 */
internal object WearQuickEntryProtocol {

    /**
     * Capability string that the phone app advertises via
     * [CapabilityClient.addLocalCapability]. The watch discovers the phone
     * by querying this capability with [CapabilityClient.FILTER_REACHABLE].
     */
    const val PHONE_CAPABILITY = "summ_phone_app"

    /**
     * Base path prefix shared by all quick-entry Data Layer endpoints.
     */
    const val PATH_PREFIX = "/wear/quick-entry"

    /**
     * Path used to request the category list for a given transaction type.
     * The request payload is a JSON object: `{"type": "expense" | "income"}`.
     */
    const val PATH_CATEGORIES = "$PATH_PREFIX/categories"

    /**
     * Path used to immediately save a completed quick entry on the phone.
     * The request payload is a JSON object containing the full entry data.
     */
    const val PATH_SAVE = "$PATH_PREFIX/save"

    /**
     * Path prefix under which pending (not-yet-synced) entries are stored
     * as individual Data Items. Each item gets a unique sub-path that
     * includes the entry's request ID.
     */
    const val PATH_PENDING_PREFIX = "$PATH_PREFIX/pending"

    /**
     * Builds the full Data Layer path for a specific pending entry.
     *
     * The resulting path is unique per request ID, which allows multiple
     * pending entries to coexist without overwriting each other.
     *
     * @param requestId The unique identifier of the pending entry.
     * @return The full path, e.g. "/wear/quick-entry/pending/abc-123".
     */
    fun pendingEntryPath(requestId: String): String = "$PATH_PENDING_PREFIX/$requestId"
}
