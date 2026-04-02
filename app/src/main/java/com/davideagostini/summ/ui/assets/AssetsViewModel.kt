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
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.ui.format.DEFAULT_CURRENCY
import com.davideagostini.summ.ui.format.formatEditableAmount
import com.davideagostini.summ.ui.format.parseAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AssetsViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val repository: AssetRepository,
    sessionRepository: SessionRepository,
    monthCloseRepository: MonthCloseRepository,
) : ViewModel() {
    private val defaultSelectedMonth = YearMonth.now().toString()
    private val _uiState = MutableStateFlow(AssetsUiState())
    val uiState: StateFlow<AssetsUiState> = _uiState.asStateFlow()

    // These flags gate the initial loading state until each data source has
    // emitted at least once.
    private val currentMonthHistoryLoaded = MutableStateFlow(false)
    private val previousMonthHistoryLoaded = MutableStateFlow(false)
    private val hasAnyAssetsLoaded = MutableStateFlow(false)
    private val monthClosesLoaded = MutableStateFlow(false)

    private val selectedMonth: StateFlow<String> = uiState
        .map { state -> state.selectedMonth ?: defaultSelectedMonth }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), defaultSelectedMonth)

    private val currentMonthAssetHistory: StateFlow<List<AssetHistoryEntry>> = selectedMonth
        .flatMapLatest { month -> repository.observeAssetHistoryForMonth(month) }
        .onEach { currentMonthHistoryLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val previousMonthAssetHistory: StateFlow<List<AssetHistoryEntry>> = selectedMonth
        .map { month -> YearMonth.parse(month).minusMonths(1).toString() }
        .flatMapLatest { month -> repository.observeAssetHistoryForMonth(month) }
        .onEach { previousMonthHistoryLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val hasAnyAssets: StateFlow<Boolean> = repository.observeHasAnyAssetHistory()
        .onEach { hasAnyAssetsLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val monthCloses: StateFlow<List<MonthClose>> = monthCloseRepository.allMonthCloses
        .onEach { monthClosesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val householdCurrency: StateFlow<String> = sessionRepository.householdCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_CURRENCY)

    // Loading remains true until both streams have emitted at least once so the
    // screen can initialize with complete data.
    val isLoading: StateFlow<Boolean> = combine(
        currentMonthHistoryLoaded,
        previousMonthHistoryLoaded,
        hasAnyAssetsLoaded,
        monthClosesLoaded,
    ) { currentReady, previousReady, hasAnyReady, monthClosesReady ->
        !currentReady || !previousReady || !hasAnyReady || !monthClosesReady
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private data class AssetHistoryWindow(
        val selectedMonth: String,
        val currentHistory: List<AssetHistoryEntry>,
        val previousHistory: List<AssetHistoryEntry>,
    )

    val renderState: StateFlow<AssetsRenderState> = combine(
        combine(selectedMonth, currentMonthAssetHistory, previousMonthAssetHistory) { selectedMonth, currentHistory, previousHistory ->
            AssetHistoryWindow(
                selectedMonth = selectedMonth,
                currentHistory = currentHistory,
                previousHistory = previousHistory,
            )
        },
        monthCloses,
        householdCurrency,
        uiState,
        hasAnyAssets,
    ) { window, monthCloses, householdCurrency, uiState, hasAnyAssets ->
        val selectedMonth = window.selectedMonth
        val isMonthClosed = monthCloses.any { it.period == selectedMonth && it.status == "closed" }
        val monthAssets = buildAssetsSnapshotForMonth(window.currentHistory, selectedMonth)
        val filteredAssets = monthAssets
            .filter { asset ->
                val query = uiState.searchQuery.trim()
                query.isBlank() ||
                    asset.name.contains(query, ignoreCase = true) ||
                    asset.category.contains(query, ignoreCase = true)
            }
            .map { asset ->
                AssetListItem(
                    asset = asset,
                    change = calculateAssetChange(window.currentHistory, window.previousHistory, asset.name, selectedMonth),
                )
            }
        val totalAssets = monthAssets.filter { it.type == "asset" }.sumOf { it.value }
        val totalLiabilities = monthAssets.filter { it.type == "liability" }.sumOf { it.value }
        val previousMonthAssets = buildAssetsSnapshotForMonth(
            window.previousHistory,
            YearMonth.parse(selectedMonth).minusMonths(1).toString(),
        )
        val currentNames = monthAssets.mapTo(mutableSetOf()) { normalizeAssetName(it.name) }

        AssetsRenderState(
            selectedMonth = selectedMonth,
            householdCurrency = householdCurrency,
            isMonthClosed = isMonthClosed,
            filteredAssets = filteredAssets,
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            netWorth = totalAssets - totalLiabilities,
            canCopyPreviousMonth = previousMonthAssets.any { normalizeAssetName(it.name) !in currentNames },
            hasAnyAssets = monthAssets.isNotEmpty() || hasAnyAssets,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AssetsRenderState(
            selectedMonth = defaultSelectedMonth,
            householdCurrency = DEFAULT_CURRENCY,
            isMonthClosed = false,
            filteredAssets = emptyList(),
            totalAssets = 0.0,
            totalLiabilities = 0.0,
            netWorth = 0.0,
            canCopyPreviousMonth = false,
            hasAnyAssets = false,
        ),
    )

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
                        editValue = if (asset.value == 0.0) "" else formatEditableAmount(asset.value),
                        editType = asset.type,
                        nameError = null,
                        valueError = null,
                        operationErrorMessage = null,
                        isSaving = false,
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
        _uiState.update { it.copy(isSaving = true, operationErrorMessage = null) }
        viewModelScope.launch {
            try {
                repository.insert(
                    Asset(
                        name = state.editName.trim(),
                        category = state.editCategory.trim(),
                        value = value,
                        currency = renderState.value.householdCurrency,
                        type = state.editType,
                        period = selectedMonthOrDefault(state),
                        snapshotDate = monthToSnapshotDate(selectedMonthOrDefault(state)),
                    )
                )
                showSuccess()
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        operationErrorMessage = throwable.toFirestoreUserMessage(appContext),
                    )
                }
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
        _uiState.update { it.copy(isSaving = true, operationErrorMessage = null) }
        viewModelScope.launch {
            try {
                repository.update(
                    current.copy(
                        name = state.editName.trim(),
                        category = state.editCategory.trim(),
                        value = value,
                        currency = renderState.value.householdCurrency,
                        type = state.editType,
                        period = selectedMonthOrDefault(state),
                        snapshotDate = monthToSnapshotDate(selectedMonthOrDefault(state)),
                    )
                )
                showSuccess()
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        operationErrorMessage = throwable.toFirestoreUserMessage(appContext),
                    )
                }
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
        _uiState.update { it.copy(sheetMode = AssetSheetMode.Success, showDeleteDialog = false, operationErrorMessage = null, isSaving = false) }
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
        val value = parseAmount(state.editValue)
        if (value == null) {
            // Localized and formatted inputs are normalized before validation so
            // thousand separators and decimal commas are accepted where appropriate.
            _uiState.update { it.copy(valueError = appContext.getString(R.string.asset_validation_value_required)) }
            return null
        }
        return value
    }

    private fun monthToSnapshotDate(month: String?): String =
        // A monthly asset snapshot always points to the end of the selected month,
        // even when the caller only passes a `YearMonth` string.
        java.time.YearMonth.parse(month ?: defaultSelectedMonth)
            .atEndOfMonth()
            .toString()

    private fun copyPreviousMonth() {
        val selectedMonth = selectedMonthOrDefault(_uiState.value)
        val previousMonth = java.time.YearMonth.parse(selectedMonth).minusMonths(1).toString()
        val currentAssets = buildAssetsSnapshotForMonth(currentMonthAssetHistory.value, selectedMonth)
        val previousAssets = buildAssetsSnapshotForMonth(previousMonthAssetHistory.value, previousMonth)

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

    private fun selectedMonthOrDefault(state: AssetsUiState): String = state.selectedMonth ?: defaultSelectedMonth

}
