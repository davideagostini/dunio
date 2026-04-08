package com.davideagostini.summ.ui.categories

import androidx.compose.runtime.Immutable
import com.davideagostini.summ.data.entity.Category

enum class CategorySheetMode { Hidden, Action, Add, Edit, Success }

@Immutable
data class CategoriesUiState(
    val sheetMode: CategorySheetMode  = CategorySheetMode.Hidden,
    val selectedCategory: Category?   = null,
    val editName: String              = "",
    val editEmoji: String             = "",
    val nameError: String?            = null,
    val operationErrorMessage: String? = null,
    val showDeleteDialog: Boolean     = false,
    val isSaving: Boolean             = false,
)
