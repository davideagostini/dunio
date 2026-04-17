package com.davideagostini.summ.data.entity

data class Entry(
    val id: String = "",
    val householdId: String = "",
    val type: String,
    val description: String,
    val price: Double,
    val category: String,
    val categoryKey: String? = null,
    val date: Long = System.currentTimeMillis(),
    val period: String = "",
    val recurringTransactionId: String? = null,
    val createdAt: Long = 0L,
)
