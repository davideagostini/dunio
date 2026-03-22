package com.davideagostini.summ.ui.assets

import com.davideagostini.summ.data.entity.Asset

sealed interface AssetsEvent {
    data object StartAdd : AssetsEvent
    data object CopyPreviousMonth : AssetsEvent
    data class Select(val asset: Asset) : AssetsEvent
    data object StartEdit : AssetsEvent
    data object RequestDelete : AssetsEvent
    data object ConfirmDelete : AssetsEvent
    data object DismissDeleteDialog : AssetsEvent
    data object DismissSheet : AssetsEvent
    data class SelectMonth(val month: String) : AssetsEvent
    data object ToggleSearch : AssetsEvent
    data class UpdateSearchQuery(val value: String) : AssetsEvent
    data class UpdateName(val value: String) : AssetsEvent
    data class UpdateCategory(val value: String) : AssetsEvent
    data class UpdateValue(val value: String) : AssetsEvent
    data class UpdateCurrency(val value: String) : AssetsEvent
    data class UpdateType(val value: String) : AssetsEvent
    data object SaveAdd : AssetsEvent
    data object SaveEdit : AssetsEvent
}
