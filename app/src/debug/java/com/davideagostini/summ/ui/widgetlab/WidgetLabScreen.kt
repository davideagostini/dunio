package com.davideagostini.summ.ui.widgetlab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.settings.components.SettingsSectionLabel
import com.davideagostini.summ.ui.theme.SummColors
import com.davideagostini.summ.ui.widgetlab.components.WidgetLabSection
import com.davideagostini.summ.widget.components.QuickEntryWidgetPreviewCard
import com.davideagostini.summ.widget.components.SpendingWidgetPreviewCard
import com.davideagostini.summ.widget.components.WidgetPreviewFrame
import com.davideagostini.summ.widget.model.WidgetLayoutSpec

// Debug-only screen that shows the real widget layout rules at all supported sizes.
// It is meant for fast iteration during development and is not wired into app navigation.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetLabScreen(
    onBack: () -> Unit,
) {
    val spendingSizes = WidgetLayoutSpec.spendingSizes.toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.widget_lab_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                    )
                }
            },
            colors = SummColors.topBarColors,
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingsSectionLabel(stringResource(R.string.widget_lab_title))
            }

            item {
                WidgetLabSection(
                    title = stringResource(R.string.widget_spending_title),
                    subtitle = stringResource(R.string.widget_lab_spending_subtitle),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        spendingSizes.forEach { size ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${size.width.value.toInt()} x ${size.height.value.toInt()} dp",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(modifier = Modifier.wrapContentWidth()) {
                                    WidgetPreviewFrame(width = size.width, height = size.height) {
                                        SpendingWidgetPreviewCard(
                                            variant = WidgetLayoutSpec.spendingVariant(
                                                width = size.width,
                                                height = size.height,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                WidgetLabSection(
                    title = stringResource(R.string.widget_quick_access_name),
                    subtitle = stringResource(R.string.widget_lab_quick_entry_subtitle),
                ) {
                    val quickSize = WidgetLayoutSpec.quickEntrySize
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "${quickSize.width.value.toInt()} x ${quickSize.height.value.toInt()} dp",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(modifier = Modifier.wrapContentWidth()) {
                            WidgetPreviewFrame(width = quickSize.width, height = quickSize.height) {
                                QuickEntryWidgetPreviewCard()
                            }
                        }
                    }
                }
            }
        }
    }
}

