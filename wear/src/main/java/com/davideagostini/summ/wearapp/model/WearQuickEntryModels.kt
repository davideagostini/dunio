package com.davideagostini.summ.wearapp.model

import java.util.UUID

/**
 * DTO returned to the watch UI for category selection.
 */
internal data class WearCategory(
    val name: String,
    val emoji: String,
    val systemKey: String?,
)

/**
 * Full category payload fetched from the phone, including the active household currency.
 */
internal data class WearCategoriesResponse(
    val currency: String,
    val categories: List<WearCategory>,
)

/**
 * Result of a watch save attempt.
 *
 * `Saved` means the phone handled it right away.
 * `Queued` means the watch stored it locally and will retry as soon as the phone is reachable.
 */
internal sealed interface WearSaveResult {
    data object Saved : WearSaveResult
    data class Queued(val pendingCount: Int) : WearSaveResult
}

/**
 * Minimal payload needed to sync a queued watch entry later.
 */
internal data class PendingWearEntry(
    val requestId: String = UUID.randomUUID().toString(),
    val type: String,
    val amount: Double,
    val categoryName: String,
    val categoryEmoji: String,
    val categoryKey: String?,
)
