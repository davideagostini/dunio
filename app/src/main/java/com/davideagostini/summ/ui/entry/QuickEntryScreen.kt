package com.davideagostini.summ.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.davideagostini.summ.R
import com.davideagostini.summ.data.session.SessionState
import com.davideagostini.summ.ui.auth.SessionViewModel
import com.davideagostini.summ.ui.components.SheetLoadingCard
import com.davideagostini.summ.ui.entry.components.StepAmount
import com.davideagostini.summ.ui.entry.components.StepCategory
import com.davideagostini.summ.ui.entry.components.StepDate
import com.davideagostini.summ.ui.entry.components.StepDescription
import com.davideagostini.summ.ui.entry.components.StepIndicator
import com.davideagostini.summ.ui.entry.components.StepReview
import com.davideagostini.summ.ui.entry.components.StepSuccess
import com.davideagostini.summ.ui.entry.components.StepType

@Composable
fun QuickEntryScreen(
    resolvedSessionState: SessionState? = null,
    viewModel: QuickEntryViewModel = hiltViewModel(),
    sessionViewModel: SessionViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
) {
    val observedSessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()
    val sessionState = resolvedSessionState ?: observedSessionState
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()

    if (sessionState is SessionState.Loading) {
        SheetLoadingCard()
        return
    }

    if (sessionState !is SessionState.Ready) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = when (sessionState) {
                        SessionState.SignedOut -> stringResource(R.string.quick_entry_signed_out_message)
                        is SessionState.NeedsHousehold -> stringResource(R.string.quick_entry_needs_household_message)
                        is SessionState.ConfigurationError -> sessionState.message
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        return
    }

    if (isLoading) {
        SheetLoadingCard()
        return
    }

    LaunchedEffect(Unit) {
        viewModel.handleEvent(EntryEvent.Reset)
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { event ->
            when (event) {
                EntryNavEvent.Saved -> onDismiss()
            }
        }
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding()
            .imePadding(),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.content_desc_close))
                }
            }
            if (uiState.step < 6) {
                StepIndicator(currentStep = uiState.step, totalSteps = 6)
                Spacer(Modifier.height(24.dp))
            }

            // The step body now swaps immediately without container animations. This avoids the flicker caused
            // by different step heights and keeps the card naturally sized to the current content.
            when (uiState.step) {
                0 -> StepType(uiState = uiState, onEvent = viewModel::handleEvent)
                1 -> StepDate(uiState = uiState, onEvent = viewModel::handleEvent)
                2 -> StepDescription(uiState = uiState, onEvent = viewModel::handleEvent)
                3 -> StepAmount(
                    uiState = uiState,
                    currency = sessionState.household.currency,
                    onEvent = viewModel::handleEvent,
                )
                4 -> StepCategory(categories = categories, uiState = uiState, onEvent = viewModel::handleEvent)
                5 -> StepReview(
                    uiState = uiState,
                    currency = sessionState.household.currency,
                    onEvent = viewModel::handleEvent,
                    onCancel = onDismiss,
                )
                6 -> StepSuccess()
            }
        }
    }
}
