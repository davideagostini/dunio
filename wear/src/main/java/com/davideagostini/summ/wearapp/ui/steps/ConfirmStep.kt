package com.davideagostini.summ.wearapp.ui.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.ScrollIndicatorColors
import androidx.wear.compose.material3.Text
import com.davideagostini.summ.wearapp.R
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryAction
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryStep
import com.davideagostini.summ.wearapp.presentation.WearQuickEntryUiState
import com.davideagostini.summ.wearapp.theme.WearThemeTokens
import com.davideagostini.summ.wearapp.ui.ConfirmValue
import com.davideagostini.summ.wearapp.ui.SummaryPanel

@Composable
internal fun ConfirmStep(
    uiState: WearQuickEntryUiState,
    formattedAmount: String,
    onAction: (WearQuickEntryAction) -> Unit,
    onBack: () -> Unit,
) {
    val state = rememberTransformingLazyColumnState()
    var optimisticSaving by remember { mutableStateOf(false) }
    val showSaving = uiState.isSaving || optimisticSaving

    LaunchedEffect(uiState.isSaving, uiState.errorMessage, uiState.step) {
        if (uiState.step != WearQuickEntryStep.Confirm) {
            optimisticSaving = false
        } else if (!uiState.isSaving) {
            optimisticSaving = false
        }
    }

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
                    text = stringResource(R.string.wear_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                SummaryPanel {
                    ConfirmValue(
                        label = stringResource(R.string.wear_confirm_type),
                        value = if (uiState.type == "income") {
                            stringResource(R.string.wear_type_income)
                        } else {
                            stringResource(R.string.wear_type_expense)
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    ConfirmValue(
                        label = stringResource(R.string.wear_confirm_amount),
                        value = formattedAmount,
                    )
                    Spacer(Modifier.height(8.dp))
                    ConfirmValue(
                        label = stringResource(R.string.wear_confirm_category),
                        value = "${uiState.selectedCategory?.emoji.orEmpty()} ${uiState.selectedCategory?.name.orEmpty()}".trim(),
                    )
                }
            }
            if (showSaving) {
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(R.string.wear_saving),
                            style = MaterialTheme.typography.bodySmall,
                            color = WearThemeTokens.onBackground.copy(alpha = 0.78f),
                        )
                    }
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
            item { Spacer(Modifier.height(92.dp)) }
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
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .graphicsLayer {
                    alpha = if (showSaving) 0.88f else 1f
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlertDialogDefaults.DismissButton(
                onClick = { if (!showSaving) onBack() },
                modifier = Modifier.graphicsLayer {
                    scaleX = 0.9f
                    scaleY = 0.9f
                },
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color(0xFF263847),
                    contentColor = Color(0xFFD7EEF7),
                ),
            )
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    if (!showSaving) {
                        optimisticSaving = true
                        onAction(WearQuickEntryAction.Save)
                    }
                },
                modifier = Modifier
                    .padding(top = 2.dp)
                    .graphicsLayer {
                        scaleX = 0.9f
                        scaleY = 0.9f
                    },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0xFFBFE6FA),
                    contentColor = Color(0xFF1C3C4E),
                ),
                content = {
                    if (showSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = stringResource(R.string.wear_save),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
            )
        }
    }
}
