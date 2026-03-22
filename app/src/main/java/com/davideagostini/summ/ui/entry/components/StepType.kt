package com.davideagostini.summ.ui.entry.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.entry.EntryEvent
import com.davideagostini.summ.ui.entry.EntryUiState
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen

@Composable
internal fun StepType(uiState: EntryUiState, onEvent: (EntryEvent) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StepTitle(stringResource(R.string.entry_step_type_title))
        Spacer(Modifier.height(20.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TypeCard(
                label    = stringResource(R.string.entry_type_expense_plain),
                emoji    = "💸",
                selected = uiState.type == "expense",
                color    = ExpenseRed,
                modifier = Modifier.weight(1f),
                onClick  = { onEvent(EntryEvent.SelectType("expense")) },
            )
            TypeCard(
                label    = stringResource(R.string.entry_type_income_plain),
                emoji    = "💰",
                selected = uiState.type == "income",
                color    = IncomeGreen,
                modifier = Modifier.weight(1f),
                onClick  = { onEvent(EntryEvent.SelectType("income")) },
            )
        }

        Spacer(Modifier.height(24.dp))
        StepNavRow(onBack = null, onNext = { onEvent(EntryEvent.Next) })
    }
}

@Composable
private fun TypeCard(
    label: String,
    emoji: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) color else MaterialTheme.colorScheme.outlineVariant
    val bgColor     = if (selected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLowest

    Surface(
        modifier  = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(16.dp),
        color     = bgColor,
    ) {
        Column(
            modifier              = Modifier.padding(vertical = 20.dp),
            horizontalAlignment   = Alignment.CenterHorizontally,
            verticalArrangement   = Arrangement.spacedBy(8.dp),
        ) {
            Text(emoji, fontSize = 32.sp)
            Text(
                text       = label,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color      = if (selected) color else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
