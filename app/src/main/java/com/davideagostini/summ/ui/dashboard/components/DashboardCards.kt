package com.davideagostini.summ.ui.dashboard.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.dashboard.ChartPoint
import com.davideagostini.summ.ui.dashboard.DashboardRange
import com.davideagostini.summ.ui.dashboard.formatMonthOption
import com.davideagostini.summ.ui.dashboard.formatPercent
import com.davideagostini.summ.ui.format.currencySymbol
import com.davideagostini.summ.ui.format.formatCurrency
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis.Companion.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis.Companion.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import kotlin.math.abs

@Composable
fun NetWorthCard(
    month: String,
    currency: String,
    netWorth: Double,
    monthlyChangePercent: Double?,
    chartPoints: List<ChartPoint>,
    selectedRange: DashboardRange,
    onSelectRange: (DashboardRange) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatMonthOption(month),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatCurrency(netWorth, currency),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            if (monthlyChangePercent != null) {
                val changeColor = if (monthlyChangePercent >= 0) IncomeGreen else ExpenseRed
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = if (monthlyChangePercent >= 0) Icons.Outlined.ArrowOutward else Icons.Outlined.ArrowDownward,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(14.dp),
                        )
                        Text(
                            text = stringResource(
                                R.string.dashboard_change_vs_previous_month,
                                formatPercent(abs(monthlyChangePercent)),
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = changeColor,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.width(14.dp))
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.dashboard_no_previous_month),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(20.dp))
            NetWorthChart(
                points = chartPoints,
                currency = currency,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            )
            Spacer(Modifier.height(18.dp))
            DashboardRangeGroup(
                selectedRange = selectedRange,
                onSelectRange = onSelectRange,
            )
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    note: String? = null,
    trailingValue: String? = null,
    trendLabel: String? = null,
    trendPositive: Boolean? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    trendColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                )
                if (!trailingValue.isNullOrBlank()) {
                    Text(
                        text = " $trailingValue",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!note.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!trendLabel.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                ) {
                    if (trendPositive != null) {
                        Icon(
                            imageVector = if (trendPositive) Icons.Outlined.ArrowOutward else Icons.Outlined.ArrowDownward,
                            contentDescription = null,
                            tint = trendColor,
                        )
                    }
                    Text(
                        text = trendLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = trendColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun NetWorthChart(
    points: List<ChartPoint>,
    currency: String,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.dashboard_no_history_yet),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(points) {
        modelProducer.runTransaction {
            lineSeries {
                series(points.map { it.value })
            }
        }
    }

    val labels = remember(points) { points.map(ChartPoint::label) }
    val bottomAxisFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ ->
            labels.getOrElse(value.toInt().coerceIn(0, labels.lastIndex)) { labels.first() }
        }
    }
    val startAxisFormatter = remember(currency) {
        CartesianValueFormatter.decimal(
            decimalCount = 2,
            thousandsSeparator = ",",
            prefix = currencySymbol(currency),
        )
    }
    val chartTheme = rememberM3VicoTheme(
        lineCartesianLayerColors = listOf(MaterialTheme.colorScheme.onSurface),
        lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val chartLineColor = MaterialTheme.colorScheme.onSurface
    val chartAreaTopColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val chartAreaBottomColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)

    ProvideVicoTheme(theme = chartTheme) {
        val areaFill = remember(chartAreaTopColor, chartAreaBottomColor) {
            LineCartesianLayer.AreaFill.single(
                Fill(
                    Brush.verticalGradient(
                        listOf(
                            chartAreaTopColor,
                            chartAreaBottomColor,
                        ),
                    ),
                ),
            )
        }
        val line = remember(chartLineColor, areaFill) {
            LineCartesianLayer.Line(
                fill = LineCartesianLayer.LineFill.single(Fill(chartLineColor)),
                areaFill = areaFill,
                stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 4.dp),
            )
        }
        val lineLayer = rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(line),
        )
        val startAxis = rememberStart(valueFormatter = startAxisFormatter)
        val bottomAxis = rememberBottom(valueFormatter = bottomAxisFormatter)
        val chart = rememberCartesianChart(
            lineLayer,
            startAxis = startAxis,
            bottomAxis = bottomAxis,
        )
        CartesianChartHost(
            modifier = modifier,
            chart = chart,
            modelProducer = modelProducer,
        )
    }
}

@Composable
private fun DashboardRangeGroup(
    selectedRange: DashboardRange,
    onSelectRange: (DashboardRange) -> Unit,
) {
    SingleChoiceSegmentedButtonRow {
        listOf(
            DashboardRange.ThreeMonths to stringResource(R.string.dashboard_range_3m),
            DashboardRange.SixMonths to stringResource(R.string.dashboard_range_6m),
            DashboardRange.OneYear to stringResource(R.string.dashboard_range_1y),
        ).forEachIndexed { index, (range, label) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = 3,
                ),
                onClick = { onSelectRange(range) },
                selected = range == selectedRange,
            ) {
                Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
