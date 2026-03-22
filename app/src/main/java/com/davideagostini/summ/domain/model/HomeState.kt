package com.davideagostini.summ.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class HomeState(
    val entries: List<EntryDisplayItem> = emptyList(),
    val balance: Double = 0.0,
)
