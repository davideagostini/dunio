package com.davideagostini.summ.ui.dashboard

// Dashboard screen orchestration: it reads aggregated finance state, syncs the shared month picker, and renders the KPI stack.
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.components.MonthPickerOverlay
import com.davideagostini.summ.ui.components.buildRecentMonthOptions
import com.davideagostini.summ.ui.components.preferredRecentMonth
import com.davideagostini.summ.ui.dashboard.components.DashboardToolbar
import com.davideagostini.summ.ui.dashboard.components.GetStartedScreen
import com.davideagostini.summ.ui.dashboard.components.MetricCard
import com.davideagostini.summ.ui.dashboard.components.NetWorthCard
import com.davideagostini.summ.ui.format.formatCurrency
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import kotlin.math.abs

@Composable
fun DashboardScreen(
    onMonthPickerVisibilityChanged: (Boolean) -> Unit = {},
    onGetStartedVisibilityChanged: (Boolean) -> Unit = {},
    onOpenQuickEntry: () -> Unit = {},
    onOpenNewAsset: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val renderState by viewModel.renderState.collectAsStateWithLifecycle()

    if (isLoading) {
        DashboardLoadingContent(
            selectedMonth = uiState.selectedMonth ?: preferredRecentMonth(buildRecentMonthOptions()),
        )
        return
    }

    DashboardContent(
        renderState = renderState,
        uiState = uiState,
        onSelectMonth = viewModel::selectMonth,
        onSelectRange = viewModel::selectRange,
        onDismissGetStarted = viewModel::dismissGetStarted,
        onOpenQuickEntry = onOpenQuickEntry,
        onOpenNewAsset = onOpenNewAsset,
        onGetStartedVisibilityChanged = onGetStartedVisibilityChanged,
        onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
    )
}

@Composable
private fun DashboardLoadingContent(
    selectedMonth: String,
) {
    val shimmer = rememberDashboardShimmerBrush()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 92.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.dashboard_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }

                item {
                    DashboardToolbar(
                        selectedMonth = selectedMonth,
                        onOpenMonthPicker = {},
                    )
                }

                item {
                    DashboardHeroSkeleton(shimmer)
                }

                item {
                    DashboardMetricRowSkeleton(shimmer)
                }
            }
        }
    }
}

