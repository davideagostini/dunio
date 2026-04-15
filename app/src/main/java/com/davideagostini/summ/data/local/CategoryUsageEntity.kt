package com.davideagostini.summ.data.local

import androidx.room.Entity

@Entity(
    tableName = "category_usage",
    primaryKeys = ["householdId", "type", "categoryStableId"],
)
data class CategoryUsageEntity(
    val householdId: String,
    val type: String,
    val categoryStableId: String,
    val count: Int,
    val lastUsedAt: Long,
)
