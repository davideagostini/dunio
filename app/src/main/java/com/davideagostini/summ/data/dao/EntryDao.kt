package com.davideagostini.summ.data.dao

import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.data.firebase.FirestorePaths
import com.davideagostini.summ.data.firebase.firestoreFlow
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.data.session.SessionState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private data class TransactionDocument(
    val date: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val type: String = "expense",
    val category: String = "",
    val period: String = "",
    val recurringTransactionId: String? = null,
)

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class EntryDao @Inject constructor(
    private val firestore: FirebaseFirestore?,
    private val sessionRepository: SessionRepository,
) {

    suspend fun insert(entry: Entry) {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        val reference = db.collection(FirestorePaths.transactions(householdId)).document()
        reference.set(
            mapOf(
                "date" to epochToDate(entry.date),
                "description" to entry.description.trim(),
                "amount" to entry.price,
                "type" to entry.type,
                "category" to entry.category,
                "period" to resolvePeriod(entry),
                "recurringTransactionId" to entry.recurringTransactionId,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
        ).await()
    }

    suspend fun update(entry: Entry) {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        db.document(FirestorePaths.transaction(householdId, entry.id))
            .update(
                mapOf(
                    "date" to epochToDate(entry.date),
                    "description" to entry.description.trim(),
                    "amount" to entry.price,
                    "type" to entry.type,
                    "category" to entry.category,
                    "period" to resolvePeriod(entry),
                    "recurringTransactionId" to entry.recurringTransactionId,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                )
            )
            .await()
    }

    suspend fun delete(entry: Entry) {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        db.document(FirestorePaths.transaction(householdId, entry.id)).delete().await()
    }

    fun getAllEntries(): Flow<List<Entry>> {
        val db = firestore ?: return flowOf(emptyList())
        return sessionRepository.sessionState.flatMapLatest { sessionState ->
            val readyState = sessionState as? SessionState.Ready
            if (readyState == null) {
                flowOf(emptyList())
            } else {
                val householdId = readyState.household.id
                firestoreFlow<List<Entry>> { emit ->
                    db.collection(FirestorePaths.transactions(householdId))
                        .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            when {
                                error != null -> emit(Result.failure(error))
                                snapshot != null -> emit(
                                    Result.success(
                                        snapshot.documents.mapNotNull { document ->
                                            document.toObject(TransactionDocument::class.java)?.toEntry(
                                                id = document.id,
                                                householdId = householdId,
                                            )
                                        }
                                    )
                                )
                            }
                        }
                }.map { result -> result.getOrElse { emptyList() } }
            }
        }
    }

    fun getBalance(): Flow<Double> =
        getAllEntries().map { entries ->
            entries.sumOf { entry -> if (entry.type == "income") entry.price else -entry.price }
        }

    private fun TransactionDocument.toEntry(id: String, householdId: String): Entry =
        Entry(
            id = id,
            householdId = householdId,
            type = type,
            description = description,
            price = amount,
            category = category,
            date = dateToEpoch(date),
            period = period.ifBlank { date.take(7) },
            recurringTransactionId = recurringTransactionId,
        )

    private fun resolvePeriod(entry: Entry): String =
        entry.period.ifBlank { epochToDate(entry.date).take(7) }

    private fun epochToDate(epochMillis: Long): String =
        java.time.Instant.ofEpochMilli(epochMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()

    private fun dateToEpoch(value: String): Long =
        runCatching {
            java.time.LocalDate.parse(value)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrElse { System.currentTimeMillis() }
}
