package com.davideagostini.summ.data.entity

data class AssetHistoryEntry(
    val id: String = "",
    val assetId: String = "",
    val householdId: String = "",
    val action: String = "updated",
    val name: String = "",
    val type: String = "asset",
    val category: String = "",
    val value: Double = 0.0,
    val currency: String = "EUR",
    val liquid: Boolean = false,
    val period: String = "",
    val snapshotDate: String = "",
)
