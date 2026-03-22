package com.davideagostini.summ.ui.categories.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.ui.theme.listItemShape

@Composable
internal fun CategoryCard(
    category: Category,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val shape = listItemShape(index, count)
    val verticalPadding = when {
        count == 1         -> PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        index == 0         -> PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 1.dp)
        index == count - 1 -> PaddingValues(start = 16.dp, end = 16.dp, top = 1.dp, bottom = 4.dp)
        else               -> PaddingValues(horizontal = 16.dp, vertical = 1.dp)
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(verticalPadding)
            .clickable(onClick = onClick),
        shape     = shape,
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = category.emoji,
                fontSize = 20.sp,
                modifier = Modifier.size(28.dp),
            )

            Text(
                text       = category.name,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier   = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )

            Icon(
                imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier           = Modifier.size(20.dp),
            )
        }
    }
}
