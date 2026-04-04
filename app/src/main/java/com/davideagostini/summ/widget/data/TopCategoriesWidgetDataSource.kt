package com.davideagostini.summ.widget.data

import android.content.Context
import com.davideagostini.summ.R
import com.davideagostini.summ.data.category.SystemCategories
import com.davideagostini.summ.data.firebase.FirestorePaths
import com.davideagostini.summ.ui.format.DEFAULT_CURRENCY
import com.davideagostini.summ.ui.format.normalizeCurrencyCode
import com.davideagostini.summ.widget.model.TopCategoriesWidgetState
import com.davideagostini.summ.widget.model.TopCategoryWidgetItem
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private data class TopCategoryTransactionDocument(
    val amount: Double,
    val type: String,
    val category: String,
    val categoryKey: String?,
)

private data class CategoryWidgetDocument(
    val name: String = "",
    val icon: String? = null,
    val systemKey: String? = null,
)

@Singleton
class TopCategoriesWidgetDataSource @Inject constructor() {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun loadCached(context: Context): TopCategoriesWidgetState? =
        TopCategoriesWidgetCache.read(context)

    suspend fun refreshAndCache(context: Context): TopCategoriesWidgetState {
        val state = loadRemote(context)
        TopCategoriesWidgetCache.write(context, state)
        return state
    }

    private suspend fun loadRemote(context: Context): TopCategoriesWidgetState {
        val currentUser = auth.currentUser ?: return TopCategoriesWidgetState.SignedOut

        return runCatching {
            val userSnapshot = firestore.document(FirestorePaths.user(currentUser.uid)).get().await()
            val householdId = userSnapshot.getString("householdId")?.takeIf(String::isNotBlank)
                ?: return TopCategoriesWidgetState.NeedsHousehold
            val householdSnapshot = firestore.document(FirestorePaths.household(householdId)).get().await()
            val currency = normalizeCurrencyCode(householdSnapshot.getString("currency") ?: DEFAULT_CURRENCY)
            val startOfMonthDate = LocalDate.now(zoneId).withDayOfMonth(1)
            val startOfMonth = startOfMonthDate.toString()
            val endExclusiveDate = startOfMonthDate.plusMonths(1).toString()
            val uncategorizedLabel = context.getString(R.string.entries_reports_uncategorized)

            val transactionSnapshots = firestore.collection(FirestorePaths.transactions(householdId))
                .whereGreaterThanOrEqualTo("date", startOfMonth)
                .whereLessThan("date", endExclusiveDate)
                .get()
                .await()
            val categorySnapshots = firestore.collection(FirestorePaths.categories(householdId))
                .get()
                .await()

            val categoryEmojiById = categorySnapshots.documents.associate { document ->
                val categoryDocument = document.toObject(CategoryWidgetDocument::class.java)
                document.id to (categoryDocument?.icon ?: "📦")
            }
            val categoryEmojiByName = categorySnapshots.documents.associateNotNull { document ->
                val categoryDocument = document.toObject(CategoryWidgetDocument::class.java) ?: return@associateNotNull null
                val label = categoryDocument.name.trim()
                if (label.isBlank()) null else label to (categoryDocument.icon ?: "📦")
            }

            val expenseDocuments = transactionSnapshots.documents
                .mapNotNull { document -> document.toTopCategoryTransaction(zoneId) }
                .filter { it.type == "expense" }

            val totalMonthExpense = expenseDocuments.sumOf(TopCategoryTransactionDocument::amount)

            val topItems = expenseDocuments
                .groupBy { document -> document.category.ifBlank { uncategorizedLabel } }
                .map { (label, items) ->
                    val totalAmount = items.sumOf(TopCategoryTransactionDocument::amount)
                    val categoryKey = items.firstOrNull()?.categoryKey
                    val emoji = when {
                        categoryKey != null && categoryEmojiById.containsKey(categoryKey) -> categoryEmojiById.getValue(categoryKey)
                        categoryEmojiByName.containsKey(label) -> categoryEmojiByName.getValue(label)
                        else -> {
                            val inferredSystemKey = categoryKey ?: SystemCategories.inferSystemKey(context, label)
                            SystemCategories.definitions.firstOrNull { it.key == inferredSystemKey }?.emoji ?: "📦"
                        }
                    }
                    Triple(label, emoji, totalAmount)
                }
                .sortedByDescending { it.third }
                .take(3)

            TopCategoriesWidgetState.Ready(
                categories = topItems.map { (label, emoji, amount) ->
                    TopCategoryWidgetItem(
                        label = label,
                        emoji = emoji,
                        amount = amount,
                        shareOfMonth = if (totalMonthExpense <= 0.0) {
                            0f
                        } else {
                            (amount / totalMonthExpense).toFloat().coerceIn(0f, 1f)
                        },
                    )
                },
                currency = currency,
            )
        }.getOrElse {
            TopCategoriesWidgetState.Error
        }
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toTopCategoryTransaction(
    zoneId: ZoneId,
): TopCategoryTransactionDocument? {
    val amount = when (val rawAmount = get("amount")) {
        is Number -> rawAmount.toDouble()
        else -> 0.0
    }
    val type = getString("type")?.takeIf(String::isNotBlank) ?: "expense"
    val category = getString("category").orEmpty()
    val categoryKey = getString("categoryKey")
    get("date").toTopCategoryLocalDateOrNull(zoneId) ?: return null
    return TopCategoryTransactionDocument(
        amount = amount,
        type = type,
        category = category,
        categoryKey = categoryKey,
    )
}

private fun Any?.toTopCategoryLocalDateOrNull(zoneId: ZoneId): LocalDate? =
    when (this) {
        is String -> runCatching { LocalDate.parse(this) }.getOrNull()
            ?: runCatching { Instant.parse(this).atZone(zoneId).toLocalDate() }.getOrNull()
        is Long -> Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
        is Int -> Instant.ofEpochMilli(this.toLong()).atZone(zoneId).toLocalDate()
        is Double -> Instant.ofEpochMilli(this.toLong()).atZone(zoneId).toLocalDate()
        is Date -> this.toInstant().atZone(zoneId).toLocalDate()
        is Timestamp -> this.toDate().toInstant().atZone(zoneId).toLocalDate()
        else -> null
    }

private inline fun <K, V> Iterable<com.google.firebase.firestore.DocumentSnapshot>.associateNotNull(
    transform: (com.google.firebase.firestore.DocumentSnapshot) -> Pair<K, V>?,
): Map<K, V> =
    buildMap {
        for (item in this@associateNotNull) {
            val pair = transform(item) ?: continue
            put(pair.first, pair.second)
        }
    }
