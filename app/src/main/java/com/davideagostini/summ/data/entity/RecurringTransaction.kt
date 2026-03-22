package com.davideagostini.summ.data.entity

data class RecurringTransaction(
    val id: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val type: String = "expense",
    val category: String = "",
    val dayOfMonth: Int = 1,
    val startDate: String = "",
    val active: Boolean = true,
    val lastAppliedDate: String? = null,
)
