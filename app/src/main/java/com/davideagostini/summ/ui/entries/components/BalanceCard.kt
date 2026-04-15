package com.davideagostini.summ.ui.entries.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.components.SummaryCardDefaults
import com.davideagostini.summ.ui.components.SummaryMetricPill
import com.davideagostini.summ.ui.entries.EntriesFilterType
import com.davideagostini.summ.ui.format.formatCurrency
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import kotlin.math.abs

@Composable
internal fun BalanceCard(
    currency: String,
    monthLabel: String,
    expenses: Double,
    income: Double,
    netCashFlow: Double,
    filterType: EntriesFilterType,
    onFilterSelected: (EntriesFilterType) -> Unit,
) {
        Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SummaryCardDefaults.outerPadding),
        shape = SummaryCardDefaults.shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
            Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SummaryCardDefaults.contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.entries_balance_spent_in, monthLabel),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(SummaryCardDefaults.titleSpacing))

            Text(
                text = formatCurrency(expenses, currency),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(SummaryCardDefaults.sectionSpacing))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SummaryCardDefaults.metricRowSpacing),
            ) {
                SummaryMetricPill(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.entries_balance_income_label),
                    value = formatCurrency(income, currency),
                )
                SummaryMetricPill(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.entries_balance_cash_flow_label),
                    value = if (netCashFlow >= 0) {
                        "+${formatCurrency(netCashFlow, currency)}"
                    } else {
                        "-${formatCurrency(abs(netCashFlow), currency)}"
                    },
                    valueColor = if (netCashFlow >= 0) IncomeGreen else ExpenseRed,
                )
            }
            Spacer(Modifier.height(SummaryCardDefaults.sectionSpacing))
            // Keep the filter segmented control visually anchored to the summary above.
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                EntriesFilterType.entries.forEachIndexed { index, item ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = EntriesFilterType.entries.size,
                        ),
                        onClick = { onFilterSelected(item) },
                        selected = filterType == item,
                        label = {
                            Text(
                                text = when (item) {
                                    EntriesFilterType.All -> stringResource(R.string.entries_filter_all)
                                    EntriesFilterType.Expenses -> stringResource(R.string.entries_filter_expenses)
                                    EntriesFilterType.Income -> stringResource(R.string.entries_filter_income)
                                },
                                style = MaterialTheme.typography.labelLarge,
                            )
                        },
                    )
                }
            }
        }
    }
}
