package com.davideagostini.summ.ui.settings.monthclose

// Month close screen orchestration: it exposes the selected period, checklist status, and the shared month picker overlay.
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.components.FullScreenLoading
import com.davideagostini.summ.ui.components.MonthPickerField
import com.davideagostini.summ.ui.components.MonthPickerOverlay
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import com.davideagostini.summ.ui.theme.SummColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Screen for reviewing and closing a month.
 *
 * It summarizes the selected month and exposes the controls that make the month read-only once the
 * household is ready to lock it.
 */
@Composable
fun MonthCloseScreen(
    onBack: () -> Unit,
    onMonthPickerVisibilityChanged: (Boolean) -> Unit = {},
    viewModel: MonthCloseViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (isLoading) {
        FullScreenLoading()
        return
    }

    MonthCloseContent(
        uiState = uiState,
        onBack = onBack,
        onSelectMonth = viewModel::selectMonth,
        onSetStatus = viewModel::setStatus,
        onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthCloseContent(
    uiState: MonthCloseUiState,
    onBack: () -> Unit,
    onSelectMonth: (String) -> Unit,
    onSetStatus: (String) -> Unit,
    onMonthPickerVisibilityChanged: (Boolean) -> Unit,
) {
    var showMonthPicker by remember { mutableStateOf(false) }

    // Keep the shared bottom bar hidden while the month picker overlay is open.
    LaunchedEffect(showMonthPicker) {
        onMonthPickerVisibilityChanged(showMonthPicker)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            // The top app bar keeps navigation obvious while the month close screen stays focused on a single period.
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_month_close_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
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
                    // The month picker and status chip share the same row to keep the selected period visually anchored.
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MonthPickerField(
                            label = formatMonthOption(uiState.month),
                            onClick = { showMonthPicker = true },
                            compact = true,
                            modifier = Modifier.weight(1f),
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (uiState.status == "closed") IncomeGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerLowest,
                        ) {
                            Text(
                                text = if (uiState.status == "closed") stringResource(R.string.month_close_closed) else stringResource(R.string.month_close_draft),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (uiState.status == "closed") IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    // This overview card summarizes the current close state before the user inspects the checklist items.
                    Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.month_close_overview), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (uiState.canClose) stringResource(R.string.month_close_ready) else stringResource(R.string.month_close_needs_review),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (uiState.canClose) stringResource(R.string.month_close_ready_message) else stringResource(R.string.month_close_needs_review_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                ChecklistCard(
                    title = stringResource(R.string.month_close_asset_snapshots),
                    subtitle = stringResource(R.string.month_close_asset_snapshots_subtitle),
                    value = uiState.assetSnapshotCount.toString(),
                )
            }
            item {
                ChecklistCard(
                    title = stringResource(R.string.month_close_transactions),
                    subtitle = stringResource(R.string.month_close_transactions_subtitle),
                    value = uiState.transactionCount.toString(),
                )
            }
            item {
                // Recurring checks are handled separately because they can block closing even when the core counts are complete.
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.month_close_recurring_due), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.month_close_recurring_due_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                uiState.recurringMissingCount.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.recurringMissingCount > 0) ExpenseRed else IncomeGreen,
                            )
                        }
                        if (uiState.recurringMissingLabels.isEmpty()) {
                            Surface(shape = RoundedCornerShape(999.dp), color = IncomeGreen.copy(alpha = 0.12f)) {
                                Text(
                                    text = stringResource(R.string.month_close_all_recurring_applied),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IncomeGreen,
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.recurringMissingLabels.forEach { label ->
                                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                                        Text(
                                            text = label,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                // The action row is the final gate: it either closes the month or reopens it depending on readiness.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onSetStatus("closed") },
                        enabled = uiState.canClose,
                        modifier = Modifier.weight(1f),
                        shape = AppButtonShape,
                    ) { Text(stringResource(R.string.month_close_action_close)) }
                    OutlinedButton(
                        onClick = { onSetStatus("draft") },
                        modifier = Modifier.weight(1f),
                        shape = AppButtonShape,
                    ) {
                        Text(stringResource(R.string.month_close_action_reopen))
                    }
                }
            }
            }
        }

        MonthPickerOverlay(
            visible = showMonthPicker,
            selectedOption = uiState.month,
            options = uiState.monthOptions,
            optionLabel = ::formatMonthOption,
            onSelect = onSelectMonth,
            onDismiss = { showMonthPicker = false },
        )
    }
}

@Composable
private fun ChecklistCard(title: String, subtitle: String, value: String) {
    // Reusable checklist row used for the month close counts and readiness indicators.
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// The selected month is shown in a human-friendly format so the screen reads like a lightweight checklist, not a raw data table.
private fun formatMonthOption(monthValue: String): String =
    YearMonth.parse(monthValue).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
