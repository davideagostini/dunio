package com.davideagostini.summ.ui.assets.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.assets.formatMonthLabel
import com.davideagostini.summ.ui.components.MonthPickerField

@Composable
fun AssetsToolbar(
    monthOptions: List<String>,
    selectedMonth: String,
    searchVisible: Boolean,
    searchQuery: String,
    onSelectMonth: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonthPickerField(
                label = formatMonthLabel(selectedMonth),
                options = monthOptions,
                optionLabel = ::formatMonthLabel,
                onSelect = onSelectMonth,
                modifier = Modifier.weight(1f),
            )

            Surface(
                shape = CircleShape,
                color = if (searchVisible) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLowest
                },
                tonalElevation = 2.dp,
                modifier = Modifier.size(60.dp),
            ) {
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.content_desc_search_assets),
                        tint = if (searchVisible) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }

        if (searchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                placeholder = { Text(stringResource(R.string.assets_search_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.content_desc_clear_search),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            )
        }
    }
}
