package com.davideagostini.summ.ui.entry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.auth.components.AuthErrorCard
import com.davideagostini.summ.ui.entry.EntryEvent
import com.davideagostini.summ.ui.entry.EntryUiState
import com.davideagostini.summ.ui.format.formatCurrency
import com.davideagostini.summ.ui.format.parseAmount
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun StepReview(
    uiState: EntryUiState,
    currency: String,
    onEvent: (EntryEvent) -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StepTitle(stringResource(R.string.entry_review_title))
        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                ReviewRow(label = stringResource(R.string.entry_review_type)) {
                    val isIncome = uiState.type == "income"
                    Text(
                        text       = if (isIncome) stringResource(R.string.entry_type_income) else stringResource(R.string.entry_type_expense),
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isIncome) IncomeGreen else ExpenseRed,
                    )
                }
                ReviewDivider()
                ReviewRow(label = stringResource(R.string.entry_review_date)) {
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(uiState.date)),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
                ReviewDivider()
                ReviewRow(label = stringResource(R.string.entry_review_description)) {
                    Text(uiState.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
                ReviewDivider()
                ReviewRow(label = stringResource(R.string.entry_review_amount)) {
                    Text(
                        text       = formatCurrency(parseAmount(uiState.price) ?: 0.0, currency),
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                ReviewDivider()
                ReviewRow(label = stringResource(R.string.entry_review_category)) {
                    val cat = uiState.selectedCategory
                    if (cat != null) {
                        Text("${cat.emoji}  ${cat.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        uiState.operationErrorMessage?.let { message ->
            AuthErrorCard(message)
            Spacer(Modifier.height(16.dp))
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick  = onCancel,
                enabled  = !uiState.isSaving,
                shape    = AppButtonShape,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.action_cancel)) }

            Button(
                onClick  = { onEvent(EntryEvent.Save) },
                enabled  = !uiState.isSaving,
                shape    = AppButtonShape,
                modifier = Modifier.weight(1f),
            ) {
                SaveActionContent(
                    label = stringResource(R.string.action_confirm),
                    isSaving = uiState.isSaving,
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text     = label,
            style    = MaterialTheme.typography.labelMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        content()
    }
}

@Composable
private fun ReviewDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun SaveActionContent(
    label: String,
    isSaving: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(label)
    }
}