@Composable
private fun DashboardHeroSkeleton(shimmer: Brush) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            DashboardSkeletonBlock(
                brush = shimmer,
                modifier = Modifier
                    .width(120.dp)
                    .height(14.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
            DashboardSkeletonBlock(
                brush = shimmer,
                modifier = Modifier
                    .width(220.dp)
                    .height(42.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(18.dp))
            DashboardSkeletonBlock(
                brush = shimmer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
        }
    }
}

@Composable
private fun DashboardMetricRowSkeleton(shimmer: Brush) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        DashboardMetricSkeletonCard(
            shimmer = shimmer,
            modifier = Modifier.fillMaxWidth(),
        )
        DashboardMetricSkeletonCard(
            shimmer = shimmer,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DashboardMetricSkeletonCard(
    shimmer: Brush,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            DashboardSkeletonBlock(
                brush = shimmer,
                modifier = Modifier
                    .width(84.dp)
                    .height(12.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
            DashboardSkeletonBlock(
                brush = shimmer,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(28.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
            DashboardSkeletonBlock(
                brush = shimmer,
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(12.dp),
            )
        }
    }
}

@Composable
private fun DashboardSkeletonBlock(
    brush: Brush,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(brush),
    )
}

@Composable
private fun rememberDashboardShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = MaterialTheme.colorScheme.surfaceBright
    val transition = rememberInfiniteTransition(label = "dashboard_shimmer")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dashboard_shimmer_offset",
    )
    return Brush.linearGradient(
        colors = listOf(
            base.copy(alpha = 0.9f),
            highlight.copy(alpha = 0.65f),
            base.copy(alpha = 0.9f),
        ),
        start = Offset(offset - 220f, offset - 220f),
        end = Offset(offset, offset),
    )
}

@Composable
private fun DashboardContent(
    renderState: DashboardRenderState,
    uiState: DashboardUiState,
    onSelectMonth: (String) -> Unit,
    onSelectRange: (DashboardRange) -> Unit,
    onDismissGetStarted: () -> Unit,
    onOpenQuickEntry: () -> Unit,
    onOpenNewAsset: () -> Unit,
    onGetStartedVisibilityChanged: (Boolean) -> Unit,
    onMonthPickerVisibilityChanged: (Boolean) -> Unit,
) {
    var showMonthPicker by remember { mutableStateOf(false) }
    val monthOptions = remember { buildRecentMonthOptions() }
    val selectedMonth = renderState.selectedMonth

    // Keep the shared bottom bar hidden while the month picker overlay is open.
    LaunchedEffect(showMonthPicker) {
        onMonthPickerVisibilityChanged(showMonthPicker)
    }

    LaunchedEffect(uiState.showGetStarted) {
        onGetStartedVisibilityChanged(uiState.showGetStarted)
    }

    if (uiState.showGetStarted) {
        GetStartedScreen(
            hasEntries = renderState.hasEntries,
            hasAssets = renderState.hasAssets,
            onDismiss = onDismissGetStarted,
            onAddEntry = onOpenQuickEntry,
            onAddAsset = onOpenNewAsset,
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            // The dashboard remains a vertically stacked mobile layout so the KPI cards stay readable on small screens.
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 92.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.dashboard_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }

                item {
                    DashboardToolbar(
                        selectedMonth = selectedMonth,
                        onOpenMonthPicker = { showMonthPicker = true },
                    )
                }

                item {
                    NetWorthCard(
                        month = selectedMonth,
                        currency = renderState.householdCurrency,
                        netWorth = renderState.metrics.netWorth,
                        monthlyChangePercent = renderState.monthlyChangePercent,
                        chartPoints = renderState.chartPoints,
                        selectedRange = renderState.selectedRange,
                        onSelectRange = onSelectRange,
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_assets_label),
                        value = formatCurrency(renderState.metrics.totalAssets, renderState.householdCurrency),
                        note = stringResource(R.string.dashboard_current_value),
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_liabilities_label),
                        value = formatCurrency(renderState.metrics.totalLiabilities, renderState.householdCurrency),
                        note = stringResource(R.string.dashboard_outstanding),
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_cash_flow_label),
                        value = formatCurrency(renderState.metrics.monthlyCashFlow, renderState.householdCurrency),
                        note = stringResource(R.string.dashboard_cash_flow_note),
                        trendLabel = renderState.cashFlowChangePercent?.let {
                            stringResource(R.string.dashboard_change_vs_previous_month, formatPercent(abs(it)))
                        },
                        trendPositive = renderState.cashFlowChangePercent?.let { it >= 0 },
                        trendColor = when {
                            renderState.cashFlowChangePercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            renderState.cashFlowChangePercent > 0 -> IncomeGreen
                            renderState.cashFlowChangePercent < 0 -> ExpenseRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        valueColor = if (renderState.metrics.monthlyCashFlow >= 0) IncomeGreen else ExpenseRed,
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_savings_rate_label),
                        value = renderState.metrics.savingsRate?.let { formatPercent(it) } ?: stringResource(R.string.dashboard_value_not_available),
                        note = stringResource(R.string.dashboard_savings_rate_note),
                        trendLabel = renderState.savingsRateDelta?.let {
                            stringResource(R.string.dashboard_change_vs_3m_avg, formatPercent(abs(it)))
                        },
                        trendPositive = renderState.savingsRateDelta?.let { it >= 0 },
                        trendColor = when {
                            renderState.savingsRateDelta == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            renderState.savingsRateDelta > 0 -> IncomeGreen
                            renderState.savingsRateDelta < 0 -> ExpenseRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_runway_label),
                        value = renderState.metrics.financialRunway?.let { formatRunwayMonths(it) } ?: stringResource(R.string.dashboard_value_not_available),
                        trailingValue = renderState.metrics.financialRunway?.let { "/ ${formatRunwayYears(it)}" },
                        trendLabel = renderState.runwayChangePercent?.let {
                            stringResource(R.string.dashboard_change_vs_previous_month, formatPercent(abs(it)))
                        },
                        trendPositive = renderState.runwayChangePercent?.let { it >= 0 },
                        trendColor = when {
                            renderState.runwayChangePercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            renderState.runwayChangePercent > 0 -> IncomeGreen
                            renderState.runwayChangePercent < 0 -> ExpenseRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                item {
                    MetricCard(
                        label = stringResource(R.string.dashboard_monthly_expenses_label),
                        value = formatCurrency(renderState.monthlyExpenses, renderState.householdCurrency),
                        trendLabel = renderState.monthlyExpensesChangePercent?.let {
                            stringResource(R.string.dashboard_change_vs_previous_month, formatPercent(abs(it)))
                        },
                    trendPositive = renderState.monthlyExpensesChangePercent?.let { it >= 0 },
                    trendColor = when {
                        renderState.monthlyExpensesChangePercent == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        renderState.monthlyExpensesChangePercent < 0 -> IncomeGreen
                        renderState.monthlyExpensesChangePercent > 0 -> ExpenseRed
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        MonthPickerOverlay(
            visible = showMonthPicker,
            selectedOption = selectedMonth,
            options = monthOptions,
            optionLabel = ::formatMonthOption,
            onSelect = onSelectMonth,
            onDismiss = { showMonthPicker = false },
        )
    }
}
