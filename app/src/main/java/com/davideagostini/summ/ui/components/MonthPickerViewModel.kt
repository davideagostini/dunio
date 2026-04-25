package com.davideagostini.summ.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.data.repository.DashboardMonthlyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MonthPickerViewModel @Inject constructor(
    dashboardMonthlyRepository: DashboardMonthlyRepository,
) : ViewModel() {
    val monthOptions: StateFlow<List<String>> = dashboardMonthlyRepository.observeEarliestSummary()
        .map { summary ->
            buildMonthOptions(summary?.period)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = buildMonthOptions(null),
        )
}
