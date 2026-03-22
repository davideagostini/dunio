package com.davideagostini.summ.ui.dashboard.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.ui.components.MonthPickerField
import com.davideagostini.summ.ui.dashboard.formatMonthOption

@Composable
fun DashboardToolbar(
    selectedMonth: String,
    onOpenMonthPicker: () -> Unit,
) {
    MonthPickerField(
        label = formatMonthOption(selectedMonth),
        onClick = onOpenMonthPicker,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}
