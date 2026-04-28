package com.davideagostini.summ.data.repository

import com.davideagostini.summ.data.category.stableUsageId
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.local.CategoryUsageDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryUsageRepository @Inject constructor(
    private val categoryUsageDao: CategoryUsageDao,
) {

    fun observeMostUsedCategories(
        householdId: String,
        type: String,
        categories: List<Category>,
        limit: Int = MOST_USED_LIMIT,
    ): Flow<List<Category>> {
        val categoriesByStableId = categories
            .asSequence()
            .associateBy { category -> category.stableUsageId() }

        return categoryUsageDao
            .observeMostUsed(
                householdId = householdId,
                type = type,
            )
            .map { usageRows ->
                usageRows
                    .mapNotNull { row -> categoriesByStableId[row.categoryStableId] }
                    .take(limit)
            }
    }

    suspend fun markUsed(
        householdId: String,
        type: String,
        category: Category,
    ) {
        categoryUsageDao.markUsed(
            householdId = householdId,
            type = type,
            categoryStableId = category.stableUsageId(),
            usedAt = System.currentTimeMillis(),
        )
    }

    private companion object {
        const val MOST_USED_LIMIT = 5
    }
}
