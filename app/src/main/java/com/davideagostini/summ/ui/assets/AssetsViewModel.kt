package com.davideagostini.summ.ui.assets

// ViewModel for the assets feature: it owns the UI state, routes events, and
// turns repository calls into screen-visible transitions.
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Asset
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.data.entity.MonthClose
import com.davideagostini.summ.data.firebase.toFirestoreUserMessage
import com.davideagostini.summ.data.repository.AssetRepository
import com.davideagostini.summ.data.repository.MonthCloseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssetsViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val repository: AssetRepository,
    monthCloseRepository: MonthCloseRepository,
) : ViewModel() {
    // These flags gate the initial loading state until each data source has
    // emitted at least once.
    private val historyLoaded = MutableStateFlow(false)
    private val monthClosesLoaded = MutableStateFlow(false)

    val assetHistory: StateFlow<List<AssetHistoryEntry>> = repository.allAssetHistory
        .onEach { historyLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthCloses: StateFlow<List<MonthClose>> = monthCloseRepository.allMonthCloses
        .onEach { monthClosesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Loading remains true until both streams have emitted at least once so the
    // screen can initialize with complete data.
    val isLoading: StateFlow<Boolean> = combine(historyLoaded, monthClosesLoaded) { historyReady, monthClosesReady ->
        !historyReady || !monthClosesReady
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _uiState = MutableStateFlow(AssetsUiState())
    val uiState: StateFlow<AssetsUiState> = _uiState.asStateFlow()

    fun handleEvent(event: AssetsEvent) {
        when (event) {
            // Add starts from a clean form and clears stale validation state.
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
                    operationErrorMessage = null,
                )
            }
            // Copy previous month is a standalone bulk action, not a form action.
            AssetsEvent.CopyPreviousMonth -> copyPreviousMonth()

            // Selecting an asset opens the compact action sheet around that item.
            is AssetsEvent.Select -> _uiState.update {
                it.copy(sheetMode = AssetSheetMode.Action, selectedAsset = event.asset, operationErrorMessage = null)
            }

            // Edit pre-fills the form with the current asset so the user edits a
            // snapshot instead of reconstructing the asset from scratch.
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
                        operationErrorMessage = null,
                    )
                }
            }

            // Delete uses a confirmation dialog so the destructive action requires
            // an explicit second tap.
            AssetsEvent.RequestDelete -> _uiState.update { it.copy(showDeleteDialog = true, operationErrorMessage = null) }
            AssetsEvent.ConfirmDelete -> confirmDelete()
            AssetsEvent.DismissDeleteDialog -> _uiState.update { it.copy(showDeleteDialog = false) }
            // DismissSheet clears transient form state but preserves month/search
            // filters that belong to the screen, not the sheet.
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
            is AssetsEvent.UpdateName -> _uiState.update { it.copy(editName = event.value, nameError = null, operationErrorMessage = null) }
            is AssetsEvent.UpdateCategory -> _uiState.update { it.copy(editCategory = event.value, operationErrorMessage = null) }
            is AssetsEvent.UpdateValue -> _uiState.update { it.copy(editValue = event.value, valueError = null, operationErrorMessage = null) }
            is AssetsEvent.UpdateCurrency -> _uiState.update { it.copy(editCurrency = event.value.uppercase(), operationErrorMessage = null) }
            is AssetsEvent.UpdateType -> _uiState.update { it.copy(editType = event.value, operationErrorMessage = null) }
            AssetsEvent.SaveAdd -> saveAdd()
            AssetsEvent.SaveEdit -> saveEdit()
        }
    }

    private fun saveAdd() {
        val state = _uiState.value
        val value = validateValue(state) ?: return
        if (state.editName.isBlank()) {
            // Validation is surfaced inline instead of failing the write silently.
            _uiState.update { it.copy(nameError = appContext.getString(R.string.asset_validation_name_required)) }
            return
        }

        // Save failures are converted to a friendly inline message so asset writes never crash the screen.
        viewModelScope.launch {
            try {
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
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(operationErrorMessage = throwable.toFirestoreUserMessage(appContext)) }
            }
        }
    }

    private fun saveEdit() {
        val state = _uiState.value
        val current = state.selectedAsset ?: return
        val value = validateValue(state) ?: return
        if (state.editName.isBlank()) {
            // Edit validation mirrors the add flow so the user sees the same rules.
            _uiState.update { it.copy(nameError = appContext.getString(R.string.asset_validation_name_required)) }
            return
        }
        viewModelScope.launch {
            try {
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
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(operationErrorMessage = throwable.toFirestoreUserMessage(appContext)) }
            }
        }
    }

    private fun confirmDelete() {
        val current = _uiState.value.selectedAsset ?: return
        viewModelScope.launch {
            try {
                // Delete is executed through the repository, then the success state
                // is shown so the user gets the same feedback as add/edit.
                repository.delete(current)
                showSuccess()
            } catch (throwable: Throwable) {
                // On failure we close the confirmation dialog and surface the backend
                // message inline in the sheet instead.
                _uiState.update {
                    it.copy(
                        showDeleteDialog = false,
                        operationErrorMessage = throwable.toFirestoreUserMessage(appContext),
                    )
                }
            }
        }
    }

    private suspend fun showSuccess() {
        // Success is transient: show the confirmation state, keep it visible for a
        // short interval, then reset the screen back to its initial filters.
        _uiState.update { it.copy(sheetMode = AssetSheetMode.Success, showDeleteDialog = false, operationErrorMessage = null) }
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
            // Localized decimals are normalized before validation so comma input is
            // accepted where appropriate.
            _uiState.update { it.copy(valueError = appContext.getString(R.string.asset_validation_value_required)) }
            return null
        }
        return value
    }

    private fun Double.toPlainString(): String =
        if (this % 1.0 == 0.0) toInt().toString() else toString()

    private fun monthToSnapshotDate(month: String?): String =
        // A monthly asset snapshot always points to the end of the selected month,
        // even when the caller only passes a `YearMonth` string.
        java.time.YearMonth.parse(month ?: java.time.YearMonth.now().toString())
            .atEndOfMonth()
            .toString()

    private fun copyPreviousMonth() {
        val selectedMonth = _uiState.value.selectedMonth ?: java.time.YearMonth.now().toString()
        val previousMonth = java.time.YearMonth.parse(selectedMonth).minusMonths(1).toString()
        val currentAssets = buildAssetsSnapshotForMonth(assetHistory.value, selectedMonth)
        val previousAssets = buildAssetsSnapshotForMonth(assetHistory.value, previousMonth)

        // Nothing to copy means there is no useful user-visible action to run.
        if (previousAssets.isEmpty()) return

        val currentNames = currentAssets.mapTo(mutableSetOf()) { normalizeAssetName(it.name) }
        val assetsToCopy = previousAssets.filter { asset ->
            normalizeAssetName(asset.name) !in currentNames
        }

        // If all previous assets already exist in the current month, skip the bulk
        // write entirely.
        if (assetsToCopy.isEmpty()) return

        // Bulk copy performs multiple writes, so handle the whole operation as a single user-visible failure.
        viewModelScope.launch {
            try {
                assetsToCopy.forEach { asset ->
                    repository.insert(
                        asset.copy(
                            period = selectedMonth,
                            snapshotDate = monthToSnapshotDate(selectedMonth),
                        )
                    )
                }
                _uiState.update { it.copy(operationErrorMessage = null) }
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(operationErrorMessage = throwable.toFirestoreUserMessage(appContext)) }
            }
        }
    }

    private fun buildAssetsSnapshotForMonth(
        entries: List<AssetHistoryEntry>,
        month: String,
    ): List<Asset> = entries
        // History can contain multiple actions for the same asset, so we scope by
        // month first and then collapse by normalized name.
        .filter { it.period == month }
        .associateBy { normalizeAssetName(it.name) }
        .values
        // Deleted rows are filtered out from the current snapshot.
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

    // Normalize names before comparisons so copy/dedup logic is resilient to
    // casing and surrounding whitespace.
    private fun normalizeAssetName(value: String): String = value.trim().lowercase()
}
