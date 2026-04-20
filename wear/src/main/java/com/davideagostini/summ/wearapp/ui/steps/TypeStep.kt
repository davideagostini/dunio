/**
 * First step of the Wear quick-entry wizard: choose the transaction type.
 *
 * This screen presents two full-width action buttons — "Expense" and
 * "Income" — with emoji icons. When pending entries exist in the Data Layer
 * queue (i.e. waiting to sync with the phone), a [QueueStatusChip] is
 * displayed above the buttons to inform the user.
 *
 * Tapping a button dispatches [WearQuickEntryAction.SelectType] which
 * triggers the ViewModel to advance to the Amount step and start loading
 * categories for the chosen type.
 */
package com.davideagostini.summ.wearapp.ui.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.davideagostini.summ.wearapp.R
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryAction
import com.davideagostini.summ.wearapp.theme.WearThemeTokens
import com.davideagostini.summ.wearapp.ui.QueueStatusChip
import com.davideagostini.summ.wearapp.ui.WearActionButton

/**
 * Composable for the transaction-type selection screen.
 *
 * Layout structure (top to bottom):
 * 1. Title text ("What do you want to add?").
 * 2. Optional [QueueStatusChip] if [pendingCount] > 0.
 * 3. "Expense" button with money-wings emoji.
 * 4. "Income" button with money-bag emoji.
 * 5. Bottom spacer for scroll breathing room.
 *
 * @param pendingCount Number of entries queued for deferred sync.
 * @param onAction     Callback to dispatch user actions to the ViewModel.
 */
@Composable
internal fun TypeStep(
    pendingCount: Int,
    onAction: (WearQuickEntryAction) -> Unit,
) {
    val state = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = state,
    ) { contentPadding ->
        TransformingLazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = contentPadding.calculateTopPadding() + WearStepTopInset,
                bottom = contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Text(
                    text = stringResource(R.string.wear_type_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = WearThemeTokens.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                )
            }
            if (pendingCount > 0) {
                item {
                    QueueStatusChip(pendingCount = pendingCount)
                }
            }
            item {
                WearActionButton(
                    label = stringResource(R.string.wear_type_expense),
                    secondaryLabel = null,
                    iconEmoji = "\uD83D\uDCB8",
                    onClick = { onAction(WearQuickEntryAction.SelectType("expense")) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WearThemeTokens.surfaceContainerHigh,
                        contentColor = WearThemeTokens.onBackground,
                        secondaryContentColor = WearThemeTokens.onBackground.copy(alpha = 0.72f),
                        iconColor = WearThemeTokens.onBackground,
                    ),
                )
            }
            item {
                WearActionButton(
                    label = stringResource(R.string.wear_type_income),
                    secondaryLabel = null,
                    iconEmoji = "\uD83D\uDCB0",
                    onClick = { onAction(WearQuickEntryAction.SelectType("income")) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WearThemeTokens.surfaceContainerHigh,
                        contentColor = WearThemeTokens.onBackground,
                        secondaryContentColor = WearThemeTokens.onBackground.copy(alpha = 0.72f),
                        iconColor = WearThemeTokens.onBackground,
                    ),
                )
            }
            item {
                Spacer(Modifier.height(88.dp))
            }
        }
    }
}
