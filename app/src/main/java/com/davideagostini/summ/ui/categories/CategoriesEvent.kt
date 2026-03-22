package com.davideagostini.summ.ui.categories

import com.davideagostini.summ.data.entity.Category

sealed class CategoriesEvent {
    // list
    data class Select(val category: Category) : CategoriesEvent()
    data object StartAdd              : CategoriesEvent()
    // action sheet navigation
    data object StartEdit             : CategoriesEvent()
    data object RequestDelete         : CategoriesEvent()
    data object DismissSheet          : CategoriesEvent()
    // shared form fields (used for both Add and Edit)
    data class UpdateFormName(val value: String)  : CategoriesEvent()
    data class UpdateFormEmoji(val value: String) : CategoriesEvent()
    data object SaveAdd               : CategoriesEvent()
    data object SaveEdit              : CategoriesEvent()
    // delete dialog
    data object ConfirmDelete         : CategoriesEvent()
    data object DismissDeleteDialog   : CategoriesEvent()
}
