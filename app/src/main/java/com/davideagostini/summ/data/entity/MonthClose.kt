package com.davideagostini.summ.data.entity

data class MonthClose(
    val id: String = "",
    val period: String = "",
    val status: String = "draft",
    val assetSnapshotCount: Int = 0,
    val transactionCount: Int = 0,
    val recurringMissingCount: Int = 0,
    val closedBy: String? = null,
)
