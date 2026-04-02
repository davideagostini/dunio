package com.davideagostini.summ.ui.entries

import androidx.compose.runtime.Immutable
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.domain.model.EntryDisplayItem

enum class EntrySheetMode { Hidden, Action, Edit, Success }
enum class EntriesFilterType { All, Expenses, Income }
enum class EntriesContentMode { Entries, Reports }

@Immutable
data class EntriesUiState(
    val selectedMonth: String?        = null,
    val filterType: EntriesFilterType = EntriesFilterType.All,
    val contentMode: EntriesContentMode = EntriesContentMode.Entries,
    val searchQuery: String           = "",
    val searchVisible: Boolean        = false,
    val sheetMode: EntrySheetMode     = EntrySheetMode.Hidden,
    val selectedEntry: EntryDisplayItem? = null,
    val editType: String              = "expense",
    val editDescription: String       = "",
    val editPrice: String             = "",
    val editDate: Long                = System.currentTimeMillis(),
    val editCategory: Category?       = null,
    val descriptionError: String?     = null,
    val priceError: String?           = null,
    val operationErrorMessage: String? = null,
    val isSaving: Boolean             = false,
    val showDeleteDialog: Boolean     = false,
)
