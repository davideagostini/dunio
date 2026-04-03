package com.davideagostini.summ.data.dao

import com.davideagostini.summ.data.entity.Asset
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.data.firebase.FirestorePaths
import com.davideagostini.summ.data.firebase.firestoreFlow
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.data.session.SessionState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private data class AssetDocument(
    val name: String = "",
    val type: String = "asset",
    val category: String = "",
    val value: Double = 0.0,
    val currency: String = "EUR",
    val liquid: Boolean = false,
    val period: String = "",
    val snapshotDate: String = "",
)

private data class AssetHistoryDocument(
    val assetId: String = "",
    val householdId: String = "",
    val action: String = "updated",
    val name: String = "",
    val type: String = "asset",
    val category: String = "",
    val value: Double = 0.0,
    val currency: String = "EUR",
    val liquid: Boolean = false,
    val period: String = "",
    val snapshotDate: String = "",
)

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
/**
 * Firestore access layer for current assets and monthly asset history snapshots.
 *
 * The DAO encapsulates the dual-write asset model used by the app: current values live in the
 * `assets` collection, while month snapshots are mirrored into `assets/{assetId}/history`.
 * UI layers consume month-scoped history queries from here instead of walking the full history.
 */
class AssetDao @Inject constructor(
    private val firestore: FirebaseFirestore?,
    private val sessionRepository: SessionRepository,
) {
    companion object {
        private const val ASSET_HISTORY_SYNC_BATCH_SIZE = 12L
    }


    suspend fun insert(asset: Asset) {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        val assetId = asset.id.ifBlank { db.collection(FirestorePaths.assets(householdId)).document().id }
        upsertAssetSnapshot(
            db = db,
            householdId = householdId,
            assetId = assetId,
            action = "created",
            asset = asset,
        )
    }

    suspend fun update(asset: Asset) {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        upsertAssetSnapshot(
            db = db,
            householdId = householdId,
            assetId = asset.id,
            action = "updated",
            asset = asset,
        )
    }

    suspend fun delete(asset: Asset) {
        val db = requireNotNull(firestore) { "Firestore is not available." }
        val householdId = sessionRepository.requireHouseholdId()
        val snapshotDate = asset.snapshotDate.ifBlank { monthToSnapshotDate(asset.period.ifBlank { currentMonth() }) }
        val period = snapshotDate.take(7)
        db.document(FirestorePaths.assetHistoryEntry(householdId, asset.id, period))
            .set(
                mapOf(
                    "assetId" to asset.id,
                    "householdId" to householdId,
                    "action" to "deleted",
                    "name" to asset.name.trim(),
                    "type" to asset.type,
                    "category" to asset.category.trim(),
                    "value" to asset.value,
                    "currency" to asset.currency.trim().ifBlank { "EUR" },
                    "liquid" to asset.liquid,
                    "period" to period,
                    "snapshotDate" to snapshotDate,
                    "recordedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                )
            )
            .await()
        syncCurrentAssetFromHistory(db, householdId, asset.id)
    }

    fun getAllAssetHistory(): Flow<List<AssetHistoryEntry>> {
        val db = firestore ?: return flowOf(emptyList())
        return sessionRepository.sessionState.flatMapLatest { sessionState ->
            val readyState = sessionState as? SessionState.Ready
            if (readyState == null) {
                flowOf(emptyList())
            } else {
                val householdId = readyState.household.id
                firestoreFlow<List<AssetHistoryEntry>> { emit ->
                    db.collectionGroup("history")
                        .whereEqualTo("householdId", householdId)
                        .addSnapshotListener { snapshot, error ->
                            when {
                                error != null -> emit(Result.failure(error))
                                snapshot != null -> emit(
                                    Result.success(
                                        snapshot.documents.mapNotNull { document ->
                                            document.toObject(AssetHistoryDocument::class.java)?.toHistoryEntry(document.id)
                                        }
                                    )
                                )
                            }
                        }
                }.map { result -> result.getOrElse { emptyList() } }
            }
        }
    }

    fun getAssetHistoryForMonth(period: String): Flow<List<AssetHistoryEntry>> {
        val db = firestore ?: return flowOf(emptyList())
        return sessionRepository.sessionState.flatMapLatest { sessionState ->
            val readyState = sessionState as? SessionState.Ready
            if (readyState == null) {
                flowOf(emptyList())
            } else {
                val householdId = readyState.household.id
                firestoreFlow<List<AssetHistoryEntry>> { emit ->
                    db.collectionGroup("history")
                        .whereEqualTo("householdId", householdId)
                        .whereEqualTo("period", period)
                        .addSnapshotListener { snapshot, error ->
                            when {
                                error != null -> emit(Result.failure(error))
                                snapshot != null -> emit(
                                    Result.success(
                                        snapshot.documents.mapNotNull { document ->
                                            document.toObject(AssetHistoryDocument::class.java)?.toHistoryEntry(document.id)
                                        },
                                    ),
                                )
                            }
                        }
                }.map { result -> result.getOrElse { emptyList() } }
            }
        }
    }

    fun getHasAnyAssetHistory(): Flow<Boolean> {
        val db = firestore ?: return flowOf(false)
        return sessionRepository.sessionState.flatMapLatest { sessionState ->
            val readyState = sessionState as? SessionState.Ready
            if (readyState == null) {
                flowOf(false)
            } else {
                val householdId = readyState.household.id
                firestoreFlow<Boolean> { emit ->
                    db.collectionGroup("history")
                        .whereEqualTo("householdId", householdId)
                        .limit(1)
                        .addSnapshotListener { snapshot, error ->
                            when {
                                error != null -> emit(Result.failure(error))
                                snapshot != null -> emit(Result.success(!snapshot.isEmpty))
                            }
                        }
                }.map { result -> result.getOrElse { false } }
            }
        }
    }

    private suspend fun upsertAssetSnapshot(
        db: FirebaseFirestore,
        householdId: String,
        assetId: String,
        action: String,
        asset: Asset,
    ) {
        val snapshotDate = asset.snapshotDate.ifBlank { monthToSnapshotDate(asset.period.ifBlank { currentMonth() }) }
        val period = snapshotDate.take(7)
        db.document(FirestorePaths.assetHistoryEntry(householdId, assetId, period))
            .set(
                mapOf(
                    "assetId" to assetId,
                    "householdId" to householdId,
                    "action" to action,
                    "name" to asset.name.trim(),
                    "type" to asset.type,
                    "category" to asset.category.trim(),
                    "value" to asset.value,
                    "currency" to asset.currency.trim().ifBlank { "EUR" },
                    "liquid" to asset.liquid,
                    "period" to period,
                    "snapshotDate" to snapshotDate,
                    "recordedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                )
            )
            .await()
        syncCurrentAssetFromHistory(db, householdId, assetId)
    }

    private suspend fun syncCurrentAssetFromHistory(
        db: FirebaseFirestore,
        householdId: String,
        assetId: String,
    ) {
        val latestActiveSnapshot = findLatestActiveSnapshot(db, householdId, assetId)

        val assetRef = db.document(FirestorePaths.asset(householdId, assetId))
        if (latestActiveSnapshot == null) {
            assetRef.delete().await()
            return
        }

        assetRef.set(
            mapOf(
                "name" to latestActiveSnapshot.name,
                "type" to latestActiveSnapshot.type,
                "category" to latestActiveSnapshot.category,
                "value" to latestActiveSnapshot.value,
                "currency" to latestActiveSnapshot.currency,
                "liquid" to latestActiveSnapshot.liquid,
                "period" to latestActiveSnapshot.period,
                "snapshotDate" to latestActiveSnapshot.snapshotDate,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            ),
            com.google.firebase.firestore.SetOptions.merge(),
        ).await()
    }

    private suspend fun findLatestActiveSnapshot(
        db: FirebaseFirestore,
        householdId: String,
        assetId: String,
    ): AssetHistoryDocument? {
        var lastDocument: QueryDocumentSnapshot? = null

        while (true) {
            var query = db.collection(FirestorePaths.assetHistory(householdId, assetId))
                .orderBy("period", Query.Direction.DESCENDING)
                .limit(ASSET_HISTORY_SYNC_BATCH_SIZE)

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            val snapshot = query.get().await()
            if (snapshot.isEmpty) {
                return null
            }

            val activeSnapshot = snapshot.documents
                .mapNotNull { it.toObject(AssetHistoryDocument::class.java) }
                .firstOrNull { it.action != "deleted" }

            if (activeSnapshot != null) {
                return activeSnapshot
            }

            lastDocument = snapshot.documents.last() as? QueryDocumentSnapshot ?: return null
        }
    }

    private fun AssetHistoryDocument.toHistoryEntry(documentId: String): AssetHistoryEntry =
        AssetHistoryEntry(
            id = documentId,
            assetId = assetId,
            householdId = householdId,
            action = action,
            name = name,
            type = type,
            category = category,
            value = value,
            currency = currency,
            liquid = liquid,
            period = period,
            snapshotDate = snapshotDate,
        )

    private fun monthToSnapshotDate(month: String): String =
        java.time.YearMonth.parse(month)
            .atEndOfMonth()
            .toString()

    private fun currentMonth(): String = java.time.YearMonth.now().toString()
}
