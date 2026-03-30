package com.davideagostini.summ.ui.settings.recurring.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Repeat
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
import androidx.compose.material3.Switch
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
import com.davideagostini.summ.ui.format.currencySymbol
import com.davideagostini.summ.ui.format.formatCurrency
import com.davideagostini.summ.ui.settings.recurring.RecurringEvent
import com.davideagostini.summ.ui.settings.recurring.RecurringSheetMode
import com.davideagostini.summ.ui.settings.recurring.RecurringUiState
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import com.davideagostini.summ.ui.theme.listItemShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecurringSheet(
    uiState: RecurringUiState,
    categories: List<Category>,
    currency: String,
    onEvent: (RecurringEvent) -> Unit,
    onDismiss: () -> Unit,
    fullScreen: Boolean = false,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.startDate)
    val containerModifier = if (fullScreen) {
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .navigationBarsPadding()
            .imePadding()
    } else {
        Modifier
            .fillMaxWidth()
            .then(if (uiState.sheetMode == RecurringSheetMode.Add || uiState.sheetMode == RecurringSheetMode.Edit) Modifier.fillMaxHeight(0.94f) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding()
            .imePadding()
    }

    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp).statusBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.content_desc_close))
                }
            }
            when (uiState.sheetMode) {
                RecurringSheetMode.Action -> {
                    uiState.selectedRecurring?.let { recurring ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            uiState.operationErrorMessage?.let { message ->
                                AuthErrorCard(message)
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(72.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Outlined.Repeat,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                            Text(recurring.description, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text(
                                formatCurrency(recurring.amount, currency),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (recurring.type == "expense") ExpenseRed else IncomeGreen,
                            )
                            Text(
                                stringResource(R.string.recurring_every_month_on_day, recurring.category, recurring.dayOfMonth),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { onEvent(RecurringEvent.RequestDelete) }, modifier = Modifier.weight(1f), shape = AppButtonShape) {
                                    Text(stringResource(R.string.action_delete), color = ExpenseRed)
                                }
                                Button(onClick = { onEvent(RecurringEvent.StartEdit) }, modifier = Modifier.weight(1f), shape = AppButtonShape) {
                                    Text(stringResource(R.string.action_edit))
                                }
                            }
                        }
                    }
                }
                RecurringSheetMode.Add, RecurringSheetMode.Edit -> {
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
                                    if (uiState.sheetMode == RecurringSheetMode.Add) stringResource(R.string.recurring_add_title) else stringResource(R.string.recurring_edit_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
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
                                TypeToggle(selectedType = uiState.type, onSelect = { onEvent(RecurringEvent.UpdateType(it)) })
                                Spacer(Modifier.height(12.dp))
                            }
                            item {
                                OutlinedTextField(
                                    value = uiState.description,
                                    onValueChange = { onEvent(RecurringEvent.UpdateDescription(it)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.entry_description_label)) },
                                    isError = uiState.descriptionError != null,
                                    supportingText = uiState.descriptionError?.let { message -> { Text(message) } },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            item {
                                OutlinedTextField(
                                    value = uiState.amount,
                                    onValueChange = { onEvent(RecurringEvent.UpdateAmount(it)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.entry_amount_label)) },
                                    isError = uiState.amountError != null,
                                    supportingText = uiState.amountError?.let { message -> { Text(message) } },
                                    prefix = { Text(currencySymbol(currency)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            item {
                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    shape = AppButtonShape,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = formatDateLabel(uiState.startDate.toLocalDateStringCompat()),
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
                            items(categories.size, key = { categories[it].id }) { index ->
                                val category = categories[index]
                                RecurringCategoryRow(
                                    category = category,
                                    index = index,
                                    count = categories.size,
                                    selected = uiState.category == category.name,
                                    onClick = { onEvent(RecurringEvent.UpdateCategory(category)) },
                                )
                            }
                            item {
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = uiState.dayOfMonth,
                                    onValueChange = { onEvent(RecurringEvent.UpdateDayOfMonth(it)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(stringResource(R.string.recurring_day_label)) },
                                    isError = uiState.dayError != null,
                                    supportingText = uiState.dayError?.let { message -> { Text(message) } },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stringResource(R.string.recurring_active), modifier = Modifier.weight(1f))
                                    Switch(checked = uiState.active, onCheckedChange = { onEvent(RecurringEvent.UpdateActive(it)) })
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                        }

                        Surface(
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding(),
                        ) {
                            // Keep the fixed footer above the keyboard instead of letting the IME cover it.
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                            ) {
                                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = AppButtonShape) {
                                    Text(stringResource(R.string.action_cancel))
                                }
                                Button(
                                    onClick = { onEvent(if (uiState.sheetMode == RecurringSheetMode.Add) RecurringEvent.SaveAdd else RecurringEvent.SaveEdit) },
                                    modifier = Modifier.weight(1f),
                                    shape = AppButtonShape,
                                ) {
                                    Text(if (uiState.sheetMode == RecurringSheetMode.Add) stringResource(R.string.action_create) else stringResource(R.string.action_save))
                                }
                            }
                        }
                    }
                }
                RecurringSheetMode.Hidden -> Unit
            }
        }
    }

    if (fullScreen) {
        Surface(modifier = containerModifier, color = MaterialTheme.colorScheme.surfaceContainerLow) { content() }
    } else {
        Card(
            modifier = containerModifier,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) { content() }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onEvent(RecurringEvent.UpdateStartDate(it)) }
                    showDatePicker = false
                }, shape = AppButtonShape) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }, shape = AppButtonShape) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun TypeToggle(selectedType: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            "income" to stringResource(R.string.entry_type_income),
            "expense" to stringResource(R.string.entry_type_expense),
        ).forEach { (value, label) ->
            val selected = selectedType == value
            val color = if (value == "income") IncomeGreen else ExpenseRed
            OutlinedButton(
                onClick = { onSelect(value) },
                modifier = Modifier.weight(1f),
                shape = AppButtonShape,
                colors = if (selected) ButtonDefaults.outlinedButtonColors(
                    containerColor = color.copy(alpha = 0.12f),
                    contentColor = color,
                ) else ButtonDefaults.outlinedButtonColors(),
                border = if (selected) BorderStroke(1.5.dp, color) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun RecurringCategoryRow(
    category: Category,
    index: Int,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = listItemShape(index, count)
    val verticalPadding = when {
        count == 1 -> PaddingValues(horizontal = 0.dp, vertical = 4.dp)
        index == 0 -> PaddingValues(start = 0.dp, end = 0.dp, top = 4.dp, bottom = 1.dp)
        index == count - 1 -> PaddingValues(start = 0.dp, end = 0.dp, top = 1.dp, bottom = 4.dp)
        else -> PaddingValues(horizontal = 0.dp, vertical = 1.dp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(verticalPadding),
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
            Text(text = category.emoji, fontSize = 20.sp, modifier = Modifier.size(28.dp))
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

private fun formatDateLabel(dateString: String): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateString.toInstantCompat()))

private fun String.toInstantCompat(): Long =
    java.time.LocalDate.parse(this)
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

private fun Long.toLocalDateStringCompat(): String =
    java.time.Instant.ofEpochMilli(this)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .toString()
