package com.davideagostini.summ.ui.assets

import androidx.compose.runtime.Immutable
import com.davideagostini.summ.data.entity.Asset
import com.davideagostini.summ.data.entity.AssetHistoryEntry
import com.davideagostini.summ.ui.format.formatAmount
import com.davideagostini.summ.ui.format.formatEuro
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Immutable
data class AssetListItem(
    val asset: Asset,
    val change: Double?,
)

@Immutable
data class AssetsRenderState(
    val selectedMonth: String,
    val isMonthClosed: Boolean,
    val filteredAssets: List<AssetListItem>,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double,
    val canCopyPreviousMonth: Boolean,
    val hasAnyAssets: Boolean,
)

fun formatCurrency(value: Double, currency: String = "EUR"): String =
    when (currency.uppercase()) {
        "EUR" -> formatEuro(value)
        else -> "$currency ${formatAmount(value)}"
    }

fun buildAssetsSnapshotForMonth(entries: List<AssetHistoryEntry>, month: String): List<Asset> =
    entries
        .filter { it.period == month }
        .sortedWith(
            compareByDescending<AssetHistoryEntry> { it.period }
                .thenByDescending { it.snapshotDate }
        )
        .associateBy { it.name.trim().lowercase(Locale.getDefault()) }
        .values
        .filter { it.action != "deleted" }
        .map { entry ->
            Asset(
                id = entry.assetId,
                householdId = entry.householdId,
                name = entry.name,
                type = entry.type,
                category = entry.category,
                value = entry.value,
                currency = entry.currency,
                liquid = entry.liquid,
                period = entry.period,
                snapshotDate = entry.snapshotDate,
            )
        }
        .sortedByDescending { it.value }

fun formatMonthLabel(month: String): String =
    YearMonth.parse(month).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))

fun calculateAssetChange(
    historyEntries: List<AssetHistoryEntry>,
    assetName: String,
    month: String,
): Double? {
    val normalizedName = assetName.trim().lowercase(Locale.getDefault())
    val currentEntry = historyEntries.find { entry ->
        entry.name.trim().lowercase(Locale.getDefault()) == normalizedName &&
            entry.period == month &&
            entry.action != "deleted"
    }

    val previousEntry = historyEntries
        .filter { entry ->
            entry.name.trim().lowercase(Locale.getDefault()) == normalizedName &&
                entry.period < month &&
                entry.action != "deleted"
        }
        .sortedWith(
            compareByDescending<AssetHistoryEntry> { it.period }
                .thenByDescending { it.snapshotDate }
        )
        .firstOrNull()

    if (currentEntry == null || previousEntry == null || previousEntry.value == 0.0) {
        return null
    }

    return (currentEntry.value - previousEntry.value) / abs(previousEntry.value)
}

fun buildChangeLabel(change: Double): String =
    when {
        abs(change) < 0.0005 -> "0.0%"
        else -> "${if (change > 0) "+" else ""}${"%.1f".format(change * 100)}%"
    }

internal fun normalizeAssetName(value: String): String = value.trim().lowercase(Locale.getDefault())
