package com.davideagostini.summ.data.dao

import com.davideagostini.summ.data.entity.MonthClose
import com.davideagostini.summ.data.firebase.FirestoreExecutors
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

private data class MonthCloseDocument(
    val period: String = "",
    val status: String = "draft",
    val assetSnapshotCount: Int = 0,
    val transactionCount: Int = 0,
    val recurringMissingCount: Int = 0,
    val closedBy: String? = null,
)

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
/**
 * Firestore access layer for month-close markers.
 *
 * Month close documents define which months are frozen/read-only and are observed by several
 * screens to prevent edits after a close has been confirmed.
 */
class MonthCloseDao @Inject constructor(
    private val firestore: FirebaseFirestore?,
    private val sessionRepository: SessionRepository,
) {
    fun getAllMonthCloses(): Flow<List<MonthClose>> {
        val db = firestore ?: return flowOf(emptyList())
        return sessionRepository.sessionState.flatMapLatest { state ->
            val readyState = state as? SessionState.Ready
            if (readyState == null) {
                flowOf(emptyList())
            } else {
                firestoreFlow<List<MonthClose>> { emit ->
                    db.collection(FirestorePaths.monthCloses(readyState.household.id))
                        .addSnapshotListener(FirestoreExecutors.listenerExecutor) { snapshot, error ->
                            when {
                                error != null -> emit(Result.failure(error))
                                snapshot != null -> emit(
                                    Result.success(
                                        snapshot.documents.mapNotNull { document ->
                                            document.toObject(MonthCloseDocument::class.java)?.toMonthClose(document.id)
                                        }
                                    )
                                )
                            }
                        }
                }.map { result -> result.getOrElse { emptyList() } }
            }
        }
    }

    suspend fun upsertMonthClose(
        period: String,
        status: String,
        assetSnapshotCount: Int,
        transactionCount: Int,
        recurringMissingCount: Int,
    ) {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        val userId = sessionRepository.requireFirebaseUser().uid
        db.document(FirestorePaths.monthClose(householdId, period))
            .set(
                mapOf(
                    "period" to period,
                    "status" to status,
                    "assetSnapshotCount" to assetSnapshotCount,
                    "transactionCount" to transactionCount,
                    "recurringMissingCount" to recurringMissingCount,
                    "closedBy" to userId,
                    "closedAt" to if (status == "closed") com.google.firebase.firestore.FieldValue.serverTimestamp() else null,
                    "reopenedAt" to if (status == "draft") com.google.firebase.firestore.FieldValue.serverTimestamp() else null,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
                com.google.firebase.firestore.SetOptions.merge(),
            ).await()
    }

    private fun MonthCloseDocument.toMonthClose(id: String): MonthClose =
        MonthClose(
            id = id,
            period = period,
            status = status,
            assetSnapshotCount = assetSnapshotCount,
            transactionCount = transactionCount,
            recurringMissingCount = recurringMissingCount,
            closedBy = closedBy,
        )
}
