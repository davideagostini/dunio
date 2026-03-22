package com.davideagostini.summ.ui.settings.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.data.entity.RecurringTransaction
import com.davideagostini.summ.data.repository.CategoryRepository
import com.davideagostini.summ.data.repository.EntryRepository
import com.davideagostini.summ.data.repository.RecurringTransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    recurringRepository: RecurringTransactionRepository,
    categoryRepository: CategoryRepository,
    entryRepository: EntryRepository,
) : ViewModel() {
    private val recurringLoaded = MutableStateFlow(false)
    private val categoriesLoaded = MutableStateFlow(false)
    private val entriesLoaded = MutableStateFlow(false)

    val recurringTransactions: StateFlow<List<RecurringTransaction>> = recurringRepository.allRecurringTransactions
        .onEach { recurringLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .onEach { categoriesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val entries: StateFlow<List<Entry>> = entryRepository.allEntries
        .onEach { entriesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoading: StateFlow<Boolean> = combine(recurringLoaded, categoriesLoaded) { recurringReady, categoriesReady ->
        !recurringReady || !categoriesReady
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    private val recurringRepositoryRef = recurringRepository

    fun handleEvent(event: RecurringEvent) {
        when (event) {
            RecurringEvent.ToggleSearch -> _uiState.update { it.copy(searchVisible = !it.searchVisible) }
            is RecurringEvent.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = event.value) }
            RecurringEvent.StartAdd -> _uiState.update {
                it.copy(
                    sheetMode = RecurringSheetMode.Add,
                    selectedRecurring = null,
                    description = "",
                    amount = "",
                    type = "expense",
                    category = "",
                    dayOfMonth = "1",
                    startDate = System.currentTimeMillis(),
                    active = true,
                    descriptionError = null,
                    amountError = null,
                    dayError = null,
                )
            }
            is RecurringEvent.Select -> _uiState.update { it.copy(sheetMode = RecurringSheetMode.Action, selectedRecurring = event.recurring) }
            RecurringEvent.StartEdit -> {
                val recurring = _uiState.value.selectedRecurring ?: return
                _uiState.update {
                    it.copy(
                        sheetMode = RecurringSheetMode.Edit,
                        description = recurring.description,
                        amount = recurring.amount.toString(),
                        type = recurring.type,
                        category = recurring.category,
                        dayOfMonth = recurring.dayOfMonth.toString(),
                        startDate = recurring.startDate.toEpochMillis(),
                        active = recurring.active,
                        descriptionError = null,
                        amountError = null,
                        dayError = null,
                    )
                }
            }
            RecurringEvent.DismissSheet -> _uiState.update {
                RecurringUiState(searchVisible = it.searchVisible, searchQuery = it.searchQuery)
            }
            RecurringEvent.RequestDelete -> _uiState.update { it.copy(showDeleteDialog = true) }
            RecurringEvent.DismissDeleteDialog -> _uiState.update { it.copy(showDeleteDialog = false) }
            RecurringEvent.ConfirmDelete -> confirmDelete()
            RecurringEvent.ApplyDue -> applyDue()
            is RecurringEvent.UpdateDescription -> _uiState.update { it.copy(description = event.value, descriptionError = null) }
            is RecurringEvent.UpdateAmount -> _uiState.update { it.copy(amount = event.value, amountError = null) }
            is RecurringEvent.UpdateType -> _uiState.update { it.copy(type = event.value) }
            is RecurringEvent.UpdateCategory -> _uiState.update { it.copy(category = event.value) }
            is RecurringEvent.UpdateDayOfMonth -> _uiState.update { it.copy(dayOfMonth = event.value, dayError = null) }
            is RecurringEvent.UpdateStartDate -> _uiState.update { it.copy(startDate = event.value) }
            is RecurringEvent.UpdateActive -> _uiState.update { it.copy(active = event.value) }
            RecurringEvent.SaveAdd -> saveAdd()
            RecurringEvent.SaveEdit -> saveEdit()
        }
    }

    private fun saveAdd() {
        val recurring = buildRecurring() ?: return
        viewModelScope.launch {
            recurringRepositoryRef.insert(recurring)
            _uiState.update { RecurringUiState(searchVisible = it.searchVisible, searchQuery = it.searchQuery) }
        }
    }

    private fun saveEdit() {
        val selected = _uiState.value.selectedRecurring ?: return
        val recurring = buildRecurring(id = selected.id, lastAppliedDate = selected.lastAppliedDate) ?: return
        viewModelScope.launch {
            recurringRepositoryRef.update(recurring)
            _uiState.update { RecurringUiState(searchVisible = it.searchVisible, searchQuery = it.searchQuery) }
        }
    }

    private fun confirmDelete() {
        val id = _uiState.value.selectedRecurring?.id ?: return
        viewModelScope.launch {
            recurringRepositoryRef.delete(id)
            _uiState.update { RecurringUiState(searchVisible = it.searchVisible, searchQuery = it.searchQuery) }
        }
    }

    private fun applyDue() {
        viewModelScope.launch {
            recurringRepositoryRef.applyDueRecurringTransactions(recurringTransactions.value, entries.value)
        }
    }

    private fun buildRecurring(id: String = "", lastAppliedDate: String? = null): RecurringTransaction? {
        val state = _uiState.value
        val amount = state.amount.replace(',', '.').toDoubleOrNull()
        val day = state.dayOfMonth.toIntOrNull()
        var valid = true

        if (state.description.isBlank()) {
            _uiState.update { it.copy(descriptionError = context.getString(R.string.recurring_validation_description)) }
            valid = false
        }
        if (amount == null) {
            _uiState.update { it.copy(amountError = context.getString(R.string.recurring_validation_amount)) }
            valid = false
        }
        if (day == null || day !in 1..31) {
            _uiState.update { it.copy(dayError = context.getString(R.string.recurring_validation_day)) }
            valid = false
        }
        if (!valid || amount == null || day == null) return null

        return RecurringTransaction(
            id = id,
            description = state.description.trim(),
            amount = amount,
            type = state.type,
            category = state.category,
            dayOfMonth = day,
            startDate = state.startDate.toLocalDateString(),
            active = state.active,
            lastAppliedDate = lastAppliedDate,
        )
    }
}

private fun String.toEpochMillis(): Long =
    LocalDate.parse(this)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

private fun Long.toLocalDateString(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
