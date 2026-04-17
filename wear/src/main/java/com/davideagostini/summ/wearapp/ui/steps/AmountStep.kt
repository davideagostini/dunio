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
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.ScrollIndicatorColors
import androidx.wear.compose.material3.Text
import com.davideagostini.summ.wearapp.R
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryAction
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryUiState
import com.davideagostini.summ.wearapp.theme.WearThemeTokens
import com.davideagostini.summ.wearapp.ui.AmountField
import com.davideagostini.summ.wearapp.ui.BackTextButton

@Composable
internal fun AmountStep(
    uiState: WearQuickEntryUiState,
    onAction: (WearQuickEntryAction) -> Unit,
    onBack: () -> Unit,
) {
    val state = rememberTransformingLazyColumnState()
    Box(modifier = Modifier.fillMaxSize()) {
        TransformingLazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 44.dp),
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
                Spacer(Modifier.height(92.dp))
            }
        }
        ScrollIndicator(
            state = state,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp),
            colors = ScrollIndicatorColors(
                indicatorColor = WearThemeTokens.onBackground.copy(alpha = 0.92f),
                trackColor = WearThemeTokens.onBackground.copy(alpha = 0.20f),
            ),
        )
        EdgeButton(
            onClick = { onAction(WearQuickEntryAction.ContinueFromAmount) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 26.dp, vertical = 10.dp),
            buttonSize = EdgeButtonSize.Small,
            colors = ButtonDefaults.buttonColors(
                containerColor = WearThemeTokens.primary,
                contentColor = WearThemeTokens.onPrimary,
                secondaryContentColor = WearThemeTokens.onPrimary.copy(alpha = 0.72f),
                iconColor = WearThemeTokens.onPrimary,
            ),
        ) {
            Text(
                text = stringResource(R.string.wear_amount_cta),
                maxLines = 1,
            )
        }
    }
}
