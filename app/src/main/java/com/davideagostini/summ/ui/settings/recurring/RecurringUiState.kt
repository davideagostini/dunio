package com.davideagostini.summ.ui.settings.recurring

import androidx.compose.runtime.Immutable
import com.davideagostini.summ.data.entity.RecurringTransaction

enum class RecurringSheetMode { Hidden, Action, Add, Edit }

@Immutable
data class RecurringUiState(
    val searchVisible: Boolean = false,
    val searchQuery: String = "",
    val sheetMode: RecurringSheetMode = RecurringSheetMode.Hidden,
    val selectedRecurring: RecurringTransaction? = null,
    val description: String = "",
    val amount: String = "",
    val type: String = "expense",
    val category: String = "",
    val dayOfMonth: String = "1",
    val startDate: Long = System.currentTimeMillis(),
    val active: Boolean = true,
    val descriptionError: String? = null,
    val amountError: String? = null,
    val dayError: String? = null,
    val showDeleteDialog: Boolean = false,
)
