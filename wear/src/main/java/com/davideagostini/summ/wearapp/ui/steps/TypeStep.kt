package com.davideagostini.summ.wearapp.ui.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.ScrollIndicatorColors
import androidx.wear.compose.material3.Text
import com.davideagostini.summ.wearapp.R
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryAction
import com.davideagostini.summ.wearapp.theme.WearThemeTokens
import com.davideagostini.summ.wearapp.ui.QueueStatusChip
import com.davideagostini.summ.wearapp.ui.WearActionButton

@Composable
internal fun TypeStep(
    pendingCount: Int,
    onAction: (WearQuickEntryAction) -> Unit,
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
                Text(
                    text = stringResource(R.string.wear_type_title),
                    style = MaterialTheme.typography.titleMedium,
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
    }
}
