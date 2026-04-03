package com.davideagostini.summ.data.dao

import android.content.Context
import com.davideagostini.summ.data.category.SystemCategories
import com.davideagostini.summ.data.entity.RecurringTransaction
import com.davideagostini.summ.data.firebase.FirestoreExecutors
import com.davideagostini.summ.data.firebase.FirestorePaths
import com.davideagostini.summ.data.firebase.firestoreFlow
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.data.session.SessionState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

private data class RecurringTransactionDocument(
    val description: String = "",
    val amount: Double = 0.0,
    val type: String = "expense",
    val category: String = "",
    val categoryKey: String? = null,
    val frequency: String = "monthly",
    val dayOfMonth: Int = 1,
    val startDate: String = "",
    val active: Boolean = true,
    val lastAppliedDate: String? = null,
)

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
/**
 * Firestore access layer for recurring transactions.
 *
 * This DAO owns the recurring schedule documents that feed month close suggestions and the
 * recurring settings screen.
 */
class RecurringTransactionDao @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val firestore: FirebaseFirestore?,
    private val sessionRepository: SessionRepository,
) {
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> {
        val db = firestore ?: return flowOf(emptyList())
        return sessionRepository.sessionState.flatMapLatest { state ->
            val readyState = state as? SessionState.Ready
            if (readyState == null) {
                flowOf(emptyList())
            } else {
                firestoreFlow<List<RecurringTransaction>> { emit ->
                    db.collection(FirestorePaths.recurringTransactions(readyState.household.id))
                        .orderBy("description", Query.Direction.ASCENDING)
                        .addSnapshotListener(FirestoreExecutors.listenerExecutor) { snapshot, error ->
                            when {
                                error != null -> emit(Result.failure(error))
                                snapshot != null -> emit(
                                    Result.success(
                                        snapshot.documents.mapNotNull { document ->
                                            document.toObject(RecurringTransactionDocument::class.java)?.toRecurring(document.id)
                                        }
                                    )
                                )
                            }
                        }
                }.map { result -> result.getOrElse { emptyList() } }
            }
        }
    }

    suspend fun insert(recurring: RecurringTransaction) {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        val ref = if (recurring.id.isBlank()) {
            db.collection(FirestorePaths.recurringTransactions(householdId)).document()
        } else {
            db.document(FirestorePaths.recurringTransaction(householdId, recurring.id))
        }
        ref.set(
            mapOf(
                "description" to recurring.description.trim(),
                "amount" to recurring.amount,
                "type" to recurring.type,
                "category" to recurring.category,
                "categoryKey" to recurring.categoryKey,
                "frequency" to "monthly",
                "dayOfMonth" to recurring.dayOfMonth,
                "startDate" to recurring.startDate,
                "active" to recurring.active,
                "lastAppliedDate" to recurring.lastAppliedDate,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
        ).await()
    }

    suspend fun update(recurring: RecurringTransaction) {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        db.document(FirestorePaths.recurringTransaction(householdId, recurring.id))
            .update(
                mapOf(
                    "description" to recurring.description.trim(),
                    "amount" to recurring.amount,
                    "type" to recurring.type,
                    "category" to recurring.category,
                    "categoryKey" to recurring.categoryKey,
                    "dayOfMonth" to recurring.dayOfMonth,
                    "startDate" to recurring.startDate,
                    "active" to recurring.active,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                )
            ).await()
    }

    suspend fun delete(recurringId: String) {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        db.document(FirestorePaths.recurringTransaction(householdId, recurringId)).delete().await()
    }

    suspend fun applyDueRecurringTransactions(recurringTransactions: List<RecurringTransaction>): Int {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        val today = LocalDate.now()
        val currentMonthKey = today.toString().take(7)
        var createdCount = 0
        val existingTransactionsSnapshot = db.collection(FirestorePaths.transactions(householdId))
            .whereEqualTo("period", currentMonthKey)
            .get()
            .await()
        val existingRecurringDates = existingTransactionsSnapshot.documents
            .mapNotNull { document ->
                val recurringId = document.getString("recurringTransactionId")?.takeIf(String::isNotBlank)
                val date = document.getString("date")?.takeIf(String::isNotBlank)
                if (recurringId == null || date == null) {
                    null
                } else {
                    recurringId to date
                }
            }
            .toMutableSet()

        recurringTransactions.forEach { recurring ->
            if (!recurring.active) return@forEach

            val dueDate = getDueDateForMonth(currentMonthKey, recurring.dayOfMonth)
            if (dueDate > today.toString() || recurring.startDate > dueDate) return@forEach

            if ((recurring.id to dueDate) in existingRecurringDates) return@forEach

            db.collection(FirestorePaths.transactions(householdId))
                .add(
                    mapOf(
                        "date" to dueDate,
                        "period" to currentMonthKey,
                        "description" to recurring.description,
                        "amount" to recurring.amount,
                        "type" to recurring.type,
                        "category" to recurring.category,
                        "categoryKey" to recurring.categoryKey,
                        "recurringTransactionId" to recurring.id,
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    )
                )
                .await()
            existingRecurringDates += recurring.id to dueDate

            db.document(FirestorePaths.recurringTransaction(householdId, recurring.id))
                .update(
                    mapOf(
                        "lastAppliedDate" to dueDate,
                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    )
                )
                .await()

            createdCount += 1
        }

        return createdCount
    }

    private fun RecurringTransactionDocument.toRecurring(id: String): RecurringTransaction =
        run {
            val resolvedCategoryKey = categoryKey ?: SystemCategories.inferSystemKey(
                context = appContext,
                name = category,
            )

            RecurringTransaction(
                id = id,
                description = description,
                amount = amount,
                type = type,
                category = category,
                categoryKey = resolvedCategoryKey,
                dayOfMonth = dayOfMonth,
                startDate = startDate,
                active = active,
                lastAppliedDate = lastAppliedDate,
            )
        }

    private fun getDueDateForMonth(monthKey: String, dayOfMonth: Int): String {
        val yearMonth = YearMonth.parse(monthKey)
        val safeDay = dayOfMonth.coerceAtMost(yearMonth.lengthOfMonth())
        return yearMonth.atDay(safeDay).toString()
    }
}
