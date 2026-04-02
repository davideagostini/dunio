package com.davideagostini.summ.data.repository

import android.content.Context
import com.davideagostini.summ.data.dao.EntryDao
import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.widget.SummWidgetsUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntryRepository @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val entryDao: EntryDao,
) {

    val allEntries: Flow<List<Entry>> = entryDao.getAllEntries()

    fun observeEntriesForMonth(period: String): Flow<List<Entry>> = entryDao.getEntriesForMonth(period)

    fun observeEntriesBetween(startDate: String, endExclusiveDate: String): Flow<List<Entry>> =
        entryDao.getEntriesBetween(startDate, endExclusiveDate)

    fun observeHasAnyEntries(): Flow<Boolean> = entryDao.getHasAnyEntries()

    // Entry mutations are the main source of truth for the spending widget,
    // so we refresh widgets immediately after every successful write.
    suspend fun insert(entry: Entry) {
        entryDao.insert(entry)
        SummWidgetsUpdater.refreshAll(appContext)
    }

    suspend fun update(entry: Entry) {
        entryDao.update(entry)
        SummWidgetsUpdater.refreshAll(appContext)
    }

    suspend fun delete(entry: Entry) {
        entryDao.delete(entry)
        SummWidgetsUpdater.refreshAll(appContext)
    }
}
