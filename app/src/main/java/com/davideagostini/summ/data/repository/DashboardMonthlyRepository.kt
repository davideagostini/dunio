package com.davideagostini.summ.data.repository

import com.davideagostini.summ.data.dao.DashboardMonthlyDao
import com.davideagostini.summ.data.entity.DashboardMonthlySummary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardMonthlyRepository @Inject constructor(
    private val dao: DashboardMonthlyDao,
) {
    fun observeHasAnyTransactions(): Flow<Boolean> = dao.observeHasAnyTransactions()

    fun observeHasAnyAssets(): Flow<Boolean> = dao.observeHasAnyAssets()

    fun observeLatestSummary(): Flow<DashboardMonthlySummary?> = dao.observeLatestSummary()

    fun observeRecentSummaries(limit: Long = 12L): Flow<List<DashboardMonthlySummary>> =
        dao.observeRecentSummaries(limit)

    fun observeSummaryWindow(startPeriod: String, endPeriod: String): Flow<List<DashboardMonthlySummary>> =
        dao.observeSummaryWindow(startPeriod, endPeriod)
}
