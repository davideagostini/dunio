package com.davideagostini.summ.ui.components

// Shared month-picker UI used by multiple screens: a compact trigger field and a full-screen overlay selector.
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.theme.listItemShape
import java.time.YearMonth

@Composable
fun MonthPickerField(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    // Compact trigger surface reused in toolbars and settings screens.
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 2.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compact) 16.dp else 18.dp,
                    vertical = if (compact) 14.dp else 18.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
            Text(
                text = label,
                style = if (compact) {
                    MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp)
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Outlined.UnfoldMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun MonthPickerOverlay(
    visible: Boolean,
    selectedOption: String,
    options: List<String>,
    optionLabel: (String) -> String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Full-screen overlay keeps month selection consistent with the mobile-first app navigation.
    BackHandler(enabled = visible) {
        onDismiss()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(
            initialOffsetY = { it / 16 },
            animationSpec = tween(220),
        ),
        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(
            targetOffsetY = { it / 24 },
            animationSpec = tween(220),
        ),
        modifier = modifier.fillMaxSize(),
    ) {
        val sections = remember(options) { buildMonthPickerSections(options) }
        val listState = rememberLazyListState()
        LaunchedEffect(selectedOption, sections) {
            val selectedSectionIndex = sections.indexOfFirst { section ->
                section.months.any { it == selectedOption }
            }
            if (selectedSectionIndex >= 0) {
                listState.scrollToItem(selectedSectionIndex)
            }
        }

        // Keep the whole overlay on a themed surface so title text and icons inherit the dark-mode palette.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.month_picker_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.content_desc_close),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(
                        items = sections,
                        key = { section -> "year:${section.year}" },
                    ) { section ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = section.year.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                            )

                            section.months.forEachIndexed { index, option ->
                                val isSelected = option == selectedOption
                                Card(
                                    onClick = {
                                        onSelect(option)
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = listItemShape(index, section.months.size),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    border = if (isSelected) {
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        null
                                    },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                ) {
                                    Text(
                                        text = optionLabel(option),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class MonthPickerYearSection(
    val year: Int,
    val months: List<String>,
)

private fun buildMonthPickerSections(options: List<String>): List<MonthPickerYearSection> =
    options
        .groupBy { YearMonth.parse(it).year }
        .map { (year, months) ->
            MonthPickerYearSection(
                year = year,
                months = months,
            )
        }
