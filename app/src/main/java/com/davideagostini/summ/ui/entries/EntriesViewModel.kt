package com.davideagostini.summ.ui.entries

// EntriesViewModel owns the data and mutation logic for the entries feature: it merges household data,
// exposes a single immutable UI state, and translates UI events into repository operations.
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.data.entity.MonthClose
import com.davideagostini.summ.data.firebase.toFirestoreUserMessage
import com.davideagostini.summ.data.repository.CategoryRepository
import com.davideagostini.summ.data.repository.EntryRepository
import com.davideagostini.summ.data.repository.MonthCloseRepository
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.domain.model.HomeState
import com.davideagostini.summ.domain.usecase.GetHomeDataUseCase
import com.davideagostini.summ.ui.components.buildRecentMonthOptions
import com.davideagostini.summ.ui.components.preferredRecentMonth
import com.davideagostini.summ.ui.format.DEFAULT_CURRENCY
import com.davideagostini.summ.ui.format.formatEditableAmount
import com.davideagostini.summ.ui.format.parseAmount
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
class EntriesViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    getHomeData: GetHomeDataUseCase,
    private val entryRepository: EntryRepository,
    private val categoryRepository: CategoryRepository,
    sessionRepository: SessionRepository,
    monthCloseRepository: MonthCloseRepository,
) : ViewModel() {
    // These flags are used only to decide when the initial loading screen can disappear.
    private val homeLoaded = MutableStateFlow(false)
    private val categoriesLoaded = MutableStateFlow(false)
    private val monthClosesLoaded = MutableStateFlow(false)

    // Household entries are provided by the shared home use case and marked as loaded as soon as the first value arrives.
    private val homeState: StateFlow<HomeState> = getHomeData()
        .onEach { homeLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    // Categories are needed for entry editing, so they load together with the main entries stream.
    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .onEach { categoriesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Month close status determines read-only behavior for the current period.
    private val monthCloses: StateFlow<List<MonthClose>> = monthCloseRepository.allMonthCloses
        .onEach { monthClosesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val householdCurrency: StateFlow<String> = sessionRepository.householdCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_CURRENCY)

    // Loading ends only when every upstream source has emitted at least once.
    val isLoading: StateFlow<Boolean> = combine(homeLoaded, categoriesLoaded, monthClosesLoaded) { homeReady, categoriesReady, monthClosesReady ->
        !homeReady || !categoriesReady || !monthClosesReady
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // The UI state stays as a single immutable snapshot so the screen can render deterministically.
    private val _uiState = MutableStateFlow(EntriesUiState())
    val uiState: StateFlow<EntriesUiState> = _uiState.asStateFlow()

    val renderState: StateFlow<EntriesRenderState> = combine(homeState, monthCloses, householdCurrency, uiState) { home, closes, householdCurrency, state ->
        val monthOptions = buildRecentMonthOptions()
        val selectedMonth = state.selectedMonth ?: preferredRecentMonth(monthOptions)
        val monthEntries = home.entries.filter { entry -> monthKey(entry.date) == selectedMonth }
        val visibleEntries = monthEntries.filter { entry ->
            matchesFilter(entry, state.filterType) && matchesSearch(entry, state.searchQuery)
        }
        val categorySpendingBreakdown = buildCategorySpendingBreakdown(
            entries = monthEntries,
            uncategorizedLabel = appContext.getString(R.string.entries_reports_uncategorized),
        )

        EntriesRenderState(
            selectedMonth = selectedMonth,
            householdCurrency = householdCurrency,
            isMonthClosed = closes.any { it.period == selectedMonth && it.status == "closed" },
            monthEntries = monthEntries,
            visibleEntries = visibleEntries,
            dayGroups = buildDayGroups(visibleEntries),
            unusualSpendingInsights = buildUnusualSpendingInsights(home.entries, selectedMonth),
            categorySpendingBreakdown = categorySpendingBreakdown,
            categorySpendingTotal = categorySpendingBreakdown.sumOf(CategorySpendingBreakdownItem::totalAmount),
            categorySpendingTransactionCount = categorySpendingBreakdown.sumOf(CategorySpendingBreakdownItem::transactionCount),
            totalExpenses = monthEntries.sumOf { entry -> if (entry.type == "expense") entry.price else 0.0 },
            totalIncome = monthEntries.sumOf { entry -> if (entry.type == "income") entry.price else 0.0 },
            monthLabel = formatMonthLabel(selectedMonth),
            hasAnyEntries = home.entries.isNotEmpty(),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EntriesRenderState(
            selectedMonth = preferredRecentMonth(buildRecentMonthOptions()),
            householdCurrency = DEFAULT_CURRENCY,
            isMonthClosed = false,
            monthEntries = emptyList(),
            visibleEntries = emptyList(),
            dayGroups = emptyList(),
            unusualSpendingInsights = emptyList(),
            categorySpendingBreakdown = emptyList(),
            categorySpendingTotal = 0.0,
            categorySpendingTransactionCount = 0,
            totalExpenses = 0.0,
            totalIncome = 0.0,
            monthLabel = formatMonthLabel(preferredRecentMonth(buildRecentMonthOptions())),
            hasAnyEntries = false,
        ),
    )

    // All UI intents are funneled through one handler to keep the state machine explicit.
    fun handleEvent(event: EntriesEvent) {
        when (event) {
            // Filter and search updates are pure state mutations and do not hit the repository layer.
            is EntriesEvent.SelectMonth    -> _uiState.update { it.copy(selectedMonth = event.monthKey) }
            is EntriesEvent.SelectFilter   -> _uiState.update { it.copy(filterType = event.filter) }
            EntriesEvent.ToggleContentMode -> _uiState.update {
                val nextMode = if (it.contentMode == EntriesContentMode.Entries) {
                    EntriesContentMode.Reports
                } else {
                    EntriesContentMode.Entries
                }
                it.copy(
                    contentMode = nextMode,
                    searchVisible = if (nextMode == EntriesContentMode.Reports) false else it.searchVisible,
                )
            }
            EntriesEvent.ToggleSearch      -> _uiState.update {
                if (it.contentMode == EntriesContentMode.Reports) {
                    it.copy(
                        contentMode = EntriesContentMode.Entries,
                        searchVisible = true,
                    )
                } else {
                    it.copy(
                        searchVisible = !it.searchVisible,
                        searchQuery = if (it.searchVisible) "" else it.searchQuery,
                    )
                }
            }
            is EntriesEvent.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = event.query) }
            is EntriesEvent.Select         -> _uiState.update {
                it.copy(sheetMode = EntrySheetMode.Action, selectedEntry = event.entry)
            }
            EntriesEvent.StartEdit         -> {
                // Editing pre-fills the form from the currently selected entry and resolves the category object.
                val entry = _uiState.value.selectedEntry ?: return
                val cat   = categories.value.firstOrNull { it.name == entry.category }
                _uiState.update {
                    it.copy(
                        sheetMode        = EntrySheetMode.Edit,
                        editType         = entry.type,
                        editDescription  = entry.description,
                        editPrice        = formatEditableAmount(entry.price),
                        editDate         = entry.date,
                        editCategory     = cat,
                        descriptionError = null,
                        priceError       = null,
                        operationErrorMessage = null,
                        isSaving         = false,
                    )
                }
            }
            // Delete opens a separate confirmation dialog before any repository write happens.
            EntriesEvent.RequestDelete     -> _uiState.update { it.copy(showDeleteDialog = true, operationErrorMessage = null) }
            EntriesEvent.DismissSheet      -> _uiState.update { it.clearTransientState() }
            is EntriesEvent.UpdateType         -> _uiState.update { it.copy(editType = event.value, operationErrorMessage = null) }
            is EntriesEvent.UpdateDescription  -> _uiState.update { it.copy(editDescription = event.value, descriptionError = null, operationErrorMessage = null) }
            is EntriesEvent.UpdatePrice        -> _uiState.update { it.copy(editPrice = event.value, priceError = null, operationErrorMessage = null) }
            is EntriesEvent.UpdateDate         -> _uiState.update { it.copy(editDate = event.value, operationErrorMessage = null) }
            is EntriesEvent.UpdateCategory     -> _uiState.update { it.copy(editCategory = event.category, operationErrorMessage = null) }
            EntriesEvent.SaveEdit          -> saveEdit()
            EntriesEvent.ConfirmDelete     -> confirmDelete()
            EntriesEvent.DismissDeleteDialog -> _uiState.update { it.copy(showDeleteDialog = false) }
        }
    }

    private fun saveEdit() {
        // Validation runs before the Firestore write so the screen can show field-level feedback immediately.
        val state = _uiState.value
        val entry = state.selectedEntry ?: return
        var hasError = false

        if (state.editDescription.isBlank()) {
            _uiState.update { it.copy(descriptionError = appContext.getString(R.string.entries_validation_description_required)) }
            hasError = true
        }
        val price = parseAmount(state.editPrice)
        if (price == null || price <= 0) {
            _uiState.update { it.copy(priceError = appContext.getString(R.string.entries_validation_amount_valid)) }
            hasError = true
        }
        if (hasError) return

        // Edit is a Firestore write, so we surface backend permission problems inline in the sheet.
        _uiState.update { it.copy(isSaving = true, operationErrorMessage = null) }
        viewModelScope.launch {
            try {
                entryRepository.update(
                    Entry(
                        id          = entry.id,
                        type        = state.editType,
                        description = state.editDescription.trim(),
                        price       = price!!,
                        category    = state.editCategory?.name ?: entry.category,
                        categoryKey = state.editCategory?.systemKey ?: entry.categoryKey,
                        date        = state.editDate,
                    )
                )
                _uiState.update { it.copy(sheetMode = EntrySheetMode.Success, operationErrorMessage = null) }
                // Keep the success state visible long enough for the user to confirm the write completed.
                delay(1_500L)
                // After the success toast-equivalent window, return to the neutral screen state.
                _uiState.update { it.clearTransientState() }
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
        // Keep a stable snapshot of the entry to delete so the repository call is immune to later UI changes.
        val entry = _uiState.value.selectedEntry ?: return

        // Delete errors must stay in the entry action flow instead of crashing the coroutine on Main.
        viewModelScope.launch {
            try {
                entryRepository.delete(
                    Entry(
                        id          = entry.id,
                        type        = entry.type,
                        description = entry.description,
                        price       = entry.price,
                        category    = entry.category,
                        categoryKey = entry.categoryKey,
                        date        = entry.date,
                    )
                )
                _uiState.update { it.copy(sheetMode = EntrySheetMode.Success, showDeleteDialog = false, operationErrorMessage = null) }
                // Keep the delete success state visible for the same duration as the edit success path.
                delay(1_500L)
                // Delete success also resets the transient flow back to the neutral screen state.
                _uiState.update { it.clearTransientState() }
            } catch (throwable: Throwable) {
                // Failed deletes return to the action flow and surface the translated backend message.
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        showDeleteDialog = false,
                        operationErrorMessage = throwable.toFirestoreUserMessage(appContext),
                    )
                }
            }
        }
    }

    private fun EntriesUiState.clearTransientState(): EntriesUiState =
        // Reset everything that belongs to the transient sheet flow while preserving the month/search context.
        copy(
            sheetMode = EntrySheetMode.Hidden,
            selectedEntry = null,
            editType = "expense",
            editDescription = "",
            editPrice = "",
            editDate = System.currentTimeMillis(),
            editCategory = null,
            descriptionError = null,
            priceError = null,
            operationErrorMessage = null,
            isSaving = false,
            showDeleteDialog = false,
        )
}
