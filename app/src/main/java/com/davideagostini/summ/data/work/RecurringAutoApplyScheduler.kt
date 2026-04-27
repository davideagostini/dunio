package com.davideagostini.summ.data.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RecurringAutoApplyScheduler {
    private const val UNIQUE_WORK_NAME = "recurring_auto_apply_daily"
    private const val DEBUG_ONCE_WORK_NAME = "recurring_auto_apply_debug_once"

    fun schedule(context: Context) {
        val request =
            PeriodicWorkRequestBuilder<RecurringAutoApplyWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun triggerNow(context: Context) {
        val request =
            OneTimeWorkRequestBuilder<RecurringAutoApplyWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DEBUG_ONCE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
