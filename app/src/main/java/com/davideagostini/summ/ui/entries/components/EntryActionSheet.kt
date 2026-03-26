package com.davideagostini.summ.ui.entries.components

// EntryActionSheet renders the compact entries sheet, the edit form, and the success state used by the feature.
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
import androidx.compose.foundation.layout.imePadding
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
import com.davideagostini.summ.ui.auth.components.AuthErrorCard
import com.davideagostini.summ.ui.components.MonthCloseReadOnlyBanner
import com.davideagostini.summ.ui.entries.EntriesEvent
import com.davideagostini.summ.ui.entries.EntriesUiState
import com.davideagostini.summ.ui.entries.EntrySheetMode
import com.davideagostini.summ.ui.format.currencySymbol
import com.davideagostini.summ.ui.format.formatCurrency
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
    currency: String,
    readOnly: Boolean,
    readOnlyMessage: String,
    onEvent: (EntriesEvent) -> Unit,
    onDismiss: () -> Unit,
    fullScreen: Boolean = false,
) {
    // Fullscreen edit reuses the same content tree, but it needs a different container and inset handling.
    val containerModifier = if (fullScreen) {
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .navigationBarsPadding()
            .imePadding()
    } else {
        Modifier
            .fillMaxWidth()
            .then(if (uiState.sheetMode == EntrySheetMode.Edit) Modifier.fillMaxHeight(0.94f) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding()
            .imePadding()
    }

    // Success is rendered immediately as a standalone fullscreen surface when edit mode completes.
    if (fullScreen && uiState.sheetMode == EntrySheetMode.Success) {
        Surface(modifier = containerModifier, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            // Fullscreen edit success should behave like a standalone screen, not like a form with a top action row.
            EntrySuccessContent(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
            )
        }
        return
    }

    // The inner content swaps between action, edit, and success without changing the outer sheet container.
    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp).systemBarsPadding()) {
            // Keep the close action in the top-right corner so the sheet matches the rest of the app patterns.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onDismiss) {
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
                    // Edit mode expands into the full form with validation, category picking, and date selection.
                    EntrySheetMode.Edit    -> EntryEditForm(
                        uiState    = uiState,
                        categories = categories,
                        currency = currency,
                        readOnly = readOnly,
                        readOnlyMessage = readOnlyMessage,
                        onEvent    = onEvent,
                        onCancel   = onDismiss,
                    )
                    // Success stays in the same container so the user gets a stable completion state.
                    EntrySheetMode.Success -> EntrySuccessContent()
                    EntrySheetMode.Hidden  -> Unit
                }
            }
        }
    }

    if (fullScreen) {
        // Fullscreen edit uses a surface instead of a card because it behaves like a standalone screen.
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

// ── Edit form ─────────────────────────────────────────────────────────────────

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
    // The edit form keeps its own local date picker visibility because that state is purely presentational.
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.editDate)

    Column(modifier = Modifier.fillMaxSize()) {
        // The scrollable area holds the actual form fields and category list.
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
                // Validation and backend errors stay near the form fields that can trigger them.
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
                // Toggle buttons make the entry type explicit and easy to hit on mobile.
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
                // Description is the primary free-text field for an entry.
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
                // Amount uses a numeric keyboard and a currency prefix so typing stays fast on mobile.
                OutlinedTextField(
                    value          = uiState.editPrice,
                    onValueChange  = { onEvent(EntriesEvent.UpdatePrice(it)) },
                    label          = { Text(stringResource(R.string.entry_amount_label)) },
                    isError        = uiState.priceError != null,
                    supportingText = uiState.priceError?.let { msg -> { Text(msg) } },
                    singleLine     = true,
                    shape          = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix         = { Text(currencySymbol(currency)) },
                    modifier       = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                // The date button opens a modal picker; the selected date is written back through the ViewModel.
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
                // The review section starts with the category label, followed by the selectable category rows.
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
                // Bottom padding keeps the final category row from touching the fixed action bar.
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
            // The fixed action row stays visible above the keyboard so cancel/save always remain reachable.
            // The action row is fixed, but it still needs to clear the on-screen keyboard.
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
                    enabled = !readOnly,
                    shape    = AppButtonShape,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }

    if (showDatePicker) {
        // Date selection is modal so the user can update the transaction date without leaving the form.
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
    // Category rows share the same rounded shape logic used elsewhere in the app's list surfaces.
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
            // The emoji acts as a quick visual anchor while the name handles the actual selection target.
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
private fun EntrySuccessContent(
    modifier: Modifier = Modifier,
) {
    // Success mirrors the assets layout so the confirm state feels consistent across feature flows.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
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

private fun formatEditDate(epochMillis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochMillis))
