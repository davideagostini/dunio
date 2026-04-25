package com.davideagostini.summ.ui.settings.monthclose

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.data.repository.AssetRepository
import com.davideagostini.summ.data.repository.EntryRepository
import com.davideagostini.summ.data.repository.MonthCloseRepository
import com.davideagostini.summ.data.repository.RecurringTransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
/**
 * ViewModel for month close management.
 *
 * It gathers the selected month's transactions, asset snapshot, recurring templates, and existing
 * month-close markers to decide whether the month can be closed and what guidance to show.
 */
class MonthCloseViewModel @Inject constructor(
    @param:ApplicationContext
    private val appContext: Context,
    assetRepository: AssetRepository,
    entryRepository: EntryRepository,
    recurringRepository: RecurringTransactionRepository,
    monthCloseRepository: MonthCloseRepository,
) : ViewModel() {
    private val defaultSelectedMonth = YearMonth.now().toString()
    private val historyLoaded = MutableStateFlow(false)
    private val entriesLoaded = MutableStateFlow(false)
    private val recurringLoaded = MutableStateFlow(false)
    private val monthClosesLoaded = MutableStateFlow(false)
    private val selectedMonth = MutableStateFlow(defaultSelectedMonth)

    private val assetHistory = selectedMonth.flatMapLatest { month ->
        assetRepository.observeAssetHistoryForMonth(month)
    }
        .onEach { historyLoaded.value = true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val entries = selectedMonth.flatMapLatest { month ->
        entryRepository.observeEntriesForMonth(month)
    }
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

    val uiState: StateFlow<MonthCloseUiState> = combine(
        selectedMonth,
        assetHistory,
        entries,
        recurringTransactions,
        monthCloses,
    ) { month, history, transactions, recurring, closes ->
        val monthAssets = history.buildAssetsSnapshotForMonth(month)
        val monthTransactions = transactions.filter { it.period.ifBlank { it.date.toMonthKey() } == month }
        val missingRecurring = recurring
            .filter { it.active }
            .filter { it.startDate.take(7) <= month }
            .filter { recurringEntry ->
                val dueDate = getDueDateForMonth(month, recurringEntry.dayOfMonth)
                monthTransactions.none { transaction ->
                    transaction.recurringTransactionId == recurringEntry.id &&
                        transaction.date.toLocalDateString() == dueDate
                }
            }
        val existingClose = closes.firstOrNull { it.period == month }

        MonthCloseUiState(
            month = month,
            status = existingClose?.status ?: "draft",
            assetSnapshotCount = monthAssets.size,
            transactionCount = monthTransactions.size,
            recurringMissingCount = missingRecurring.size,
            recurringMissingLabels = missingRecurring.map {
                appContext.getString(R.string.recurring_day_chip, it.description, it.dayOfMonth)
            },
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
