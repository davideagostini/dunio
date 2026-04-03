package com.davideagostini.summ.data.repository

import com.davideagostini.summ.data.dao.CategoryDao
import com.davideagostini.summ.data.entity.Category
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * Repository facade for categories.
 *
 * Screens use this layer to observe merged category lists and perform CRUD without reaching into
 * Firestore-specific concerns.
 */
class CategoryRepository @Inject constructor(private val categoryDao: CategoryDao) {

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun insert(category: Category) = categoryDao.insert(category)

    suspend fun update(category: Category) = categoryDao.update(category)

    suspend fun delete(category: Category) = categoryDao.delete(category)
}
