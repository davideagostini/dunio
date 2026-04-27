package com.davideagostini.summ.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import com.davideagostini.summ.widget.SummWidgetsUpdater
import kotlinx.coroutines.withTimeoutOrNull

class RecurringAutoApplyWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            RecurringAutoApplyWorkerEntryPoint::class.java,
        )
        val sessionRepository = entryPoint.sessionRepository()
        val recurringRepository = entryPoint.recurringTransactionRepository()

        val householdId = withTimeoutOrNull(5_000) {
            sessionRepository.requireHouseholdId()
        } ?: return Result.success()

        val recurringTransactions = withTimeoutOrNull(5_000) {
            recurringRepository.allRecurringTransactionsOnce(householdId)
        } ?: return Result.success()

        val createdCount = recurringRepository.applyDueRecurringTransactions(recurringTransactions)
        if (createdCount > 0) {
            SummWidgetsUpdater.refreshAll(applicationContext)
        }
        return Result.success()
    }
}
