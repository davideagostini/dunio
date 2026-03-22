package com.davideagostini.summ.ui.assets

import androidx.compose.runtime.Immutable
import com.davideagostini.summ.data.entity.Asset

enum class AssetSheetMode { Hidden, Action, Add, Edit, Success }

@Immutable
data class AssetsUiState(
    val selectedMonth: String? = null,
    val searchVisible: Boolean = false,
    val searchQuery: String = "",
    val sheetMode: AssetSheetMode = AssetSheetMode.Hidden,
    val selectedAsset: Asset? = null,
    val editName: String = "",
    val editCategory: String = "",
    val editValue: String = "",
    val editCurrency: String = "EUR",
    val editType: String = "asset",
    val nameError: String? = null,
    val valueError: String? = null,
    val showDeleteDialog: Boolean = false,
)
