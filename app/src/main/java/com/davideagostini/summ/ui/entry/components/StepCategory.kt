package com.davideagostini.summ.ui.entry.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.ui.entry.EntryEvent
import com.davideagostini.summ.ui.entry.EntryUiState

@Composable
internal fun StepCategory(
    categories: List<Category>,
    uiState: EntryUiState,
    onEvent: (EntryEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        StepTitle("Pick a category")
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier            = Modifier
                .fillMaxWidth()
                .height(240.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(categories, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    selected = uiState.selectedCategory?.id == category.id,
                    onClick  = { onEvent(EntryEvent.SelectCategory(category)) },
                )
            }
        }

        if (uiState.selectedCategory == null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Please select a category",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(16.dp))
        StepNavRow(onBack = { onEvent(EntryEvent.Back) }, onNext = { onEvent(EntryEvent.Next) })
    }
}

@Composable
private fun CategoryRow(category: Category, selected: Boolean, onClick: () -> Unit) {
    val bgColor     = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(category.emoji, fontSize = 20.sp)
        Text(
            text       = category.name,
            style      = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier   = Modifier.weight(1f),
        )
        if (selected) {
            Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
        }
    }
}
