package com.davideagostini.summ.ui.entry.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.entry.EntryEvent
import com.davideagostini.summ.ui.entry.EntryUiState

@Composable
internal fun StepDescription(uiState: EntryUiState, onEvent: (EntryEvent) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxWidth()) {
        StepTitle(stringResource(R.string.entry_step_description_title))
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value           = uiState.description,
            onValueChange   = { onEvent(EntryEvent.UpdateDescription(it)) },
            label           = { Text(stringResource(R.string.entry_description_label)) },
            isError         = uiState.descriptionError != null,
            supportingText  = uiState.descriptionError?.let { msg -> { Text(msg) } },
            singleLine      = true,
            shape           = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onEvent(EntryEvent.Next) }),
            modifier        = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
        )

        Spacer(Modifier.height(20.dp))
        StepNavRow(onBack = { onEvent(EntryEvent.Back) }, onNext = { onEvent(EntryEvent.Next) })
    }
}
