package com.davideagostini.summ.ui.settings.monthclose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.data.entity.MonthClose
import com.davideagostini.summ.data.entity.RecurringTransaction
import com.davideagostini.summ.data.repository.AssetRepository
import com.davideagostini.summ.data.repository.EntryRepository
import com.davideagostini.summ.data.repository.MonthCloseRepository
import com.davideagostini.summ.data.repository.RecurringTransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class MonthCloseViewModel @Inject constructor(
    assetRepository: AssetRepository,
    entryRepository: EntryRepository,
    recurringRepository: RecurringTransactionRepository,
    monthCloseRepository: MonthCloseRepository,
) : ViewModel() {
    private val historyLoaded = MutableStateFlow(false)
    private val entriesLoaded = MutableStateFlow(false)
    private val recurringLoaded = MutableStateFlow(false)
    private val monthClosesLoaded = MutableStateFlow(false)

    private val assetHistory = assetRepository.allAssetHistory
        .onEach { historyLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val entries = entryRepository.allEntries
        .onEach { entriesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val recurringTransactions = recurringRepository.allRecurringTransactions
        .onEach { recurringLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val monthCloses = monthCloseRepository.allMonthCloses
        .onEach { monthClosesLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isLoading: StateFlow<Boolean> = combine(historyLoaded, entriesLoaded, recurringLoaded, monthClosesLoaded) { a, b, c, d ->
        !a || !b || !c || !d
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val selectedMonth = MutableStateFlow(YearMonth.now().toString())

    val uiState: StateFlow<MonthCloseUiState> = combine(
        selectedMonth, assetHistory, entries, recurringTransactions, monthCloses
    ) { month, history, transactions, recurring, closes ->
        val monthAssets = history.buildAssetsSnapshotForMonth(month)
        val monthTransactions = transactions.filter { it.period.ifBlank { it.date.toMonthKey() } == month }
        val missingRecurring = recurring
            .filter { it.active }
            .filter { it.startDate.take(7) <= month }
            .filter { recurringEntry ->
                val dueDate = getDueDateForMonth(month, recurringEntry.dayOfMonth)
                transactions.none { transaction ->
                    transaction.recurringTransactionId == recurringEntry.id &&
                        transaction.date.toLocalDateString() == dueDate
                }
            }
        val existingClose = closes.firstOrNull { it.period == month }

        MonthCloseUiState(
            month = month,
            monthOptions = buildMonthOptions(history, transactions, recurring, month),
            status = existingClose?.status ?: "draft",
            assetSnapshotCount = monthAssets.size,
            transactionCount = monthTransactions.size,
            recurringMissingCount = missingRecurring.size,
            recurringMissingLabels = missingRecurring.map { "${it.description} · day ${it.dayOfMonth}" },
            canClose = monthAssets.isNotEmpty() && missingRecurring.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthCloseUiState())

    private val monthCloseRepositoryRef = monthCloseRepository

    fun selectMonth(month: String) {
        selectedMonth.value = month
    }

    fun setStatus(status: String) {
        val state = uiState.value
        viewModelScope.launch {
            monthCloseRepositoryRef.upsertMonthClose(
                period = state.month,
                status = status,
                assetSnapshotCount = state.assetSnapshotCount,
                transactionCount = state.transactionCount,
                recurringMissingCount = state.recurringMissingCount,
            )
        }
    }
}

private fun buildMonthOptions(
    assetHistory: List<AssetHistoryEntry>,
    entries: List<Entry>,
    recurring: List<RecurringTransaction>,
    selectedMonth: String,
): List<String> {
    val months = mutableSetOf<String>()
    months += assetHistory.map { it.period }
    months += entries.map { it.period.ifBlank { it.date.toMonthKey() } }
    months += recurring.map { it.startDate.take(7) }
    val current = YearMonth.parse(selectedMonth)
    repeat(12) { index -> months += current.minusMonths(index.toLong()).toString() }
    return months.filter { it.isNotBlank() }.sortedDescending()
}

private fun List<AssetHistoryEntry>.buildAssetsSnapshotForMonth(month: String) =
    filter { it.period == month }
        .associateBy { it.name.trim().lowercase() }
        .values
        .filter { it.action != "deleted" }

private fun getDueDateForMonth(monthKey: String, dayOfMonth: Int): String {
    val yearMonth = YearMonth.parse(monthKey)
    return yearMonth.atDay(dayOfMonth.coerceAtMost(yearMonth.lengthOfMonth())).toString()
}

private fun Long.toLocalDateString(): String =
    java.time.Instant.ofEpochMilli(this)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .toString()

private fun Long.toMonthKey(): String = toLocalDateString().take(7)
