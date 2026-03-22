package com.davideagostini.summ.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.data.repository.AssetRepository
import com.davideagostini.summ.data.repository.CategoryRepository
import com.davideagostini.summ.data.repository.EntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    assetRepository: AssetRepository,
    entryRepository: EntryRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {
    private val historyLoaded = MutableStateFlow(false)
    private val entriesLoaded = MutableStateFlow(false)
    private val categoriesLoaded = MutableStateFlow(false)

    val assetHistory: StateFlow<List<AssetHistoryEntry>> = assetRepository.allAssetHistory
        .onEach { historyLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val entries: StateFlow<List<Entry>> = entryRepository.allEntries
        .onEach { entriesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.allCategories
        .onEach { categoriesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoading: StateFlow<Boolean> = combine(historyLoaded, entriesLoaded, categoriesLoaded) { history, entries, categories ->
        !history || !entries || !categories
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun selectMonth(month: String) {
        _uiState.update { it.copy(selectedMonth = month) }
    }

    fun selectRange(range: DashboardRange) {
        _uiState.update { it.copy(selectedRange = range) }
    }
}
