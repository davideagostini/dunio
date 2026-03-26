package com.davideagostini.summ.ui.assets.components

// Presentational sheet for the assets feature: this file renders the compact
// action state, the transient success state, and the reusable action buttons.
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.assets.AssetSheetMode
import com.davideagostini.summ.ui.assets.AssetsEvent
import com.davideagostini.summ.ui.assets.AssetsUiState
import com.davideagostini.summ.ui.auth.components.AuthErrorCard
import com.davideagostini.summ.ui.components.MonthCloseReadOnlyBanner
import com.davideagostini.summ.ui.format.formatCurrency
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen

@Composable
fun AssetActionSheet(
    uiState: AssetsUiState,
    currency: String,
    readOnly: Boolean,
    readOnlyMessage: String,
    onEvent: (AssetsEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    // The card acts as the sheet surface and keeps the action flow visually
    // separate from the rest of the screen content.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
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

            // The sheet animates between action and success content so the delete
            // confirmation feels like part of the same flow instead of a reset.
            AnimatedContent(
                targetState = uiState.sheetMode,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    if (forward) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "asset_sheet_mode",
            ) { mode ->
                when (mode) {
                    AssetSheetMode.Action -> AssetActionContent(
                        uiState = uiState,
                        currency = currency,
                        readOnly = readOnly,
                        readOnlyMessage = readOnlyMessage,
                        onEvent = onEvent,
                    )
                    // Add/Edit now live in a dedicated fullscreen editor composable.
                    AssetSheetMode.Success -> AssetSuccessContent()
                    AssetSheetMode.Hidden,
                    AssetSheetMode.Add,
                    AssetSheetMode.Edit -> Unit
                }
            }
        }
    }
}

@Composable
private fun AssetActionContent(
    uiState: AssetsUiState,
    currency: String,
    readOnly: Boolean,
    readOnlyMessage: String,
    onEvent: (AssetsEvent) -> Unit,
) {
    val asset = uiState.selectedAsset ?: return
    // The action content focuses on the selected asset and exposes the two main
    // actions in a compact, mobile-friendly row.
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        uiState.operationErrorMessage?.let { message ->
            AuthErrorCard(message)
            Spacer(Modifier.size(12.dp))
        }

        if (readOnly) {
            MonthCloseReadOnlyBanner(readOnlyMessage)
            Spacer(Modifier.size(12.dp))
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null)
        }

        Spacer(Modifier.size(12.dp))
        Text(
            text = asset.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = formatCurrency(asset.value, currency),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The delete button inherits the red content color so icon and label
            // stay consistent with the destructive action styling.
            OutlinedButton(
                onClick = { onEvent(AssetsEvent.RequestDelete) },
                enabled = !readOnly,
                modifier = Modifier.weight(1f),
                shape = AppButtonShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.action_delete))
            }

            Button(
                onClick = { onEvent(AssetsEvent.StartEdit) },
                enabled = !readOnly,
                modifier = Modifier.weight(1f),
                shape = AppButtonShape,
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.action_edit))
            }
        }
    }
}

@Composable
private fun AssetSuccessContent() {
    // Success mirrors the categories flow: a centered confirmation icon followed
    // by the title and the short feedback message.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(IncomeGreen.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", fontSize = 36.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
        }
        Text(
            text = stringResource(R.string.done_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.asset_done_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
