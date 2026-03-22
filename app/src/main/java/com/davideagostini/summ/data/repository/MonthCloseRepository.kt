package com.davideagostini.summ.data.repository

import com.davideagostini.summ.data.dao.MonthCloseDao
import com.davideagostini.summ.data.entity.MonthClose
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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
