package com.davideagostini.summ.data.work

import com.davideagostini.summ.data.repository.RecurringTransactionRepository
import com.davideagostini.summ.data.session.SessionRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RecurringAutoApplyWorkerEntryPoint {
    fun sessionRepository(): SessionRepository
    fun recurringTransactionRepository(): RecurringTransactionRepository
}
