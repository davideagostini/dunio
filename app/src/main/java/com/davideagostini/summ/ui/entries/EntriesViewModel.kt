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
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
/**
 * ViewModel for the transactions screen.
 *
 * It loads the selected month for the primary list, keeps a short rolling window for spending
 * insights, and exposes a render state tailored for the screen composables.
 */
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
    private val hasAnyEntriesLoaded = MutableStateFlow(false)
    private val categoriesLoaded = MutableStateFlow(false)
    private val monthClosesLoaded = MutableStateFlow(false)
    private val pendingMonthRefresh = MutableStateFlow<String?>(null)

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

    val isMonthRefreshing: StateFlow<Boolean> = pendingMonthRefresh
        .map { pendingMonth -> pendingMonth != null }
        .stateIn(viewModelScope, sharingStarted, false)

    private val monthEntries: StateFlow<List<EntryDisplayItem>> = combine(selectedMonth, categories) { month, categories ->
        month to categories
    }.flatMapLatest { (month, categories) ->
        entryRepository.observeEntriesForMonth(month)
            .mapToDisplayItems(categories)
            .map { entries -> month to entries }
    }.onEach { (month, _) ->
        entriesLoaded.value = true
        if (pendingMonthRefresh.value == month) {
            pendingMonthRefresh.value = null
        }
    }.map { (_, entries) -> entries }
        .stateIn(viewModelScope, sharingStarted, emptyList())

    private val insightEntries: StateFlow<List<EntryDisplayItem>> = combine(
        entriesLoaded,
        selectedMonth,
        categories,
    ) { monthEntriesReady, month, categories ->
        if (!monthEntriesReady) {
            null
        } else {
            val startDate = YearMonth.parse(month).minusMonths(3).atDay(1).toString()
            val endExclusiveDate = YearMonth.parse(month).plusMonths(1).atDay(1).toString()
            Triple(startDate, endExclusiveDate, categories)
        }
    }.flatMapLatest { request ->
        if (request == null) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            val (startDate, endExclusiveDate, categories) = request
            entryRepository.observeEntriesBetween(startDate, endExclusiveDate).mapToDisplayItems(categories)
        }
    }.stateIn(viewModelScope, sharingStarted, emptyList())

    private val hasAnyEntries: StateFlow<Boolean> = entryRepository.observeHasAnyEntries()
        .onEach { hasAnyEntriesLoaded.value = true }
        .stateIn(viewModelScope, sharingStarted, false)

    val isLoading: StateFlow<Boolean> = combine(
        entriesLoaded,
        hasAnyEntriesLoaded,
        categoriesLoaded,
        monthClosesLoaded,
    ) { entriesReady, hasAnyEntriesReady, categoriesReady, monthClosesReady ->
        !entriesReady || !hasAnyEntriesReady || !categoriesReady || !monthClosesReady
    }.stateIn(viewModelScope, sharingStarted, true)

    private data class EntriesSourceState(
        val selectedMonth: String,
        val monthEntries: List<EntryDisplayItem>,
        val insightEntries: List<EntryDisplayItem>,
    )

    private data class EntriesRenderInputs(
        val source: EntriesSourceState,
        val closes: List<MonthClose>,
        val currency: String,
        val state: EntriesUiState,
        val hasAnyEntries: Boolean,
        val uncategorizedLabel: String,
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
        EntriesRenderInputs(
            source = source,
            closes = closes,
            currency = currency,
            state = state,
            hasAnyEntries = hasAnyEntries,
            uncategorizedLabel = appContext.getString(R.string.entries_reports_uncategorized),
        )
    }.mapLatest { inputs ->
        withContext(Dispatchers.Default) {
            val visibleEntries = inputs.source.monthEntries.filter { entry ->
                matchesFilter(entry, inputs.state.filterType) &&
                    matchesSearch(entry, inputs.state.searchQuery)
            }
            val categorySpendingBreakdown = if (inputs.state.contentMode == EntriesContentMode.Reports) {
                buildCategorySpendingBreakdown(
                    entries = inputs.source.monthEntries,
                    uncategorizedLabel = inputs.uncategorizedLabel,
                )
            } else {
                emptyList()
            }
            val unusualSpendingInsights = if (inputs.state.contentMode == EntriesContentMode.Entries) {
                buildUnusualSpendingInsights(inputs.source.insightEntries, inputs.source.selectedMonth)
            } else {
                emptyList()
            }
            val dayGroups = buildDayGroups(visibleEntries)

            EntriesRenderState(
                selectedMonth = inputs.source.selectedMonth,
                householdCurrency = inputs.currency,
                isMonthClosed = inputs.closes.any { close ->
                    close.period == inputs.source.selectedMonth && close.status == "closed"
                },
                monthEntries = inputs.source.monthEntries,
                visibleEntries = visibleEntries,
                listItems = buildEntriesListItems(dayGroups),
                dayGroups = dayGroups,
                unusualSpendingInsights = unusualSpendingInsights,
                categorySpendingBreakdown = categorySpendingBreakdown,
                categorySpendingTotal = categorySpendingBreakdown.sumOf(CategorySpendingBreakdownItem::totalAmount),
                categorySpendingTransactionCount = categorySpendingBreakdown.sumOf(CategorySpendingBreakdownItem::transactionCount),
                totalExpenses = inputs.source.monthEntries.sumOf { entry ->
                    if (entry.type == "expense") entry.price else 0.0
                },
                totalIncome = inputs.source.monthEntries.sumOf { entry ->
                    if (entry.type == "income") entry.price else 0.0
                },
                monthLabel = formatMonthLabel(inputs.source.selectedMonth),
                hasAnyEntries = inputs.hasAnyEntries,
            )
        }
    }.stateIn(
        viewModelScope,
        sharingStarted,
        EntriesRenderState(
            selectedMonth = defaultSelectedMonth,
            householdCurrency = DEFAULT_CURRENCY,
            isMonthClosed = false,
            monthEntries = emptyList(),
            visibleEntries = emptyList(),
            listItems = emptyList(),
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
            is EntriesEvent.SelectMonth -> {
                val currentMonth = selectedMonth.value
                if (event.monthKey != currentMonth) {
                    pendingMonthRefresh.value = event.monthKey
                }
                _uiState.update { it.copy(selectedMonth = event.monthKey) }
            }
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
        mapLatest { entries ->
            withContext(Dispatchers.Default) {
                entries.toDisplayItems(categories)
            }
        }

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
