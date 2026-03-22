package com.davideagostini.summ.ui.settings.recurring

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.data.entity.RecurringTransaction
import com.davideagostini.summ.ui.components.FullScreenLoading
import com.davideagostini.summ.ui.format.formatEuro
import com.davideagostini.summ.ui.settings.recurring.components.RecurringSheet
import com.davideagostini.summ.ui.settings.recurring.components.RecurringSplitButton
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import com.davideagostini.summ.ui.theme.SummColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecurringScreen(
    onBack: () -> Unit,
    viewModel: RecurringViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val recurring by viewModel.recurringTransactions.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (isLoading) {
        FullScreenLoading()
        return
    }

    RecurringContent(
        recurring = recurring,
        categories = categories,
        uiState = uiState,
        onBack = onBack,
        onEvent = viewModel::handleEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringContent(
    recurring: List<RecurringTransaction>,
    categories: List<Category>,
    uiState: RecurringUiState,
    onBack: () -> Unit,
    onEvent: (RecurringEvent) -> Unit,
) {
    var allowHide by remember { mutableStateOf(false) }
    var showFullScreenEdit by remember { mutableStateOf(uiState.sheetMode == RecurringSheetMode.Edit) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || allowHide },
    )
    val filtered = remember(recurring, uiState.searchQuery) {
        recurring.filter {
            listOf(it.description, it.category, it.type).joinToString(" ")
                .contains(uiState.searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(uiState.sheetMode) {
        if (uiState.sheetMode == RecurringSheetMode.Edit) {
            showFullScreenEdit = true
        } else if (uiState.sheetMode == RecurringSheetMode.Hidden) {
            showFullScreenEdit = false
        }
    }

    val dismissFullscreenEdit: () -> Unit = {
        showFullScreenEdit = false
        scope.launch {
            kotlinx.coroutines.delay(220)
            onEvent(RecurringEvent.DismissSheet)
        }
    }

    BackHandler(enabled = showFullScreenEdit) {
        dismissFullscreenEdit()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_recurring_title), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back))
                }
            },
            colors = SummColors.topBarColors,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RecurringSplitButton(
                        onAddRecurring = { onEvent(RecurringEvent.StartAdd) },
                        onApplyDue = { onEvent(RecurringEvent.ApplyDue) },
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (uiState.searchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.size(52.dp),
                    ) {
                        IconButton(onClick = { onEvent(RecurringEvent.ToggleSearch) }) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.recurring_search_desc),
                                tint = if (uiState.searchVisible) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            if (uiState.searchVisible) {
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { onEvent(RecurringEvent.UpdateSearchQuery(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.recurring_search_placeholder)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onEvent(RecurringEvent.UpdateSearchQuery("")) }) {
                                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.content_desc_clear_search))
                                }
                            }
                        },
                        shape = RoundedCornerShape(22.dp),
                    )
                }
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.recurring_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.recurring_empty_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(filtered.size, key = { filtered[it].id }) { index ->
                    val item = filtered[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEvent(RecurringEvent.Select(item)) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.description, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    stringResource(R.string.recurring_every_month_on_day, item.category, item.dayOfMonth),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    stringResource(R.string.recurring_starts_due, formatDateLabel(item.startDate), formatDateLabel(getRecurringDueDateLabel(item))),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                formatEuro(item.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (item.type == "expense") ExpenseRed else IncomeGreen,
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.sheetMode != RecurringSheetMode.Hidden && uiState.sheetMode != RecurringSheetMode.Edit) {
        ModalBottomSheet(
            onDismissRequest = {},
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        ) {
            RecurringSheet(
                uiState = uiState,
                categories = categories,
                onEvent = onEvent,
                onDismiss = {
                    scope.launch {
                        allowHide = true
                        sheetState.hide()
                        onEvent(RecurringEvent.DismissSheet)
                        allowHide = false
                    }
                },
            )
        }
    }

    AnimatedVisibility(
        visible = showFullScreenEdit,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
        ) {
            RecurringSheet(
                uiState = uiState,
                categories = categories,
                onEvent = onEvent,
                onDismiss = dismissFullscreenEdit,
                fullScreen = true,
            )
        }
    }

    if (uiState.showDeleteDialog) {
        val desc = uiState.selectedRecurring?.description.orEmpty()
        AlertDialog(
            onDismissRequest = { onEvent(RecurringEvent.DismissDeleteDialog) },
            title = { Text(stringResource(R.string.recurring_delete_title, desc)) },
            text = { Text(stringResource(R.string.recurring_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(RecurringEvent.ConfirmDelete) },
                    shape = AppButtonShape,
                    colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed),
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { onEvent(RecurringEvent.DismissDeleteDialog) },
                    shape = AppButtonShape,
                ) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

private fun formatDateLabel(dateString: String): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(toInstantCompat(dateString)))

private fun toInstantCompat(value: String): Long =
    java.time.LocalDate.parse(value)
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

private fun getRecurringDueDateLabel(recurring: RecurringTransaction, monthKey: String = java.time.YearMonth.now().toString()): String {
    val yearMonth = java.time.YearMonth.parse(monthKey)
    return yearMonth.atDay(recurring.dayOfMonth.coerceAtMost(yearMonth.lengthOfMonth())).toString()
}
