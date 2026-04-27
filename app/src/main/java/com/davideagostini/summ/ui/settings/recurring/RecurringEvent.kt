package com.davideagostini.summ.ui.settings.recurring

import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.RecurringTransaction

sealed interface RecurringEvent {
    data object ToggleSearch : RecurringEvent
    data class UpdateSearchQuery(val value: String) : RecurringEvent
    data object StartAdd : RecurringEvent
    data class Select(val recurring: RecurringTransaction) : RecurringEvent
    data object StartEdit : RecurringEvent
    data object DismissSheet : RecurringEvent
    data object RequestDelete : RecurringEvent
    data object DismissDeleteDialog : RecurringEvent
    data object ConfirmDelete : RecurringEvent
    data object ApplyDue : RecurringEvent
    data object DebugApplyDueNow : RecurringEvent
    data class UpdateDescription(val value: String) : RecurringEvent
    data class UpdateAmount(val value: String) : RecurringEvent
    data class UpdateType(val value: String) : RecurringEvent
    data class UpdateCategory(val value: Category) : RecurringEvent
    data class UpdateDayOfMonth(val value: String) : RecurringEvent
    data class UpdateStartDate(val value: Long) : RecurringEvent
    data class UpdateActive(val value: Boolean) : RecurringEvent
    data object SaveAdd : RecurringEvent
    data object SaveEdit : RecurringEvent
}
