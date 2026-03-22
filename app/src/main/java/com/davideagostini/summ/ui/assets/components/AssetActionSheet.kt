package com.davideagostini.summ.ui.assets.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.assets.AssetSheetMode
import com.davideagostini.summ.ui.assets.AssetsEvent
import com.davideagostini.summ.ui.assets.AssetsUiState
import com.davideagostini.summ.ui.assets.formatCurrency
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen

@Composable
fun AssetActionSheet(
    uiState: AssetsUiState,
    onEvent: (AssetsEvent) -> Unit,
    onDismiss: () -> Unit,
) {
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
            AnimatedContent(targetState = uiState.sheetMode, label = "asset_sheet_mode") { mode ->
                when (mode) {
                    AssetSheetMode.Action -> AssetActionContent(uiState = uiState, onEvent = onEvent)
                    AssetSheetMode.Add -> AssetFormContent(
                        title = stringResource(R.string.asset_new_title),
                        confirmLabel = stringResource(R.string.action_create),
                        uiState = uiState,
                        onEvent = onEvent,
                        onSave = { onEvent(AssetsEvent.SaveAdd) },
                        onCancel = onDismiss,
                    )
                    AssetSheetMode.Edit -> AssetFormContent(
                        title = stringResource(R.string.asset_edit_title),
                        confirmLabel = stringResource(R.string.action_save),
                        uiState = uiState,
                        onEvent = onEvent,
                        onSave = { onEvent(AssetsEvent.SaveEdit) },
                        onCancel = onDismiss,
                    )
                    AssetSheetMode.Success -> AssetSuccessContent()
                    AssetSheetMode.Hidden -> Unit
                }
            }
        }
    }
}

@Composable
private fun AssetActionContent(
    uiState: AssetsUiState,
    onEvent: (AssetsEvent) -> Unit,
) {
    val asset = uiState.selectedAsset ?: return
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null)
        }

        Spacer(Modifier.size(12.dp))
        Text(text = asset.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(6.dp))
        Text(
            text = formatCurrency(asset.value, asset.currency),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { onEvent(AssetsEvent.RequestDelete) },
                modifier = Modifier.weight(1f),
                shape = AppButtonShape,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(4.dp))
                Text(stringResource(R.string.action_delete), color = ExpenseRed)
            }

            Button(
                onClick = { onEvent(AssetsEvent.StartEdit) },
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
private fun AssetFormContent(
    title: String,
    confirmLabel: String,
    uiState: AssetsUiState,
    onEvent: (AssetsEvent) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(16.dp))

        AssetTypeButtonGroup(
            selectedType = uiState.editType,
            onSelectType = { onEvent(AssetsEvent.UpdateType(it)) },
        )

        Spacer(Modifier.size(12.dp))
        OutlinedTextField(
            value = uiState.editName,
            onValueChange = { onEvent(AssetsEvent.UpdateName(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.asset_name_label)) },
            singleLine = true,
            isError = uiState.nameError != null,
            supportingText = uiState.nameError?.let { msg -> { Text(msg) } },
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.size(12.dp))
        OutlinedTextField(
            value = uiState.editCategory,
            onValueChange = { onEvent(AssetsEvent.UpdateCategory(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.asset_category_label)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.size(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = uiState.editValue,
                onValueChange = { onEvent(AssetsEvent.UpdateValue(it)) },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.asset_value_label)) },
                singleLine = true,
                isError = uiState.valueError != null,
                supportingText = uiState.valueError?.let { msg -> { Text(msg) } },
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                value = uiState.editCurrency,
                onValueChange = { onEvent(AssetsEvent.UpdateCurrency(it)) },
                modifier = Modifier.weight(0.42f),
                label = { Text(stringResource(R.string.asset_currency_label)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
        }

        Spacer(Modifier.size(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = AppButtonShape,
            ) {
                Text(stringResource(R.string.action_cancel))
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                shape = AppButtonShape,
            ) {
                Text(confirmLabel)
            }
        }
    }
}

@Composable
private fun AssetTypeButtonGroup(
    selectedType: String,
    onSelectType: (String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf(
            "asset" to stringResource(R.string.asset_type_asset),
            "liability" to stringResource(R.string.asset_type_liability),
        ).forEachIndexed { index, item ->
            val selected = selectedType == item.first
            Button(
                onClick = { onSelectType(item.first) },
                modifier = Modifier.weight(1f),
                shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp, topEnd = 6.dp, bottomEnd = 6.dp)
                    else -> RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(item.second)
            }
        }
    }
}

@Composable
private fun AssetSuccessContent() {
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
        Text(text = "Done!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            text = "Asset updated successfully.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
