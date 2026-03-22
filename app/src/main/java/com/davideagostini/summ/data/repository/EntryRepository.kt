package com.davideagostini.summ.data.repository

import com.davideagostini.summ.data.dao.EntryDao
import com.davideagostini.summ.data.entity.Entry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntryRepository @Inject constructor(private val entryDao: EntryDao) {

    val allEntries: Flow<List<Entry>> = entryDao.getAllEntries()
    val balance: Flow<Double> = entryDao.getBalance()

    suspend fun insert(entry: Entry) = entryDao.insert(entry)
    suspend fun update(entry: Entry) = entryDao.update(entry)
    suspend fun delete(entry: Entry) = entryDao.delete(entry)
}
