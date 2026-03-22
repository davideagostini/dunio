package com.davideagostini.summ.ui.assets

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Asset
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.data.repository.AssetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssetsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: AssetRepository,
) : ViewModel() {
    private val historyLoaded = MutableStateFlow(false)

    val assetHistory: StateFlow<List<AssetHistoryEntry>> = repository.allAssetHistory
        .onEach { historyLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoading: StateFlow<Boolean> = historyLoaded
        .map { loaded -> !loaded }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _uiState = MutableStateFlow(AssetsUiState())
    val uiState: StateFlow<AssetsUiState> = _uiState.asStateFlow()

    fun handleEvent(event: AssetsEvent) {
        when (event) {
            AssetsEvent.StartAdd -> _uiState.update {
                it.copy(
                    sheetMode = AssetSheetMode.Add,
                    selectedAsset = null,
                    editName = "",
                    editCategory = "",
                    editValue = "",
                    editCurrency = "EUR",
                    editType = "asset",
                    nameError = null,
                    valueError = null,
                )
            }
            AssetsEvent.CopyPreviousMonth -> copyPreviousMonth()

            is AssetsEvent.Select -> _uiState.update {
                it.copy(sheetMode = AssetSheetMode.Action, selectedAsset = event.asset)
            }

            AssetsEvent.StartEdit -> {
                val asset = _uiState.value.selectedAsset ?: return
                _uiState.update {
                    it.copy(
                        sheetMode = AssetSheetMode.Edit,
                        editName = asset.name,
                        editCategory = asset.category,
                        editValue = if (asset.value == 0.0) "" else asset.value.toPlainString(),
                        editCurrency = asset.currency,
                        editType = asset.type,
                        nameError = null,
                        valueError = null,
                    )
                }
            }

            AssetsEvent.RequestDelete -> _uiState.update { it.copy(showDeleteDialog = true) }
            AssetsEvent.ConfirmDelete -> confirmDelete()
            AssetsEvent.DismissDeleteDialog -> _uiState.update { it.copy(showDeleteDialog = false) }
            AssetsEvent.DismissSheet -> _uiState.update {
                AssetsUiState(
                    selectedMonth = it.selectedMonth,
                    searchVisible = it.searchVisible,
                    searchQuery = it.searchQuery,
                )
            }
            is AssetsEvent.SelectMonth -> _uiState.update { it.copy(selectedMonth = event.month) }
            AssetsEvent.ToggleSearch -> _uiState.update {
                it.copy(searchVisible = !it.searchVisible)
            }
            is AssetsEvent.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = event.value) }
            is AssetsEvent.UpdateName -> _uiState.update { it.copy(editName = event.value, nameError = null) }
            is AssetsEvent.UpdateCategory -> _uiState.update { it.copy(editCategory = event.value) }
            is AssetsEvent.UpdateValue -> _uiState.update { it.copy(editValue = event.value, valueError = null) }
            is AssetsEvent.UpdateCurrency -> _uiState.update { it.copy(editCurrency = event.value.uppercase()) }
            is AssetsEvent.UpdateType -> _uiState.update { it.copy(editType = event.value) }
            AssetsEvent.SaveAdd -> saveAdd()
            AssetsEvent.SaveEdit -> saveEdit()
        }
    }

    private fun saveAdd() {
        val state = _uiState.value
        val value = validateValue(state) ?: return
        if (state.editName.isBlank()) {
            _uiState.update { it.copy(nameError = appContext.getString(R.string.asset_validation_name_required)) }
            return
        }
        viewModelScope.launch {
            repository.insert(
                Asset(
                    name = state.editName.trim(),
                    category = state.editCategory.trim(),
                    value = value,
                    currency = state.editCurrency.ifBlank { "EUR" },
                    type = state.editType,
                    period = state.selectedMonth.orEmpty(),
                    snapshotDate = monthToSnapshotDate(state.selectedMonth),
                )
            )
            showSuccess()
        }
    }

    private fun saveEdit() {
        val state = _uiState.value
        val current = state.selectedAsset ?: return
        val value = validateValue(state) ?: return
        if (state.editName.isBlank()) {
            _uiState.update { it.copy(nameError = appContext.getString(R.string.asset_validation_name_required)) }
            return
        }
        viewModelScope.launch {
            repository.update(
                current.copy(
                    name = state.editName.trim(),
                    category = state.editCategory.trim(),
                    value = value,
                    currency = state.editCurrency.ifBlank { "EUR" },
                    type = state.editType,
                    period = state.selectedMonth.orEmpty(),
                    snapshotDate = monthToSnapshotDate(state.selectedMonth),
                )
            )
            showSuccess()
        }
    }

    private fun confirmDelete() {
        val current = _uiState.value.selectedAsset ?: return
        viewModelScope.launch {
            repository.delete(current)
            showSuccess()
        }
    }

    private suspend fun showSuccess() {
        _uiState.update { it.copy(sheetMode = AssetSheetMode.Success, showDeleteDialog = false) }
        delay(1_500L)
        _uiState.update {
            AssetsUiState(
                selectedMonth = it.selectedMonth,
                searchVisible = it.searchVisible,
                searchQuery = it.searchQuery,
            )
        }
    }

    private fun validateValue(state: AssetsUiState): Double? {
        val value = state.editValue.replace(',', '.').toDoubleOrNull()
        if (value == null) {
            _uiState.update { it.copy(valueError = appContext.getString(R.string.asset_validation_value_required)) }
            return null
        }
        return value
    }

    private fun Double.toPlainString(): String =
        if (this % 1.0 == 0.0) toInt().toString() else toString()

    private fun monthToSnapshotDate(month: String?): String =
        java.time.YearMonth.parse(month ?: java.time.YearMonth.now().toString())
            .atEndOfMonth()
            .toString()

    private fun copyPreviousMonth() {
        val selectedMonth = _uiState.value.selectedMonth ?: java.time.YearMonth.now().toString()
        val previousMonth = java.time.YearMonth.parse(selectedMonth).minusMonths(1).toString()
        val currentAssets = buildAssetsSnapshotForMonth(assetHistory.value, selectedMonth)
        val previousAssets = buildAssetsSnapshotForMonth(assetHistory.value, previousMonth)

        if (previousAssets.isEmpty()) return

        val currentNames = currentAssets.mapTo(mutableSetOf()) { normalizeAssetName(it.name) }
        val assetsToCopy = previousAssets.filter { asset ->
            normalizeAssetName(asset.name) !in currentNames
        }

        if (assetsToCopy.isEmpty()) return

        viewModelScope.launch {
            assetsToCopy.forEach { asset ->
                repository.insert(
                    asset.copy(
                        period = selectedMonth,
                        snapshotDate = monthToSnapshotDate(selectedMonth),
                    )
                )
            }
        }
    }

    private fun buildAssetsSnapshotForMonth(
        entries: List<AssetHistoryEntry>,
        month: String,
    ): List<Asset> = entries
        .filter { it.period == month }
        .associateBy { normalizeAssetName(it.name) }
        .values
        .filter { it.action != "deleted" }
        .map { entry ->
            Asset(
                id = entry.assetId,
                householdId = entry.householdId,
                name = entry.name,
                type = entry.type,
                category = entry.category,
                value = entry.value,
                currency = entry.currency,
                liquid = entry.liquid,
                period = entry.period,
                snapshotDate = entry.snapshotDate,
            )
        }

    private fun normalizeAssetName(value: String): String = value.trim().lowercase()
}
