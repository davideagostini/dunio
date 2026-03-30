package com.davideagostini.summ.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class EntryDisplayItem(
    val id: String,
    val type: String,
    val description: String,
    val price: Double,
    val category: String,
    val categoryKey: String? = null,
    val emoji: String,
    val date: Long,
)
