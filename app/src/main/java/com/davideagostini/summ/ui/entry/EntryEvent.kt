package com.davideagostini.summ.ui.entry

import com.davideagostini.summ.data.entity.Category

sealed class EntryEvent {
    data class SelectType(val type: String)         : EntryEvent()
    data class UpdateDate(val value: Long)          : EntryEvent()
    data class UpdateDescription(val value: String) : EntryEvent()
    data class UpdatePrice(val value: String)       : EntryEvent()
    data class SelectCategory(val category: Category) : EntryEvent()
    data object Next : EntryEvent()
    data object Back : EntryEvent()
    data object Save : EntryEvent()
}
