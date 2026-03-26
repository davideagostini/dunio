package com.davideagostini.summ.widget.data

import com.davideagostini.summ.data.firebase.FirestorePaths
import com.davideagostini.summ.ui.format.DEFAULT_CURRENCY
import com.davideagostini.summ.ui.format.normalizeCurrencyCode
import com.davideagostini.summ.widget.model.SpendingSummaryWidgetState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

private data class WidgetTransactionDocument(
    val date: String = "",
    val amount: Double = 0.0,
    val type: String = "expense",
)

@Singleton
class SpendingSummaryWidgetDataSource @Inject constructor() {
    // Widgets run outside the regular screen/ViewModel flow, so they read Firebase
    // directly instead of depending on app-only state holders.
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val zoneId: ZoneId = ZoneId.systemDefault()

    suspend fun load(): SpendingSummaryWidgetState {
        // Keep the signed-out case explicit so the widget can guide the user instead
        // of silently showing zeros.
        val currentUser = auth.currentUser ?: return SpendingSummaryWidgetState.SignedOut

        return runCatching {
            // The widget starts from the authenticated Firebase user, then resolves the
            // app-level user document because household membership lives there.
            val userSnapshot = firestore.document(FirestorePaths.user(currentUser.uid)).get().await()
            val householdId = userSnapshot.getString("householdId")?.takeIf(String::isNotBlank)
                ?: return SpendingSummaryWidgetState.NeedsHousehold
            val householdSnapshot = firestore.document(FirestorePaths.household(householdId)).get().await()
            val currency = normalizeCurrencyCode(householdSnapshot.getString("currency") ?: DEFAULT_CURRENCY)

            val today = LocalDate.now(zoneId)
            // We align the "week" window with the rest of the app expectations:
            // Monday is treated as the first day of the week.
            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val startOfMonth = today.withDayOfMonth(1)
            val startOfPreviousMonth = startOfMonth.minusMonths(1)
            val endOfPreviousMonth = startOfMonth.minusDays(1)

            // Query only from the first day of the month. This keeps the fetch bounded
            // while still covering current month plus previous month for the delta.
            // We intentionally avoid a broader history query because the widget must stay
            // cheap to refresh from launcher updates and from repository write hooks.
            val transactionSnapshots = firestore.collection(FirestorePaths.transactions(householdId))
                .whereGreaterThanOrEqualTo("date", startOfPreviousMonth.toString())
                .get()
                .await()

            // The widget intentionally measures spending only, so income is excluded here.
            val expenses = transactionSnapshots.documents.mapNotNull { document ->
                document.toObject(WidgetTransactionDocument::class.java)
            }.filter { transaction ->
                transaction.type == "expense" && transaction.date.isNotBlank()
            }

            // Compute the three windows the widget needs in advance so rendering stays
            // completely presentational and does not depend on date math inside Glance.
            val todayAmount = expenses.sumBetween(today, today)
            val weekAmount = expenses.sumBetween(startOfWeek, today)
            val monthAmount = expenses.sumBetween(startOfMonth, today)
            val previousMonthAmount = expenses.sumBetween(startOfPreviousMonth, endOfPreviousMonth)

            SpendingSummaryWidgetState.Ready(
                todayAmount = todayAmount,
                weekAmount = weekAmount,
                monthAmount = monthAmount,
                previousMonthAmount = previousMonthAmount,
                currency = currency,
            )
        }.getOrElse {
            // Widgets should fail soft: one loading error must not crash the host launcher.
            SpendingSummaryWidgetState.Error
        }
    }

    // We keep date math here instead of the composable so formatting and rendering remain
    // completely separate from Firestore parsing.
    private fun List<WidgetTransactionDocument>.sumBetween(start: LocalDate, end: LocalDate): Double =
        // Each widget transaction stores the date as ISO text. We parse defensively:
        // one malformed document must not break the entire widget.
        filter { document ->
            runCatching { LocalDate.parse(document.date) }
                .getOrNull()
                ?.let { !it.isBefore(start) && !it.isAfter(end) }
                ?: false
        }.sumOf { it.amount }
}
