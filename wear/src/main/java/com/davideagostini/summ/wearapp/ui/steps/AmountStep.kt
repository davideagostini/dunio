/**
 * Second step of the Wear quick-entry wizard: enter the monetary amount.
 *
 * This screen provides a styled text input field ([AmountField]) for the
 * user to type a decimal amount. If the user had already selected a category
 * from a previous flow iteration (e.g. they went back), the category name
 * and emoji are shown above the input field for context.
 *
 * An [EdgeButton] at the bottom of the screen acts as the "Continue" CTA.
 * Validation errors (e.g. non-positive amount) are displayed inline.
 *
 * Tapping Continue dispatches [WearQuickEntryAction.ContinueFromAmount].
 */
package com.davideagostini.summ.wearapp.ui.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.davideagostini.summ.wearapp.R
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryAction
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryUiState
import com.davideagostini.summ.wearapp.theme.WearThemeTokens
import com.davideagostini.summ.wearapp.ui.AmountField
import com.davideagostini.summ.wearapp.ui.BackTextButton

/**
 * Composable for the amount-entry screen.
 *
 * Layout structure (inside a Box for the bottom EdgeButton overlay):
 * - [TransformingLazyColumn] with:
 *   1. Title text ("Amount").
 *   2. Previously selected category (if any) for context.
 *   3. [AmountField] text input.
 *   4. Optional validation error message.
 *   5. [BackTextButton] to go back.
 * - [EdgeButton] pinned at the bottom centre as the "Continue" CTA.
 *
 * The list has extra bottom padding (72.dp) to prevent the EdgeButton
 * from overlapping the last list item.
 *
 * @param uiState  Current UI state containing the amount text and error messages.
 * @param onAction Callback to dispatch user actions to the ViewModel.
 * @param onBack   Callback invoked when the user presses the back button.
 */
@Composable
internal fun AmountStep(
    uiState: WearQuickEntryUiState,
    onAction: (WearQuickEntryAction) -> Unit,
    onBack: () -> Unit,
) {
    val state = rememberTransformingLazyColumnState()
    ScreenScaffold(
        scrollState = state,
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            TransformingLazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = contentPadding.calculateTopPadding() + WearStepTopInset,
                    bottom = contentPadding.calculateBottomPadding() + 72.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.wear_amount_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = WearThemeTokens.onBackground,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                uiState.selectedCategory?.let { category ->
                    item {
                        Text(
                            text = "${category.emoji} ${category.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = WearThemeTokens.onBackground.copy(alpha = 0.74f),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AmountField(
                            value = uiState.amount,
                            onValueChange = { onAction(WearQuickEntryAction.AmountChanged(it)) },
                            placeholder = stringResource(R.string.wear_amount_placeholder),
                        )
                    }
                }
                uiState.errorMessage?.let { message ->
                    item {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = WearThemeTokens.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                item {
                    BackTextButton(onClick = onBack)
                }
                item {
                    Spacer(Modifier.height(4.dp))
                }
            }
            EdgeButton(
                onClick = { onAction(WearQuickEntryAction.ContinueFromAmount) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 26.dp, vertical = 6.dp),
                buttonSize = EdgeButtonSize.Small,
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFBFE6FA),
                    contentColor = androidx.compose.ui.graphics.Color(0xFF1C3C4E),
                    secondaryContentColor = androidx.compose.ui.graphics.Color(0xFF1C3C4E).copy(alpha = 0.72f),
                    iconColor = androidx.compose.ui.graphics.Color(0xFF1C3C4E),
                ),
            ) {
                Text(
                    text = stringResource(R.string.wear_amount_cta),
                    color = androidx.compose.ui.graphics.Color(0xFF1C3C4E),
                    maxLines = 1,
                )
            }
        }
    }
}
