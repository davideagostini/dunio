package com.davideagostini.summ.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CategoryUsageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SummLocalDatabase : RoomDatabase() {
    abstract fun categoryUsageDao(): CategoryUsageDao
}
