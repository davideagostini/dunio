package com.davideagostini.summ.ui.entries.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.davideagostini.summ.ui.auth.components.AuthErrorCard
import com.davideagostini.summ.ui.components.MonthCloseReadOnlyBanner
import com.davideagostini.summ.ui.entries.EntriesEvent
import com.davideagostini.summ.ui.entries.EntriesUiState
import com.davideagostini.summ.ui.entries.EntrySheetMode
import com.davideagostini.summ.ui.format.currencySymbol
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import com.davideagostini.summ.ui.theme.listItemShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun EntryEditorScreen(
    uiState: EntriesUiState,
    categories: List<Category>,
    currency: String,
    readOnly: Boolean,
    readOnlyMessage: String,
    onEvent: (EntriesEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .systemBarsPadding()
                .navigationBarsPadding(),
        ) {
            val closeEnabled = !uiState.isSaving
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onDismiss, enabled = closeEnabled) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.content_desc_close))
                }
            }

            when (uiState.sheetMode) {
                EntrySheetMode.Edit -> EntryEditForm(
                    uiState = uiState,
                    categories = categories,
                    currency = currency,
                    readOnly = readOnly,
                    readOnlyMessage = readOnlyMessage,
                    onEvent = onEvent,
                    onCancel = onDismiss,
                )

                EntrySheetMode.Success -> EntrySuccessContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                )

                else -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryEditForm(
    uiState: EntriesUiState,
    categories: List<Category>,
    currency: String,
    readOnly: Boolean,
    readOnlyMessage: String,
    onEvent: (EntriesEvent) -> Unit,
    onCancel: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.editDate)
    val controlsEnabled = !readOnly && !uiState.isSaving
    val cancelEnabled = !uiState.isSaving

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.entries_edit_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(16.dp))
            }
            item {
                uiState.operationErrorMessage?.let { message ->
                    AuthErrorCard(message)
                    Spacer(Modifier.height(12.dp))
                }
            }
            item {
                if (readOnly) {
                    MonthCloseReadOnlyBanner(readOnlyMessage)
                    Spacer(Modifier.height(12.dp))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "income" to stringResource(R.string.entry_type_income),
                        "expense" to stringResource(R.string.entry_type_expense),
                    ).forEach { (value, label) ->
                        val selected = uiState.editType == value
                        val color = if (value == "income") IncomeGreen else ExpenseRed
                        OutlinedButton(
                            onClick = { onEvent(EntriesEvent.UpdateType(value)) },
                            enabled = controlsEnabled,
                            shape = AppButtonShape,
                            modifier = Modifier.weight(1f),
                            colors = if (selected) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = color.copy(alpha = 0.12f),
                                    contentColor = color,
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                            border = if (selected) BorderStroke(1.5.dp, color)
                            else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            item {
                OutlinedTextField(
                    value = uiState.editDescription,
                    onValueChange = { onEvent(EntriesEvent.UpdateDescription(it)) },
                    label = { Text(stringResource(R.string.entry_description_label)) },
                    isError = uiState.descriptionError != null,
                    supportingText = uiState.descriptionError?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                OutlinedTextField(
                    value = uiState.editPrice,
                    onValueChange = { onEvent(EntriesEvent.UpdatePrice(it)) },
                    label = { Text(stringResource(R.string.entry_amount_label)) },
                    isError = uiState.priceError != null,
                    supportingText = uiState.priceError?.let { msg -> { Text(msg) } },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text(currencySymbol(currency)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    enabled = controlsEnabled,
                    shape = AppButtonShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                    text = stringResource(R.string.entry_review_category),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
            }
            itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
                EditCategoryRow(
                    category = category,
                    index = index,
                    count = categories.size,
                    selected = uiState.editCategory?.id == category.id,
                    enabled = controlsEnabled,
                    onClick = { onEvent(EntriesEvent.UpdateCategory(category)) },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        Surface(
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = cancelEnabled,
                    shape = AppButtonShape,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = { onEvent(EntriesEvent.SaveEdit) },
                    enabled = controlsEnabled,
                    shape = AppButtonShape,
                    modifier = Modifier.weight(1f),
                ) {
                    SaveActionContent(
                        label = stringResource(R.string.action_save),
                        isSaving = uiState.isSaving,
                    )
                }
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
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }, shape = AppButtonShape) {
                    Text(stringResource(R.string.action_cancel))
                }
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
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = listItemShape(index, count)
    val verticalPadding = when {
        count == 1 -> PaddingValues(horizontal = 0.dp, vertical = 4.dp)
        index == 0 -> PaddingValues(start = 0.dp, end = 0.dp, top = 4.dp, bottom = 1.dp)
        index == count - 1 -> PaddingValues(start = 0.dp, end = 0.dp, top = 1.dp, bottom = 4.dp)
        else -> PaddingValues(horizontal = 0.dp, vertical = 1.dp)
    }

    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(verticalPadding)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp),
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

private fun formatEditDate(epochMillis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
