package com.davideagostini.summ.ui.entries.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.ui.entries.EntriesFilterType
import com.davideagostini.summ.ui.format.formatEuro
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import kotlin.math.abs

@Composable
internal fun BalanceCard(
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
            .padding(horizontal = 20.dp, vertical = 10.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Spent in $monthLabel",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatEuro(expenses),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Income ${formatEuro(income)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (netCashFlow >= 0) IncomeGreen.copy(alpha = 0.12f) else ExpenseRed.copy(alpha = 0.12f),
            ) {
                Text(
                    text = if (netCashFlow >= 0) {
                        "In the green ${formatEuro(netCashFlow)}"
                    } else {
                        "Over budget ${formatEuro(abs(netCashFlow))}"
                    },
                    color = if (netCashFlow >= 0) IncomeGreen else ExpenseRed,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                options = EntriesFilterType.entries,
                selectedOption = filterType,
                onOptionSelected = onFilterSelected,
                label = { item ->
                    when (item) {
                        EntriesFilterType.All -> "All"
                        EntriesFilterType.Expenses -> "Expenses"
                        EntriesFilterType.Income -> "Income"
                    }
                },
            )
        }
    }
}

@Composable
private fun <T> ButtonGroup(
    modifier: Modifier = Modifier,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    label: (T) -> String,
) {
    Row(modifier = modifier) {
        options.forEachIndexed { index, option ->
            val selected = option == selectedOption
            Button(
                modifier = Modifier.weight(1f),
                onClick = { onOptionSelected(option) },
                shape = buttonGroupShape(index = index, lastIndex = options.lastIndex),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    contentColor = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                ),
                border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

private fun buttonGroupShape(index: Int, lastIndex: Int): Shape =
    when (index) {
        0 -> RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 6.dp, bottomEnd = 6.dp)
        lastIndex -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp)
        else -> RoundedCornerShape(6.dp)
    }
