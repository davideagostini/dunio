package com.davideagostini.summ.data.dao

import android.content.Context
import com.davideagostini.summ.data.category.SystemCategories
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.firebase.FirestorePaths
import com.davideagostini.summ.data.firebase.firestoreFlow
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.data.session.SessionState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

private data class CategoryDocument(
    val name: String = "",
    val type: String = "expense",
    val icon: String? = null,
    val systemKey: String? = null,
    val usesDefaultTranslation: Boolean? = null,
)

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
/**
 * Firestore access layer for household categories.
 *
 * It merges system defaults with custom category documents and keeps category CRUD isolated from
 * the rest of the UI stack.
 */
class CategoryDao @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val firestore: FirebaseFirestore?,
    private val sessionRepository: SessionRepository,
) {

    suspend fun insert(category: Category) {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        val categoryId = category.id.ifBlank { encodeCategoryId(category.name) }
        db.document(FirestorePaths.category(householdId, categoryId))
            .set(
                mapOf(
                    "name" to category.name.trim(),
                    "type" to category.type,
                    "icon" to category.emoji,
                    "systemKey" to category.systemKey,
                    "usesDefaultTranslation" to category.usesDefaultTranslation,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                )
            )
            .await()
    }

    suspend fun update(category: Category) {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        val normalizedName = category.name.trim()
        val shouldUseDefaultTranslation = SystemCategories.shouldUseDefaultTranslation(
            context = appContext,
            key = category.systemKey,
            name = normalizedName,
        )
        db.document(FirestorePaths.category(householdId, category.id))
            .update(
                mapOf(
                    "name" to normalizedName,
                    "type" to category.type,
                    "icon" to category.emoji,
                    "systemKey" to category.systemKey,
                    "usesDefaultTranslation" to shouldUseDefaultTranslation,
                )
            )
            .await()
    }

    suspend fun delete(category: Category) {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        db.document(FirestorePaths.category(householdId, category.id)).delete().await()
    }

    fun getAllCategories(): Flow<List<Category>> {
        val db = firestore ?: return flowOf(emptyList())
        return sessionRepository.sessionState.flatMapLatest { sessionState ->
            val readyState = sessionState as? SessionState.Ready
            if (readyState == null) {
                flowOf(emptyList())
            } else {
                val householdId = readyState.household.id
                firestoreFlow<List<Category>> { emit ->
                    db.collection(FirestorePaths.categories(householdId))
                        .orderBy("name", Query.Direction.ASCENDING)
                        .addSnapshotListener { snapshot, error ->
                            when {
                                error != null -> emit(Result.failure(error))
                                snapshot != null -> emit(
                                    Result.success(
                                        snapshot.documents.mapNotNull { document ->
                                            document.toObject(CategoryDocument::class.java)?.let { categoryDocument ->
                                                val inferredSystemKey =
                                                    categoryDocument.systemKey
                                                        ?: SystemCategories.inferSystemKey(
                                                            context = appContext,
                                                            name = categoryDocument.name,
                                                            emoji = categoryDocument.icon,
                                                        )
                                                val usesDefaultTranslation =
                                                    categoryDocument.usesDefaultTranslation
                                                        ?: SystemCategories.shouldUseDefaultTranslation(
                                                            context = appContext,
                                                            key = inferredSystemKey,
                                                            name = categoryDocument.name,
                                                        )
                                                Category(
                                                    id = document.id,
                                                    name = SystemCategories.displayName(
                                                        context = appContext,
                                                        storedName = categoryDocument.name,
                                                        systemKey = inferredSystemKey,
                                                        usesDefaultTranslation = usesDefaultTranslation,
                                                    ),
                                                    emoji = categoryDocument.icon ?: "📦",
                                                    type = categoryDocument.type,
                                                    systemKey = inferredSystemKey,
                                                    usesDefaultTranslation = usesDefaultTranslation,
                                                )
                                            }
                                        }
                                    )
                                )
                            }
                        }
                }.map { result -> result.getOrElse { emptyList() } }
            }
        }
    }

    suspend fun count(): Int = getAllCategories()
        .map { categories -> categories.size }
        .first()

    private fun encodeCategoryId(name: String): String =
        URLEncoder.encode(name.trim().lowercase(), StandardCharsets.UTF_8.toString())
}
