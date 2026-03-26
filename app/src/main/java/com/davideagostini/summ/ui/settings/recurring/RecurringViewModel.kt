package com.davideagostini.summ.ui.settings.recurring

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.RecurringTransaction
import com.davideagostini.summ.data.firebase.toFirestoreUserMessage
import com.davideagostini.summ.data.repository.CategoryRepository
import com.davideagostini.summ.data.repository.RecurringTransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @param:ApplicationContext private val context: Context,
    recurringRepository: RecurringTransactionRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {
    private val recurringLoaded = MutableStateFlow(false)
    private val categoriesLoaded = MutableStateFlow(false)

    private val recurringTransactions: StateFlow<List<RecurringTransaction>> = recurringRepository.allRecurringTransactions
        .onEach { recurringLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .onEach { categoriesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoading: StateFlow<Boolean> = combine(recurringLoaded, categoriesLoaded) { recurringReady, categoriesReady ->
        !recurringReady || !categoriesReady
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    val renderState: StateFlow<RecurringRenderState> = combine(recurringTransactions, uiState) { recurring, uiState ->
        val query = uiState.searchQuery.trim()
        RecurringRenderState(
            filteredRecurring = recurring.filter {
                query.isBlank() ||
                    listOf(it.description, it.category, it.type).joinToString(" ")
                        .contains(query, ignoreCase = true)
            },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        RecurringRenderState(filteredRecurring = emptyList()),
    )

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
                    operationErrorMessage = null,
                )
            }
            is RecurringEvent.Select -> _uiState.update {
                it.copy(
                    sheetMode = RecurringSheetMode.Action,
                    selectedRecurring = event.recurring,
                    operationErrorMessage = null,
                )
            }
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
                        operationErrorMessage = null,
                    )
                }
            }
            RecurringEvent.DismissSheet -> _uiState.update {
                RecurringUiState(searchVisible = it.searchVisible, searchQuery = it.searchQuery)
            }
            RecurringEvent.RequestDelete -> _uiState.update { it.copy(showDeleteDialog = true, operationErrorMessage = null) }
            RecurringEvent.DismissDeleteDialog -> _uiState.update { it.copy(showDeleteDialog = false) }
            RecurringEvent.ConfirmDelete -> confirmDelete()
            RecurringEvent.ApplyDue -> applyDue()
            is RecurringEvent.UpdateDescription -> _uiState.update { it.copy(description = event.value, descriptionError = null, operationErrorMessage = null) }
            is RecurringEvent.UpdateAmount -> _uiState.update { it.copy(amount = event.value, amountError = null, operationErrorMessage = null) }
            is RecurringEvent.UpdateType -> _uiState.update { it.copy(type = event.value, operationErrorMessage = null) }
            is RecurringEvent.UpdateCategory -> _uiState.update { it.copy(category = event.value, operationErrorMessage = null) }
            is RecurringEvent.UpdateDayOfMonth -> _uiState.update { it.copy(dayOfMonth = event.value, dayError = null, operationErrorMessage = null) }
            is RecurringEvent.UpdateStartDate -> _uiState.update { it.copy(startDate = event.value, operationErrorMessage = null) }
            is RecurringEvent.UpdateActive -> _uiState.update { it.copy(active = event.value, operationErrorMessage = null) }
            RecurringEvent.SaveAdd -> saveAdd()
            RecurringEvent.SaveEdit -> saveEdit()
        }
    }

    private fun saveAdd() {
        val recurring = buildRecurring() ?: return

        // Permission errors on recurring writes are shown inline so the full-screen form stays recoverable.
        viewModelScope.launch {
            try {
                recurringRepositoryRef.insert(recurring)
                _uiState.update { RecurringUiState(searchVisible = it.searchVisible, searchQuery = it.searchQuery) }
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(operationErrorMessage = throwable.toFirestoreUserMessage(context)) }
            }
        }
    }

    private fun saveEdit() {
        val selected = _uiState.value.selectedRecurring ?: return
        val recurring = buildRecurring(id = selected.id, lastAppliedDate = selected.lastAppliedDate) ?: return
        viewModelScope.launch {
            try {
                recurringRepositoryRef.update(recurring)
                _uiState.update { RecurringUiState(searchVisible = it.searchVisible, searchQuery = it.searchQuery) }
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(operationErrorMessage = throwable.toFirestoreUserMessage(context)) }
            }
        }
    }

    private fun confirmDelete() {
        val id = _uiState.value.selectedRecurring?.id ?: return
        viewModelScope.launch {
            try {
                recurringRepositoryRef.delete(id)
                _uiState.update { RecurringUiState(searchVisible = it.searchVisible, searchQuery = it.searchQuery) }
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
                        showDeleteDialog = false,
                        operationErrorMessage = throwable.toFirestoreUserMessage(context),
                    )
                }
            }
        }
    }

    private fun applyDue() {
        // Applying due transactions may generate many writes, so treat the whole batch as one recoverable action.
        viewModelScope.launch {
            try {
                recurringRepositoryRef.applyDueRecurringTransactions(recurringTransactions.value)
                _uiState.update { it.copy(operationErrorMessage = null) }
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(operationErrorMessage = throwable.toFirestoreUserMessage(context)) }
            }
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
