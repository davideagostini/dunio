/**
 * Model definitions for the Wear OS quick-entry module.
 *
 * This file contains all data structures used across the Wear app layers:
 * - [WearCategory]: a single category displayed in the UI.
 * - [WearCategoriesResponse]: the full payload received from the phone, including currency.
 * - [WearSaveResult]: the outcome of a save attempt (immediate vs. queued).
 * - [PendingWearEntry]: a lightweight representation of an entry waiting to be synced.
 *
 * All types are scoped as `internal` because they are only consumed within the Wear app module.
 */
package com.davideagostini.summ.wearapp.model

import java.util.UUID

/**
 * Represents a single transaction category shown on the watch screen.
 *
 * Instances are parsed from the JSON payload sent by the phone app through
 * the Wear Data Layer. The [systemKey] is nullable because custom user-created
 * categories may not have a system-defined key.
 *
 * @property name      Human-readable category name (e.g. "Groceries").
 * @property emoji     Emoji icon used as a visual shortcut on the small watch display.
 * @property systemKey Optional system-defined identifier for built-in categories.
 */
internal data class WearCategory(
    val name: String,
    val emoji: String,
    val systemKey: String?,
)

/**
 * Envelope for the category list downloaded from the phone.
 *
 * The phone app sends this in response to a category request. The [currency]
 * code is included so the watch can format the amount correctly without having
 * to maintain its own currency settings.
 *
 * @property currency   ISO 4217 currency code (e.g. "EUR", "USD").
 * @property categories Ordered list of categories, most-relevant first.
 */
internal data class WearCategoriesResponse(
    val currency: String,
    val categories: List<WearCategory>,
)

/**
 * Sealed result of a watch-initiated save attempt.
 *
 * The Wear app never writes to Firebase directly. Instead it tries to forward
 * the entry to the phone in real-time. When the phone is unreachable the entry
 * is persisted in the Wear Data Layer for automatic platform-sync later.
 *
 * Implementations:
 * - [Saved]: the phone handled the entry immediately.
 * - [Queued]: the entry was stored locally and will sync when the phone reconnects;
 *   [Queued.pendingCount] reports how many entries are currently waiting.
 */
internal sealed interface WearSaveResult {

    /**
     * The phone successfully received and persisted the entry in real-time.
     */
    data object Saved : WearSaveResult

    /**
     * The entry was queued in the Wear Data Layer for deferred synchronization.
     *
     * @property pendingCount Total number of pending entries (including this one).
     */
    data class Queued(val pendingCount: Int) : WearSaveResult
}

/**
 * Lightweight representation of a quick entry that has not yet been confirmed by the phone.
 *
 * A new [PendingWearEntry] is created every time the user completes the flow on the watch.
 * The [requestId] is a random UUID used as a unique key in the Wear Data Layer so that
 * multiple pending entries can coexist without collisions.
 *
 * @property requestId      Unique identifier for this request (auto-generated UUID).
 * @property type           Transaction type: "expense" or "income".
 * @property amount         Monetary value in the household currency.
 * @property categoryName   Display name of the selected category.
 * @property categoryEmoji  Emoji icon of the selected category.
 * @property categoryKey    System key of the category, or null for custom categories.
 */
internal data class PendingWearEntry(
    val requestId: String = UUID.randomUUID().toString(),
    val type: String,
    val amount: Double,
    val categoryName: String,
    val categoryEmoji: String,
    val categoryKey: String?,
)
