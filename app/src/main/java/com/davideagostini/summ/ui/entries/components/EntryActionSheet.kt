package com.davideagostini.summ.ui.entries.components

// EntryActionSheet renders only the compact action/success sheet used by the feature.
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.domain.model.EntryDisplayItem
import com.davideagostini.summ.ui.auth.components.AuthErrorCard
import com.davideagostini.summ.ui.components.MonthCloseReadOnlyBanner
import com.davideagostini.summ.ui.entries.EntriesEvent
import com.davideagostini.summ.ui.entries.EntriesUiState
import com.davideagostini.summ.ui.entries.EntrySheetMode
import com.davideagostini.summ.ui.format.formatCurrency
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun EntryActionSheet(
    uiState: EntriesUiState,
    categories: List<Category>,
    currency: String,
    readOnly: Boolean,
    readOnlyMessage: String,
    onEvent: (EntriesEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    // The compact sheet only owns action and delete-success states. Edit lives in a dedicated
    // fullscreen editor, matching the assets flow.
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            // Keep the close action in the top-right corner so the sheet matches the rest of the app patterns.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = onDismiss,
                    enabled = uiState.sheetMode != EntrySheetMode.Edit || !uiState.isSaving,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.content_desc_close))
                }
            }
            // AnimatedContent preserves the previous state long enough to animate between sheet modes.
            AnimatedContent(
                targetState    = uiState.sheetMode,
                transitionSpec = {
                    // Direction-aware horizontal motion makes the sheet feel like a small state machine instead of a hard swap.
                    val forward = targetState.ordinal > initialState.ordinal
                    if (forward) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "entry_sheet_mode",
            ) { mode ->
                when (mode) {
                    // Action mode shows the summary plus the delete/edit buttons for the selected entry.
                    EntrySheetMode.Action  -> ActionContent(
                        entry   = uiState.selectedEntry ?: return@AnimatedContent,
                        currency = currency,
                        errorMessage = uiState.operationErrorMessage,
                        readOnly = readOnly,
                        readOnlyMessage = readOnlyMessage,
                        onEdit  = { onEvent(EntriesEvent.StartEdit) },
                        onDelete = { onEvent(EntriesEvent.RequestDelete) },
                    )
                    // Success stays in the same container so the user gets a stable completion state.
                    EntrySheetMode.Success -> EntrySuccessContent()
                    EntrySheetMode.Hidden,
                    EntrySheetMode.Edit -> Unit
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        content()
    }
}

// ── Action view ───────────────────────────────────────────────────────────────

@Composable
private fun ActionContent(
    entry: EntryDisplayItem,
    currency: String,
    errorMessage: String?,
    readOnly: Boolean,
    readOnlyMessage: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    // The action summary stays centered and compact so the main decision buttons remain the visual focus.
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val isIncome   = entry.type == "income"
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val sign        = if (isIncome) "+" else "-"

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        errorMessage?.let { message ->
            // Backend or validation errors are shown inline above the entry summary.
            AuthErrorCard(message)
            Spacer(Modifier.height(12.dp))
        }

        if (readOnly) {
            // Read-only months still allow inspection, but the banner explains why edits are disabled.
            MonthCloseReadOnlyBanner(readOnlyMessage)
            Spacer(Modifier.height(12.dp))
        }

        Surface(
            shape    = RoundedCornerShape(16.dp),
            color    = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(entry.emoji, fontSize = 32.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text       = entry.description,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.onSurface,
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text  = "$sign${formatCurrency(entry.price, currency)}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = amountColor,
        )

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = amountColor.copy(alpha = 0.12f),
            ) {
                Text(
                    text      = if (isIncome) stringResource(R.string.entry_type_income_plain) else stringResource(R.string.entry_type_expense_plain),
                    style     = MaterialTheme.typography.labelMedium,
                    color     = amountColor,
                    fontWeight = FontWeight.SemiBold,
                    modifier  = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }

            Text(stringResource(R.string.entries_action_group_separator), color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text(
                text  = entry.category,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(stringResource(R.string.entries_action_group_separator), color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text(
                text  = dateFormat.format(Date(entry.date)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Delete is intentionally destructive and uses the shared red palette with the other features.
            OutlinedButton(
                onClick  = onDelete,
                enabled = !readOnly,
                shape    = AppButtonShape,
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.action_delete))
            }

            Button(
                onClick  = onEdit,
                enabled = !readOnly,
                shape    = AppButtonShape,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.action_edit))
            }
        }
    }
}

// ── Success ───────────────────────────────────────────────────────────────────

@Composable
internal fun EntrySuccessContent(
    modifier: Modifier = Modifier,
) {
    // Success mirrors the assets layout so the confirm state feels consistent across feature flows.
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        // The icon, title, and message are intentionally simple so the success state reads instantly.
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(IncomeGreen.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", fontSize = 36.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
            }

            Text(
                text = stringResource(R.string.done_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text      = stringResource(R.string.entries_done_message),
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
