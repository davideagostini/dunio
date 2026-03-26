package com.davideagostini.summ.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.data.repository.AssetRepository
import com.davideagostini.summ.data.repository.EntryRepository
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.ui.format.DEFAULT_CURRENCY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    assetRepository: AssetRepository,
    entryRepository: EntryRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {
    private val historyLoaded = MutableStateFlow(false)
    private val entriesLoaded = MutableStateFlow(false)

    private val assetHistory: StateFlow<List<AssetHistoryEntry>> = assetRepository.allAssetHistory
        .onEach { historyLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val entries: StateFlow<List<Entry>> = entryRepository.allEntries
        .onEach { entriesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val householdCurrency: StateFlow<String> = sessionRepository.householdCurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_CURRENCY)

    val isLoading: StateFlow<Boolean> = combine(historyLoaded, entriesLoaded) { history, entries ->
        !history || !entries
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val renderState: StateFlow<DashboardRenderState> = combine(assetHistory, entries, householdCurrency, uiState) { history, allEntries, householdCurrency, state ->
        val selectedMonth = state.selectedMonth ?: YearMonth.now().toString()
        val selectedRange = state.selectedRange
        val assetsForMonth = buildAssetsSnapshotForMonth(history, selectedMonth)
        val monthEntries = allEntries.filter { entry -> monthKey(entry.date) == selectedMonth }
        val metrics = calculateDashboardMetrics(assetsForMonth, monthEntries, allEntries, selectedMonth)
        val averageSavingsRate = calculateAverageSavingsRate(allEntries, selectedMonth, 3)
        val savingsRateDelta = if (metrics.savingsRate != null && averageSavingsRate != null) {
            metrics.savingsRate - averageSavingsRate
        } else {
            null
        }

        val monthOptions = buildPreviousMonths(selectedMonth, selectedRange.months)
        val chartPoints = monthOptions
            .asReversed()
            .map { month ->
                ChartPoint(
                    month = month,
                    label = formatShortMonth(month),
                    value = calculateNetWorthForMonth(history, month),
                )
            }

        val previousMonth = YearMonth.parse(selectedMonth).minusMonths(1).toString()
        val previousAssetsForMonth = buildAssetsSnapshotForMonth(history, previousMonth)
        val previousMonthEntries = allEntries.filter { entry -> monthKey(entry.date) == previousMonth }
        val previousMetrics = calculateDashboardMetrics(
            previousAssetsForMonth,
            previousMonthEntries,
            allEntries,
            previousMonth,
        )

        val previousValue = calculateNetWorthForMonth(history, previousMonth)
            .takeIf { hasActiveSnapshotForMonth(history, previousMonth) }
        val monthlyChangePercent = if (previousValue != null && previousValue != 0.0) {
            (metrics.netWorth - previousValue) / kotlin.math.abs(previousValue)
        } else {
            null
        }
        val cashFlowChangePercent = calculateRelativeChange(
            currentValue = metrics.monthlyCashFlow,
            previousValue = previousMetrics.monthlyCashFlow.takeIf { previousMonthEntries.isNotEmpty() },
        )
        val monthlyExpenses = monthEntries
            .filter { it.type == "expense" }
            .sumOf { it.price }
        val previousMonthlyExpenses = previousMonthEntries
            .filter { it.type == "expense" }
            .sumOf { it.price }
        val monthlyExpensesChangePercent = calculateRelativeChange(
            currentValue = monthlyExpenses,
            previousValue = previousMonthlyExpenses.takeIf { previousMonthEntries.isNotEmpty() },
        )
        val runwayChangePercent = calculateRelativeChange(
            currentValue = metrics.financialRunway ?: 0.0,
            previousValue = previousMetrics.financialRunway?.takeIf { previousMonthEntries.isNotEmpty() },
        )

        DashboardRenderState(
            selectedMonth = selectedMonth,
            householdCurrency = householdCurrency,
            selectedRange = selectedRange,
            metrics = metrics,
            chartPoints = chartPoints,
            monthlyChangePercent = monthlyChangePercent,
            cashFlowChangePercent = cashFlowChangePercent,
            savingsRateDelta = savingsRateDelta,
            monthlyExpenses = monthlyExpenses,
            monthlyExpensesChangePercent = monthlyExpensesChangePercent,
            runwayChangePercent = runwayChangePercent,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardRenderState(
            selectedMonth = YearMonth.now().toString(),
            householdCurrency = DEFAULT_CURRENCY,
            selectedRange = DashboardRange.SixMonths,
            metrics = DashboardMetrics(0.0, 0.0, 0.0, null, 0.0, null),
            chartPoints = emptyList(),
            monthlyChangePercent = null,
            cashFlowChangePercent = null,
            savingsRateDelta = null,
            monthlyExpenses = 0.0,
            monthlyExpensesChangePercent = null,
            runwayChangePercent = null,
        ),
    )

    fun selectMonth(month: String) {
        _uiState.update { it.copy(selectedMonth = month) }
    }

    fun selectRange(range: DashboardRange) {
        _uiState.update { it.copy(selectedRange = range) }
    }
}
