package com.davideagostini.summ.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryUsageDao {

    @Query(
        """
        SELECT *
        FROM category_usage
        WHERE householdId = :householdId AND type = :type
        ORDER BY count DESC, lastUsedAt DESC
        """
    )
    fun observeMostUsed(
        householdId: String,
        type: String,
    ): Flow<List<CategoryUsageEntity>>

    @Query(
        """
        UPDATE category_usage
        SET count = count + 1, lastUsedAt = :lastUsedAt
        WHERE householdId = :householdId
            AND type = :type
            AND categoryStableId = :categoryStableId
        """
    )
    suspend fun increment(
        householdId: String,
        type: String,
        categoryStableId: String,
        lastUsedAt: Long,
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: CategoryUsageEntity)

    @Transaction
    suspend fun markUsed(
        householdId: String,
        type: String,
        categoryStableId: String,
        usedAt: Long,
    ) {
        val updated = increment(
            householdId = householdId,
            type = type,
            categoryStableId = categoryStableId,
            lastUsedAt = usedAt,
        )
        if (updated == 0) {
            insert(
                CategoryUsageEntity(
                    householdId = householdId,
                    type = type,
                    categoryStableId = categoryStableId,
                    count = 1,
                    lastUsedAt = usedAt,
                )
            )
        }
    }
}
