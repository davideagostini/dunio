package com.davideagostini.summ.ui.entries

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
import com.davideagostini.summ.domain.model.HomeState
import com.davideagostini.summ.domain.usecase.GetHomeDataUseCase
import com.davideagostini.summ.ui.format.formatAmount
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
    @ApplicationContext private val appContext: Context,
    getHomeData: GetHomeDataUseCase,
    private val entryRepository: EntryRepository,
    private val categoryRepository: CategoryRepository,
    monthCloseRepository: MonthCloseRepository,
) : ViewModel() {
    private val homeLoaded = MutableStateFlow(false)
    private val categoriesLoaded = MutableStateFlow(false)
    private val monthClosesLoaded = MutableStateFlow(false)

    val homeState: StateFlow<HomeState> = getHomeData()
        .onEach { homeLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .onEach { categoriesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthCloses: StateFlow<List<MonthClose>> = monthCloseRepository.allMonthCloses
        .onEach { monthClosesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoading: StateFlow<Boolean> = combine(homeLoaded, categoriesLoaded, monthClosesLoaded) { homeReady, categoriesReady, monthClosesReady ->
        !homeReady || !categoriesReady || !monthClosesReady
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _uiState = MutableStateFlow(EntriesUiState())
    val uiState: StateFlow<EntriesUiState> = _uiState.asStateFlow()

    fun handleEvent(event: EntriesEvent) {
        when (event) {
            is EntriesEvent.SelectMonth    -> _uiState.update { it.copy(selectedMonth = event.monthKey) }
            is EntriesEvent.SelectFilter   -> _uiState.update { it.copy(filterType = event.filter) }
            EntriesEvent.ToggleSearch      -> _uiState.update {
                it.copy(
                    searchVisible = !it.searchVisible,
                    searchQuery = if (it.searchVisible) "" else it.searchQuery,
                )
            }
            is EntriesEvent.UpdateSearchQuery -> _uiState.update { it.copy(searchQuery = event.query) }
            is EntriesEvent.Select         -> _uiState.update {
                it.copy(sheetMode = EntrySheetMode.Action, selectedEntry = event.entry)
            }
            EntriesEvent.StartEdit         -> {
                val entry = _uiState.value.selectedEntry ?: return
                val cat   = categories.value.firstOrNull { it.name == entry.category }
                _uiState.update {
                    it.copy(
                        sheetMode        = EntrySheetMode.Edit,
                        editType         = entry.type,
                        editDescription  = entry.description,
                        editPrice        = formatAmount(entry.price),
                        editDate         = entry.date,
                        editCategory     = cat,
                        descriptionError = null,
                        priceError       = null,
                        operationErrorMessage = null,
                    )
                }
            }
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
        val state = _uiState.value
        val entry = state.selectedEntry ?: return
        var hasError = false

        if (state.editDescription.isBlank()) {
            _uiState.update { it.copy(descriptionError = appContext.getString(R.string.entries_validation_description_required)) }
            hasError = true
        }
        val price = state.editPrice.toDoubleOrNull()
        if (price == null || price <= 0) {
            _uiState.update { it.copy(priceError = appContext.getString(R.string.entries_validation_amount_valid)) }
            hasError = true
        }
        if (hasError) return

        // Edit is a Firestore write, so we surface backend permission problems inline in the sheet.
        viewModelScope.launch {
            try {
                entryRepository.update(
                    Entry(
                        id          = entry.id,
                        type        = state.editType,
                        description = state.editDescription.trim(),
                        price       = price!!,
                        category    = state.editCategory?.name ?: entry.category,
                        date        = state.editDate,
                    )
                )
                _uiState.update { it.copy(sheetMode = EntrySheetMode.Success, operationErrorMessage = null) }
                delay(1_500L)
                _uiState.update { it.clearTransientState() }
            } catch (throwable: Throwable) {
                _uiState.update { it.copy(operationErrorMessage = throwable.toFirestoreUserMessage(appContext)) }
            }
        }
    }

    private fun confirmDelete() {
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
                        date        = entry.date,
                    )
                )
                _uiState.update { it.copy(sheetMode = EntrySheetMode.Success, showDeleteDialog = false, operationErrorMessage = null) }
                delay(1_500L)
                _uiState.update { it.clearTransientState() }
            } catch (throwable: Throwable) {
                _uiState.update {
                    it.copy(
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
            showDeleteDialog = false,
        )
}
