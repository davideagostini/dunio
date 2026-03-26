package com.davideagostini.summ.data.repository

import com.davideagostini.summ.data.dao.RecurringTransactionDao
import com.davideagostini.summ.data.entity.RecurringTransaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringTransactionRepository @Inject constructor(
    private val dao: RecurringTransactionDao,
) {
    val allRecurringTransactions: Flow<List<RecurringTransaction>> = dao.getAllRecurringTransactions()

    suspend fun insert(recurring: RecurringTransaction) = dao.insert(recurring)
    suspend fun update(recurring: RecurringTransaction) = dao.update(recurring)
    suspend fun delete(recurringId: String) = dao.delete(recurringId)
    suspend fun applyDueRecurringTransactions(recurringTransactions: List<RecurringTransaction>): Int =
        dao.applyDueRecurringTransactions(recurringTransactions)
}
