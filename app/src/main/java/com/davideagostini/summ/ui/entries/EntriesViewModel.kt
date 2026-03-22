package com.davideagostini.summ.ui.entries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.data.repository.CategoryRepository
import com.davideagostini.summ.data.repository.EntryRepository
import com.davideagostini.summ.domain.model.HomeState
import com.davideagostini.summ.domain.usecase.GetHomeDataUseCase
import com.davideagostini.summ.ui.format.formatAmount
import dagger.hilt.android.lifecycle.HiltViewModel
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
    getHomeData: GetHomeDataUseCase,
    private val entryRepository: EntryRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val homeLoaded = MutableStateFlow(false)
    private val categoriesLoaded = MutableStateFlow(false)

    val homeState: StateFlow<HomeState> = getHomeData()
        .onEach { homeLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .onEach { categoriesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoading: StateFlow<Boolean> = combine(homeLoaded, categoriesLoaded) { homeReady, categoriesReady ->
        !homeReady || !categoriesReady
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
                    )
                }
            }
            EntriesEvent.RequestDelete     -> _uiState.update { it.copy(showDeleteDialog = true) }
            EntriesEvent.DismissSheet      -> _uiState.update { it.clearTransientState() }
            is EntriesEvent.UpdateType         -> _uiState.update { it.copy(editType = event.value) }
            is EntriesEvent.UpdateDescription  -> _uiState.update { it.copy(editDescription = event.value, descriptionError = null) }
            is EntriesEvent.UpdatePrice        -> _uiState.update { it.copy(editPrice = event.value, priceError = null) }
            is EntriesEvent.UpdateDate         -> _uiState.update { it.copy(editDate = event.value) }
            is EntriesEvent.UpdateCategory     -> _uiState.update { it.copy(editCategory = event.category) }
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
            _uiState.update { it.copy(descriptionError = "Description is required") }
            hasError = true
        }
        val price = state.editPrice.toDoubleOrNull()
        if (price == null || price <= 0) {
            _uiState.update { it.copy(priceError = "Enter a valid amount") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
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
            _uiState.update { it.copy(sheetMode = EntrySheetMode.Success) }
            delay(1_500L)
            _uiState.update { it.clearTransientState() }
        }
    }

    private fun confirmDelete() {
        val entry = _uiState.value.selectedEntry ?: return
        viewModelScope.launch {
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
            _uiState.update { it.copy(sheetMode = EntrySheetMode.Success, showDeleteDialog = false) }
            delay(1_500L)
            _uiState.update { it.clearTransientState() }
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
            showDeleteDialog = false,
        )
}
