package com.davideagostini.summ.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.data.entity.DashboardMonthlySummary
import com.davideagostini.summ.data.repository.DashboardMonthlyRepository
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.ui.format.DEFAULT_CURRENCY
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
/**
 * ViewModel for the household dashboard.
 *
 * It combines the backend-generated monthly dashboard summaries with local UI preferences such as
 * the selected month/range and the Get Started dismissal state. The result is a render model that
 * keeps the Compose screen lightweight.
 */
class DashboardViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    dashboardMonthlyRepository: DashboardMonthlyRepository,
    sessionRepository: SessionRepository,
) : ViewModel() {
    private val sharingStarted = SharingStarted.WhileSubscribed(30_000)
    private val latestSummaryLoaded = MutableStateFlow(false)
    private val windowLoaded = MutableStateFlow(false)
    private val hasTransactionsLoaded = MutableStateFlow(false)
    private val hasAssetsLoaded = MutableStateFlow(false)
    private val pendingMonthRefresh = MutableStateFlow<String?>(null)

    private val latestSummary: StateFlow<DashboardMonthlySummary?> = dashboardMonthlyRepository.observeLatestSummary()
        .onEach { latestSummaryLoaded.value = true }
        .stateIn(viewModelScope, sharingStarted, null)

    private val hasAnyTransactions: StateFlow<Boolean> = dashboardMonthlyRepository.observeHasAnyTransactions()
        .onEach { hasTransactionsLoaded.value = true }
        .stateIn(viewModelScope, sharingStarted, false)

    private val hasAnyAssets: StateFlow<Boolean> = dashboardMonthlyRepository.observeHasAnyAssets()
        .onEach { hasAssetsLoaded.value = true }
        .stateIn(viewModelScope, sharingStarted, false)

    private val householdCurrency: StateFlow<String> = sessionRepository.householdCurrency
        .stateIn(viewModelScope, sharingStarted, DEFAULT_CURRENCY)
    private val getStartedPrefs = DashboardGetStartedManager.prefs

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val effectiveSelectedMonth: StateFlow<String> = combine(uiState, latestSummary) { state, latest ->
        state.selectedMonth
            ?: latest?.period
            ?: YearMonth.now().toString()
    }.stateIn(
        viewModelScope,
        sharingStarted,
        YearMonth.now().toString(),
    )

    private val dashboardWindow: StateFlow<List<DashboardMonthlySummary>> = combine(
        effectiveSelectedMonth,
        uiState.map { it.selectedRange },
    ) { selectedMonth, selectedRange ->
        val loadCount = maxOf(selectedRange.months, 4)
        val monthWindow = buildPreviousMonths(selectedMonth, loadCount)
        monthWindow.last() to monthWindow.first()
    }.flatMapLatest { (startMonth, endMonth) ->
        dashboardMonthlyRepository.observeSummaryWindow(startMonth, endMonth)
    }.onEach { summaries ->
        windowLoaded.value = true
        val pendingMonth = pendingMonthRefresh.value
        if (pendingMonth != null && summaries.any { it.period == pendingMonth }) {
            pendingMonthRefresh.value = null
        }
    }.stateIn(
        viewModelScope,
        sharingStarted,
        emptyList(),
    )

    val isMonthRefreshing: StateFlow<Boolean> = pendingMonthRefresh
        .map { pendingMonth -> pendingMonth != null }
        .stateIn(viewModelScope, sharingStarted, false)

    private val dashboardBaseState: StateFlow<DashboardBaseState> = combine(
        dashboardWindow,
        effectiveSelectedMonth,
        uiState,
        householdCurrency,
    ) { window, selectedMonth, state, currency ->
        val summariesByPeriod = window.associateBy { it.period }
        val selectedSummary = summariesByPeriod[selectedMonth] ?: DashboardMonthlySummary.empty(selectedMonth)
        val previousMonth = YearMonth.parse(selectedMonth).minusMonths(1).toString()
        val previousSummary = summariesByPeriod[previousMonth] ?: DashboardMonthlySummary.empty(previousMonth)

        val chartPoints = buildMonthlySeries(summariesByPeriod, selectedMonth, state.selectedRange.months)
            .map { summary ->
                ChartPoint(
                    month = summary.period,
                    label = formatShortMonth(summary.period),
                    value = summary.netWorth,
                )
            }

        val savingsRateMonths = buildPreviousMonths(selectedMonth, 4)
            .drop(1)
            .map { period -> summariesByPeriod[period] ?: DashboardMonthlySummary.empty(period) }
        val averageSavingsRate = calculateAverageSavingsRate(savingsRateMonths)
        val savingsRateDelta = if (selectedSummary.savingsRate != null && averageSavingsRate != null) {
            selectedSummary.savingsRate - averageSavingsRate
        } else {
            null
        }

        val runwayMonths = buildMonthlySeries(summariesByPeriod, selectedMonth, 3)
        val previousRunwayMonths = buildMonthlySeries(summariesByPeriod, previousMonth, 3)
        val metrics = buildDashboardMetrics(
            selectedSummary = selectedSummary,
            runwaySummaries = runwayMonths,
        )
        val previousRunway = calculateFinancialRunway(previousSummary, previousRunwayMonths)

        val monthlyChangePercent = if (previousSummary.activeAssetCount > 0) {
            calculateRelativeChange(
                currentValue = metrics.netWorth,
                previousValue = previousSummary.netWorth,
            )
        } else {
            null
        }
        val cashFlowChangePercent = if (previousSummary.transactionCount > 0) {
            calculateRelativeChange(
                currentValue = metrics.monthlyCashFlow,
                previousValue = previousSummary.cashFlow,
            )
        } else {
            null
        }
        val monthlyExpensesChangePercent = if (previousSummary.transactionCount > 0) {
            calculateRelativeChange(
                currentValue = selectedSummary.monthlyExpenses,
                previousValue = previousSummary.monthlyExpenses,
            )
        } else {
            null
        }
        val runwayChangePercent = if (previousSummary.transactionCount > 0) {
            calculateRelativeChange(
                currentValue = metrics.financialRunway ?: 0.0,
                previousValue = previousRunway,
            )
        } else {
            null
        }

        DashboardBaseState(
            selectedMonth = selectedMonth,
            householdCurrency = currency,
            selectedRange = state.selectedRange,
            metrics = metrics,
            chartPoints = chartPoints,
            monthlyChangePercent = monthlyChangePercent,
            cashFlowChangePercent = cashFlowChangePercent,
            savingsRateDelta = savingsRateDelta,
            monthlyExpenses = selectedSummary.monthlyExpenses,
            monthlyExpensesChangePercent = monthlyExpensesChangePercent,
            runwayChangePercent = runwayChangePercent,
        )
    }.stateIn(
        viewModelScope,
        sharingStarted,
        DashboardBaseState(
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

    val renderState: StateFlow<DashboardRenderState> = combine(
        dashboardBaseState,
        hasAnyTransactions,
        hasAnyAssets,
    ) { baseState, hasTransactions, hasAssets ->
        DashboardRenderState(
            selectedMonth = baseState.selectedMonth,
            householdCurrency = baseState.householdCurrency,
            selectedRange = baseState.selectedRange,
            hasEntries = hasTransactions,
            hasAssets = hasAssets,
            metrics = baseState.metrics,
            chartPoints = baseState.chartPoints,
            monthlyChangePercent = baseState.monthlyChangePercent,
            cashFlowChangePercent = baseState.cashFlowChangePercent,
            savingsRateDelta = baseState.savingsRateDelta,
            monthlyExpenses = baseState.monthlyExpenses,
            monthlyExpensesChangePercent = baseState.monthlyExpensesChangePercent,
            runwayChangePercent = baseState.runwayChangePercent,
        )
    }.stateIn(
        viewModelScope,
        sharingStarted,
        DashboardRenderState(
            selectedMonth = YearMonth.now().toString(),
            householdCurrency = DEFAULT_CURRENCY,
            selectedRange = DashboardRange.SixMonths,
            hasEntries = false,
            hasAssets = false,
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

    val isLoading: StateFlow<Boolean> = combine(
        latestSummaryLoaded,
        windowLoaded,
        hasTransactionsLoaded,
        hasAssetsLoaded,
    ) { latestLoaded, windowReady, transactionsReady, assetsReady ->
        !latestLoaded || !windowReady || !transactionsReady || !assetsReady
    }.stateIn(viewModelScope, sharingStarted, true)

    init {
        DashboardGetStartedManager.init(appContext)
    }

    init {
        viewModelScope.launch {
            combine(hasAnyTransactions, hasAnyAssets, getStartedPrefs) { hasEntries, hasAssets, prefs ->
                !prefs.dismissed && !hasEntries && !hasAssets
            }.collect { showGetStarted ->
                _uiState.update {
                    it.copy(
                        showGetStarted = showGetStarted,
                    )
                }
            }
        }
    }

    fun selectMonth(month: String) {
        if (month != effectiveSelectedMonth.value) {
            pendingMonthRefresh.value = month
        }
        _uiState.update { it.copy(selectedMonth = month) }
    }

    fun selectRange(range: DashboardRange) {
        _uiState.update { it.copy(selectedRange = range) }
    }

    fun dismissGetStarted() {
        DashboardGetStartedManager.dismiss(appContext)
    }
}

private data class DashboardBaseState(
    val selectedMonth: String,
    val householdCurrency: String,
    val selectedRange: DashboardRange,
    val metrics: DashboardMetrics,
    val chartPoints: List<ChartPoint>,
    val monthlyChangePercent: Double?,
    val cashFlowChangePercent: Double?,
    val savingsRateDelta: Double?,
    val monthlyExpenses: Double,
    val monthlyExpensesChangePercent: Double?,
    val runwayChangePercent: Double?,
)

private fun calculateFinancialRunway(
    selectedSummary: DashboardMonthlySummary,
    runwaySummaries: List<DashboardMonthlySummary>,
): Double? {
    val averageMonthlyExpenses = runwaySummaries
        .map { it.monthlyExpenses }
        .average()
    return if (averageMonthlyExpenses > 0) {
        selectedSummary.liquidAssets / averageMonthlyExpenses
    } else {
        null
    }
}
