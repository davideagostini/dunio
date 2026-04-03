package com.davideagostini.summ.ui.entries

// EntriesViewModel owns the data and mutation logic for the entries feature: it loads only the
// selected month for the main list while keeping a small rolling window for insights.
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
import com.davideagostini.summ.domain.model.EntryDisplayItem
import com.davideagostini.summ.ui.components.buildRecentMonthOptions
import com.davideagostini.summ.ui.components.preferredRecentMonth
import com.davideagostini.summ.ui.format.DEFAULT_CURRENCY
import com.davideagostini.summ.ui.format.formatEditableAmount
import com.davideagostini.summ.ui.format.parseAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
class EntriesViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val entryRepository: EntryRepository,
    private val categoryRepository: CategoryRepository,
    sessionRepository: SessionRepository,
    monthCloseRepository: MonthCloseRepository,
) : ViewModel() {
    private val sharingStarted = SharingStarted.WhileSubscribed(30_000)
    private val defaultSelectedMonth = preferredRecentMonth(buildRecentMonthOptions())
    private val entriesLoaded = MutableStateFlow(false)
    private val insightsLoaded = MutableStateFlow(false)
    private val hasAnyEntriesLoaded = MutableStateFlow(false)
    private val categoriesLoaded = MutableStateFlow(false)
    private val monthClosesLoaded = MutableStateFlow(false)

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .onEach { categoriesLoaded.value = true }
        .stateIn(viewModelScope, sharingStarted, emptyList())

    private val monthCloses: StateFlow<List<MonthClose>> = monthCloseRepository.allMonthCloses
        .onEach { monthClosesLoaded.value = true }
        .stateIn(viewModelScope, sharingStarted, emptyList())

    private val householdCurrency: StateFlow<String> = sessionRepository.householdCurrency
        .stateIn(viewModelScope, sharingStarted, DEFAULT_CURRENCY)

    private val _uiState = MutableStateFlow(EntriesUiState())
    val uiState: StateFlow<EntriesUiState> = _uiState.asStateFlow()

    private val selectedMonth: StateFlow<String> = uiState
        .map { state -> state.selectedMonth ?: defaultSelectedMonth }
        .stateIn(
            viewModelScope,
            sharingStarted,
            defaultSelectedMonth,
        )

    private val monthEntries: StateFlow<List<EntryDisplayItem>> = combine(selectedMonth, categories) { month, categories ->
        month to categories
    }.flatMapLatest { (month, categories) ->
        entryRepository.observeEntriesForMonth(month).mapToDisplayItems(categories)
    }.onEach { entriesLoaded.value = true }
        .stateIn(viewModelScope, sharingStarted, emptyList())

    private val insightEntries: StateFlow<List<EntryDisplayItem>> = combine(selectedMonth, categories) { month, categories ->
        val startDate = YearMonth.parse(month).minusMonths(3).atDay(1).toString()
        val endExclusiveDate = YearMonth.parse(month).plusMonths(1).atDay(1).toString()
        Triple(startDate, endExclusiveDate, categories)
    }.flatMapLatest { (startDate, endExclusiveDate, categories) ->
        entryRepository.observeEntriesBetween(startDate, endExclusiveDate).mapToDisplayItems(categories)
    }.onEach { insightsLoaded.value = true }
        .stateIn(viewModelScope, sharingStarted, emptyList())

    private val hasAnyEntries: StateFlow<Boolean> = entryRepository.observeHasAnyEntries()
        .onEach { hasAnyEntriesLoaded.value = true }
        .stateIn(viewModelScope, sharingStarted, false)

    val isLoading: StateFlow<Boolean> = combine(
        entriesLoaded,
        insightsLoaded,
        hasAnyEntriesLoaded,
        categoriesLoaded,
        monthClosesLoaded,
    ) { entriesReady, insightsReady, hasAnyEntriesReady, categoriesReady, monthClosesReady ->
        !entriesReady || !insightsReady || !hasAnyEntriesReady || !categoriesReady || !monthClosesReady
    }.stateIn(viewModelScope, sharingStarted, true)

    private data class EntriesSourceState(
        val selectedMonth: String,
        val monthEntries: List<EntryDisplayItem>,
        val insightEntries: List<EntryDisplayItem>,
    )

    val renderState: StateFlow<EntriesRenderState> = combine(
        combine(selectedMonth, monthEntries, insightEntries) { selectedMonth, monthEntries, insightEntries ->
            EntriesSourceState(
                selectedMonth = selectedMonth,
                monthEntries = monthEntries,
                insightEntries = insightEntries,
            )
        },
        monthCloses,
        householdCurrency,
        uiState,
        hasAnyEntries,
    ) { source, closes, currency, state, hasAnyEntries ->
        val visibleEntries = source.monthEntries.filter { entry ->
            matchesFilter(entry, state.filterType) && matchesSearch(entry, state.searchQuery)
        }
        val categorySpendingBreakdown = buildCategorySpendingBreakdown(
            entries = source.monthEntries,
            uncategorizedLabel = appContext.getString(R.string.entries_reports_uncategorized),
        )

        EntriesRenderState(
            selectedMonth = source.selectedMonth,
            householdCurrency = currency,
            isMonthClosed = closes.any { it.period == source.selectedMonth && it.status == "closed" },
            monthEntries = source.monthEntries,
            visibleEntries = visibleEntries,
            dayGroups = buildDayGroups(visibleEntries),
            unusualSpendingInsights = buildUnusualSpendingInsights(source.insightEntries, source.selectedMonth),
            categorySpendingBreakdown = categorySpendingBreakdown,
            categorySpendingTotal = categorySpendingBreakdown.sumOf(CategorySpendingBreakdownItem::totalAmount),
            categorySpendingTransactionCount = categorySpendingBreakdown.sumOf(CategorySpendingBreakdownItem::transactionCount),
            totalExpenses = source.monthEntries.sumOf { entry -> if (entry.type == "expense") entry.price else 0.0 },
            totalIncome = source.monthEntries.sumOf { entry -> if (entry.type == "income") entry.price else 0.0 },
            monthLabel = formatMonthLabel(source.selectedMonth),
            hasAnyEntries = hasAnyEntries,
        )
    }.stateIn(
        viewModelScope,
        sharingStarted,
        EntriesRenderState(
            selectedMonth = defaultSelectedMonth,
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
            monthLabel = formatMonthLabel(defaultSelectedMonth),
            hasAnyEntries = false,
        ),
    )

    fun handleEvent(event: EntriesEvent) {
        when (event) {
            is EntriesEvent.SelectMonth -> _uiState.update { it.copy(selectedMonth = event.monthKey) }
            is EntriesEvent.SelectFilter -> _uiState.update { it.copy(filterType = event.filter) }
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
            EntriesEvent.ToggleSearch -> _uiState.update {
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
            is EntriesEvent.Select -> _uiState.update {
                it.copy(sheetMode = EntrySheetMode.Action, selectedEntry = event.entry)
            }
            EntriesEvent.StartEdit -> {
                val entry = _uiState.value.selectedEntry ?: return
                val cat = categories.value.firstOrNull { it.name == entry.category }
                _uiState.update {
                    it.copy(
                        sheetMode = EntrySheetMode.Edit,
                        editType = entry.type,
                        editDescription = entry.description,
                        editPrice = formatEditableAmount(entry.price),
                        editDate = entry.date,
                        editCategory = cat,
                        descriptionError = null,
                        priceError = null,
                        operationErrorMessage = null,
                        isSaving = false,
                    )
                }
            }
            EntriesEvent.RequestDelete -> _uiState.update { it.copy(showDeleteDialog = true, operationErrorMessage = null) }
            EntriesEvent.DismissSheet -> _uiState.update { it.clearTransientState() }
            is EntriesEvent.UpdateType -> _uiState.update { it.copy(editType = event.value, operationErrorMessage = null) }
            is EntriesEvent.UpdateDescription -> _uiState.update { it.copy(editDescription = event.value, descriptionError = null, operationErrorMessage = null) }
            is EntriesEvent.UpdatePrice -> _uiState.update { it.copy(editPrice = event.value, priceError = null, operationErrorMessage = null) }
            is EntriesEvent.UpdateDate -> _uiState.update { it.copy(editDate = event.value, operationErrorMessage = null) }
            is EntriesEvent.UpdateCategory -> _uiState.update { it.copy(editCategory = event.category, operationErrorMessage = null) }
            EntriesEvent.SaveEdit -> saveEdit()
            EntriesEvent.ConfirmDelete -> confirmDelete()
            EntriesEvent.DismissDeleteDialog -> _uiState.update { it.copy(showDeleteDialog = false) }
        }
    }

    private fun saveEdit() {
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

        _uiState.update { it.copy(isSaving = true, operationErrorMessage = null) }
        viewModelScope.launch {
            try {
                entryRepository.update(
                    Entry(
                        id = entry.id,
                        type = state.editType,
                        description = state.editDescription.trim(),
                        price = price!!,
                        category = state.editCategory?.name ?: entry.category,
                        categoryKey = state.editCategory?.systemKey ?: entry.categoryKey,
                        date = state.editDate,
                    ),
                )
                _uiState.update { it.copy(sheetMode = EntrySheetMode.Success, operationErrorMessage = null) }
                delay(1_500L)
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
        val entry = _uiState.value.selectedEntry ?: return
        viewModelScope.launch {
            try {
                entryRepository.delete(
                    Entry(
                        id = entry.id,
                        type = entry.type,
                        description = entry.description,
                        price = entry.price,
                        category = entry.category,
                        categoryKey = entry.categoryKey,
                        date = entry.date,
                    ),
                )
                _uiState.update { it.copy(sheetMode = EntrySheetMode.Success, showDeleteDialog = false, operationErrorMessage = null) }
                delay(1_500L)
                _uiState.update { it.clearTransientState() }
            } catch (throwable: Throwable) {
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

    private fun Flow<List<Entry>>.mapToDisplayItems(categories: List<Category>): Flow<List<EntryDisplayItem>> =
        map { entries -> entries.toDisplayItems(categories) }

    private fun List<Entry>.toDisplayItems(categories: List<Category>): List<EntryDisplayItem> {
        val categoriesBySystemKey = categories.mapNotNull { category ->
            category.systemKey?.let { systemKey -> systemKey to category }
        }.toMap()
        val categoriesByName = categories.associateBy { it.name }

        return map { entry ->
            val matchedCategory =
                entry.categoryKey?.let(categoriesBySystemKey::get)
                    ?: categoriesByName[entry.category]
            EntryDisplayItem(
                id = entry.id,
                type = entry.type,
                description = entry.description,
                price = entry.price,
                category = matchedCategory?.name ?: entry.category,
                categoryKey = entry.categoryKey,
                emoji = matchedCategory?.emoji ?: "📦",
                date = entry.date,
            )
        }
    }
}
