package com.davideagostini.summ.ui.dashboard.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.ui.components.MonthPickerField
import com.davideagostini.summ.ui.dashboard.formatMonthOption

@Composable
fun DashboardToolbar(
    monthOptions: List<String>,
    selectedMonth: String,
    onSelectMonth: (String) -> Unit,
) {
    MonthPickerField(
        label = formatMonthOption(selectedMonth),
        options = monthOptions,
        optionLabel = ::formatMonthOption,
        onSelect = onSelectMonth,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}
