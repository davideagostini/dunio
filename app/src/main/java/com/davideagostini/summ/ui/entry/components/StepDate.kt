package com.davideagostini.summ.ui.entry.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.entry.EntryEvent
import com.davideagostini.summ.ui.entry.EntryUiState
import com.davideagostini.summ.ui.format.datePickerMillisToLocalStartOfDayMillis
import com.davideagostini.summ.ui.format.localStartOfDayMillisToDatePickerMillis
import com.davideagostini.summ.ui.theme.AppButtonShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StepDate(uiState: EntryUiState, onEvent: (EntryEvent) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = localStartOfDayMillisToDatePickerMillis(uiState.date),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        StepTitle(stringResource(R.string.entry_step_date_title))
        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = { showDatePicker = true },
            shape = AppButtonShape,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(uiState.date)),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))
        StepNavRow(onBack = { onEvent(EntryEvent.Back) }, onNext = { onEvent(EntryEvent.Next) })
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onEvent(EntryEvent.UpdateDate(datePickerMillisToLocalStartOfDayMillis(it)))
                        }
                        showDatePicker = false
                    },
                    shape = AppButtonShape,
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }, shape = AppButtonShape) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
