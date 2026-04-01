package com.davideagostini.summ.ui.entries

import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.domain.model.EntryDisplayItem

sealed class EntriesEvent {
    data class SelectMonth(val monthKey: String)     : EntriesEvent()
    data class SelectFilter(val filter: EntriesFilterType) : EntriesEvent()
    data object ToggleContentMode                    : EntriesEvent()
    data object ToggleSearch                         : EntriesEvent()
    data class UpdateSearchQuery(val query: String)  : EntriesEvent()
    data class Select(val entry: EntryDisplayItem)  : EntriesEvent()
    data object StartEdit                           : EntriesEvent()
    data object RequestDelete                       : EntriesEvent()
    data object DismissSheet                        : EntriesEvent()
    data class UpdateType(val value: String)        : EntriesEvent()
    data class UpdateDescription(val value: String) : EntriesEvent()
    data class UpdatePrice(val value: String)       : EntriesEvent()
    data class UpdateDate(val value: Long)          : EntriesEvent()
    data class UpdateCategory(val category: Category) : EntriesEvent()
    data object SaveEdit                            : EntriesEvent()
    data object ConfirmDelete                       : EntriesEvent()
    data object DismissDeleteDialog                 : EntriesEvent()
}
