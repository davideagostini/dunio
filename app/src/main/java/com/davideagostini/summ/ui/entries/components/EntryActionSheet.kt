package com.davideagostini.summ.ui.entries.components

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.domain.model.EntryDisplayItem
import com.davideagostini.summ.ui.entries.EntriesEvent
import com.davideagostini.summ.ui.entries.EntriesUiState
import com.davideagostini.summ.ui.entries.EntrySheetMode
import com.davideagostini.summ.ui.format.formatEuro
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import com.davideagostini.summ.ui.theme.listItemShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun EntryActionSheet(
    uiState: EntriesUiState,
    categories: List<Category>,
    onEvent: (EntriesEvent) -> Unit,
    onDismiss: () -> Unit,
    fullScreen: Boolean = false,
) {
    val containerModifier = if (fullScreen) {
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .navigationBarsPadding()
    } else {
        Modifier
            .fillMaxWidth()
            .then(if (uiState.sheetMode == EntrySheetMode.Edit) Modifier.fillMaxHeight(0.94f) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding()
    }

    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp).systemBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.content_desc_close))
                }
            }
            AnimatedContent(
                targetState    = uiState.sheetMode,
                transitionSpec = {
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
                    EntrySheetMode.Action  -> ActionContent(
                        entry   = uiState.selectedEntry ?: return@AnimatedContent,
                        onEdit  = { onEvent(EntriesEvent.StartEdit) },
                        onDelete = { onEvent(EntriesEvent.RequestDelete) },
                    )
                    EntrySheetMode.Edit    -> EntryEditForm(
                        uiState    = uiState,
                        categories = categories,
                        onEvent    = onEvent,
                        onCancel   = onDismiss,
                    )
                    EntrySheetMode.Success -> EntrySuccessContent()
                    EntrySheetMode.Hidden  -> Unit
                }
            }
        }
    }

    if (fullScreen) {
        Surface(modifier = containerModifier, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            content()
        }
    } else {
        Card(
            modifier  = containerModifier,
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            content()
        }
    }
}

// ── Action view ───────────────────────────────────────────────────────────────

@Composable
private fun ActionContent(
    entry: EntryDisplayItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val isIncome   = entry.type == "income"
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val sign        = if (isIncome) "+" else "-"

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text  = "$sign${formatEuro(entry.price)}",
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
            OutlinedButton(
                onClick  = onDelete,
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

// ── Edit form ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryEditForm(
    uiState: EntriesUiState,
    categories: List<Category>,
    onEvent: (EntriesEvent) -> Unit,
    onCancel: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.editDate)

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Text(stringResource(R.string.entries_edit_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
            }
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "income" to stringResource(R.string.entry_type_income),
                        "expense" to stringResource(R.string.entry_type_expense),
                    ).forEach { (value, label) ->
                        val selected = uiState.editType == value
                        val color    = if (value == "income") IncomeGreen else ExpenseRed
                        OutlinedButton(
                            onClick  = { onEvent(EntriesEvent.UpdateType(value)) },
                            shape    = AppButtonShape,
                            modifier = Modifier.weight(1f),
                            colors   = if (selected) ButtonDefaults.outlinedButtonColors(
                                containerColor = color.copy(alpha = 0.12f),
                                contentColor   = color,
                            ) else ButtonDefaults.outlinedButtonColors(),
                            border   = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, color)
                            else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            item {
                OutlinedTextField(
                    value          = uiState.editDescription,
                    onValueChange  = { onEvent(EntriesEvent.UpdateDescription(it)) },
                    label          = { Text(stringResource(R.string.entry_description_label)) },
                    isError        = uiState.descriptionError != null,
                    supportingText = uiState.descriptionError?.let { msg -> { Text(msg) } },
                    singleLine     = true,
                    shape          = RoundedCornerShape(12.dp),
                    modifier       = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                OutlinedTextField(
                    value          = uiState.editPrice,
                    onValueChange  = { onEvent(EntriesEvent.UpdatePrice(it)) },
                    label          = { Text(stringResource(R.string.entry_amount_label)) },
                    isError        = uiState.priceError != null,
                    supportingText = uiState.priceError?.let { msg -> { Text(msg) } },
                    singleLine     = true,
                    shape          = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix         = { Text(stringResource(R.string.entry_currency_symbol)) },
                    modifier       = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = AppButtonShape,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = formatEditDate(uiState.editDate),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = stringResource(R.string.entry_review_category),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
            }
            items(categories.size, key = { categories[it].id }) { index ->
                val category = categories[index]
                EditCategoryRow(
                    category = category,
                    index = index,
                    count = categories.size,
                    selected = uiState.editCategory?.id == category.id,
                    onClick  = { onEvent(EntriesEvent.UpdateCategory(category)) },
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
            }
        }

        Surface(
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick  = onCancel,
                    shape    = AppButtonShape,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_cancel)) }

                Button(
                    onClick  = { onEvent(EntriesEvent.SaveEdit) },
                    shape    = AppButtonShape,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onEvent(EntriesEvent.UpdateDate(it)) }
                        showDatePicker = false
                    },
                    shape = AppButtonShape,
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }, shape = AppButtonShape) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun EditCategoryRow(
    category: Category,
    index: Int,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = listItemShape(index, count)
    val verticalPadding = when {
        count == 1         -> PaddingValues(horizontal = 0.dp, vertical = 4.dp)
        index == 0         -> PaddingValues(start = 0.dp, end = 0.dp, top = 4.dp, bottom = 1.dp)
        index == count - 1 -> PaddingValues(start = 0.dp, end = 0.dp, top = 1.dp, bottom = 4.dp)
        else               -> PaddingValues(horizontal = 0.dp, vertical = 1.dp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(verticalPadding)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = category.emoji,
                fontSize = 20.sp,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
        }
    }
}

// ── Success ───────────────────────────────────────────────────────────────────

@Composable
private fun EntrySuccessContent() {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier         = Modifier
                .size(72.dp)
                .background(IncomeGreen.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", fontSize = 36.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
        }

        Text(stringResource(R.string.done_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Text(
            text      = stringResource(R.string.entries_done_message),
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))
    }
}

private fun formatEditDate(epochMillis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
