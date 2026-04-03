package com.davideagostini.summ.data.repository

import android.content.Context
import android.net.Uri
import com.davideagostini.summ.BuildConfig
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.AppUser
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.Entry
import com.davideagostini.summ.data.entity.Household
import com.davideagostini.summ.data.entity.Invite
import com.davideagostini.summ.data.entity.Member
import com.davideagostini.summ.data.entity.MonthClose
import com.davideagostini.summ.data.entity.RecurringTransaction
import com.davideagostini.summ.data.session.SessionRepository
import com.davideagostini.summ.data.session.SessionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * Builds export payloads from the app's source-of-truth streams.
 *
 * Export intentionally reads the full transaction and asset history because its job is archival,
 * not screen rendering. Keeping that broad query surface isolated here prevents UI regressions.
 */
class ExportRepository @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val sessionRepository: SessionRepository,
    private val entryRepository: EntryRepository,
    private val assetRepository: AssetRepository,
    private val categoryRepository: CategoryRepository,
    private val memberRepository: MemberRepository,
    private val recurringRepository: RecurringTransactionRepository,
    private val monthCloseRepository: MonthCloseRepository,
) {

    suspend fun exportHouseholdBackupJson(uri: Uri) {
        val readyState = currentReadyState()
        val categories = categoryRepository.allCategories.first().sortedBy { it.name.lowercase() }
        val members = memberRepository.allMembers.first().sortedBy { it.name.ifBlank { it.email.ifBlank { it.userId } }.lowercase() }
        val invites = memberRepository.allInvites.first().sortedBy { it.email.lowercase() }
        val entries = entryRepository.allEntries.first().sortedBy { it.date }
        val assets = assetRepository.allAssetHistory.first().sortedWith(compareBy<AssetHistoryEntry>({ it.period }, { it.name.lowercase() }, { it.snapshotDate }))
        val recurring = recurringRepository.allRecurringTransactions.first().sortedBy { it.description.lowercase() }
        val monthCloses = monthCloseRepository.allMonthCloses.first().sortedBy { it.period }

        val payload = JSONObject()
            .put(
                "metadata",
                JSONObject()
                    .put("formatVersion", 1)
                    .put("exportedAt", Instant.now().atOffset(ZoneOffset.UTC).toString())
                    .put("appVersion", BuildConfig.VERSION_NAME)
                    .put("householdId", readyState.household.id)
            )
            .put("household", readyState.household.toJson())
            .put("currentUser", readyState.user.toJson())
            .put("members", JSONArray(members.map(Member::toJson)))
            .put("invites", JSONArray(invites.map(Invite::toJson)))
            .put("categories", JSONArray(categories.map(Category::toJson)))
            .put("entries", JSONArray(entries.map(Entry::toJson)))
            .put("assets", JSONArray(assets.map(AssetHistoryEntry::toJson)))
            .put("recurringTransactions", JSONArray(recurring.map(RecurringTransaction::toJson)))
            .put("monthCloses", JSONArray(monthCloses.map(MonthClose::toJson)))

        writeText(uri, payload.toString(2))
    }

    suspend fun exportEntriesCsv(uri: Uri) {
        val entries = entryRepository.allEntries.first().sortedBy { it.date }
        val csv = buildString {
            appendLine("id,date,period,type,description,amount,category,category_key,recurring_transaction_id")
            entries.forEach { entry ->
                appendCsvRow(
                    entry.id,
                    entry.date.toIsoDate(),
                    entry.period.ifBlank { entry.date.toMonthKey() },
                    entry.type,
                    entry.description,
                    entry.price.toString(),
                    entry.category,
                    entry.categoryKey.orEmpty(),
                    entry.recurringTransactionId.orEmpty(),
                )
            }
        }
        writeText(uri, csv)
    }

    suspend fun exportAssetsCsv(uri: Uri) {
        val assets = assetRepository.allAssetHistory.first()
            .sortedWith(compareBy<AssetHistoryEntry>({ it.period }, { it.name.lowercase() }, { it.snapshotDate }))
        val csv = buildString {
            appendLine("history_id,asset_id,action,period,snapshot_date,name,type,category,value,currency,liquid")
            assets.forEach { asset ->
                appendCsvRow(
                    asset.id,
                    asset.assetId,
                    asset.action,
                    asset.period,
                    asset.snapshotDate,
                    asset.name,
                    asset.type,
                    asset.category,
                    asset.value.toString(),
                    asset.currency,
                    asset.liquid.toString(),
                )
            }
        }
        writeText(uri, csv)
    }

    private suspend fun currentReadyState(): SessionState.Ready =
        sessionRepository.sessionState
            .filterIsInstance<SessionState.Ready>()
            .first()

    private suspend fun writeText(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        val outputStream = appContext.contentResolver.openOutputStream(uri)
            ?: error(appContext.getString(R.string.settings_export_error_write_failed))
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(text)
        }
    }
}

private fun Household.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("name", name)
        .put("ownerId", ownerId)
        .put("currency", currency)

private fun AppUser.toJson(): JSONObject =
    JSONObject()
        .put("uid", uid)
        .put("email", email)
        .put("name", name)
        .put("photoUrl", photoUrl)
        .put("householdId", householdId)

private fun Member.toJson(): JSONObject =
    JSONObject()
        .put("userId", userId)
        .put("role", role)
        .put("name", name)
        .put("email", email)
        .put("photoUrl", photoUrl)

private fun Invite.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("email", email)
        .put("role", role)
        .put("status", status)

private fun Category.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("name", name)
        .put("emoji", emoji)
        .put("type", type)
        .put("systemKey", systemKey)
        .put("usesDefaultTranslation", usesDefaultTranslation)

private fun Entry.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("householdId", householdId)
        .put("type", type)
        .put("description", description)
        .put("price", price)
        .put("category", category)
        .put("categoryKey", categoryKey)
        .put("date", date)
        .put("dateIso", date.toIsoDate())
        .put("period", period.ifBlank { date.toMonthKey() })
        .put("recurringTransactionId", recurringTransactionId)

private fun AssetHistoryEntry.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("assetId", assetId)
        .put("householdId", householdId)
        .put("action", action)
        .put("name", name)
        .put("type", type)
        .put("category", category)
        .put("value", value)
        .put("currency", currency)
        .put("liquid", liquid)
        .put("period", period)
        .put("snapshotDate", snapshotDate)

private fun RecurringTransaction.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("description", description)
        .put("amount", amount)
        .put("type", type)
        .put("category", category)
        .put("categoryKey", categoryKey)
        .put("dayOfMonth", dayOfMonth)
        .put("startDate", startDate)
        .put("active", active)
        .put("lastAppliedDate", lastAppliedDate)

private fun MonthClose.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("period", period)
        .put("status", status)
        .put("assetSnapshotCount", assetSnapshotCount)
        .put("transactionCount", transactionCount)
        .put("recurringMissingCount", recurringMissingCount)
        .put("closedBy", closedBy)

private fun StringBuilder.appendCsvRow(vararg values: String) {
    appendLine(values.joinToString(",") { value -> "\"${value.escapeCsv()}\"" })
}

private fun String.escapeCsv(): String = replace("\"", "\"\"")

private fun Long.toIsoDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()

private fun Long.toMonthKey(): String = toIsoDate().take(7)
