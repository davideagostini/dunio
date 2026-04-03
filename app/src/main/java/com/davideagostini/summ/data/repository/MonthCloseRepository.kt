package com.davideagostini.summ.data.repository

import com.davideagostini.summ.data.dao.MonthCloseDao
import com.davideagostini.summ.data.entity.MonthClose
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * Repository facade for month-close state.
 *
 * It provides the small set of operations needed to observe and mutate closed months while
 * keeping the rest of the app unaware of the storage implementation.
 */
class MonthCloseRepository @Inject constructor(
    private val dao: MonthCloseDao,
) {
    val allMonthCloses: Flow<List<MonthClose>> = dao.getAllMonthCloses()

    suspend fun upsertMonthClose(
        period: String,
        status: String,
        assetSnapshotCount: Int,
        transactionCount: Int,
        recurringMissingCount: Int,
    ) = dao.upsertMonthClose(period, status, assetSnapshotCount, transactionCount, recurringMissingCount)
}
