package com.davideagostini.summ.data.entity

data class Asset(
    val id: String = "",
    val householdId: String = "",
    val name: String,
    val type: String = "asset",
    val category: String = "",
    val value: Double = 0.0,
    val currency: String = "EUR",
    val liquid: Boolean = false,
    val period: String = "",
    val snapshotDate: String = "",
)
