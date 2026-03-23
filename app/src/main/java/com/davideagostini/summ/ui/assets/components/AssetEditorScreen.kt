package com.davideagostini.summ.ui.assets.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.assets.AssetSheetMode
import com.davideagostini.summ.ui.assets.AssetsEvent
import com.davideagostini.summ.ui.assets.AssetsUiState
import com.davideagostini.summ.ui.auth.components.AuthErrorCard
import com.davideagostini.summ.ui.components.MonthCloseReadOnlyBanner
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.IncomeGreen

@Composable
internal fun AssetEditorScreen(
    uiState: AssetsUiState,
    readOnly: Boolean,
    readOnlyMessage: String,
    onEvent: (AssetsEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .navigationBarsPadding()
            .imePadding(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .systemBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.content_desc_close))
                }
            }

            when (uiState.sheetMode) {
                AssetSheetMode.Add -> AssetFormContent(
                    title = stringResource(R.string.asset_new_title),
                    confirmLabel = stringResource(R.string.action_create),
                    readOnly = readOnly,
                    readOnlyMessage = readOnlyMessage,
                    uiState = uiState,
                    onEvent = onEvent,
                    onSave = { onEvent(AssetsEvent.SaveAdd) },
                    onCancel = onDismiss,
                )

                AssetSheetMode.Edit -> AssetFormContent(
                    title = stringResource(R.string.asset_edit_title),
                    confirmLabel = stringResource(R.string.action_save),
                    readOnly = readOnly,
                    readOnlyMessage = readOnlyMessage,
                    uiState = uiState,
                    onEvent = onEvent,
                    onSave = { onEvent(AssetsEvent.SaveEdit) },
                    onCancel = onDismiss,
                )

                // Save success remains on the fullscreen screen to avoid snapping back into the narrow sheet.
                AssetSheetMode.Success -> AssetEditorSuccessContent()
                else -> Unit
            }
        }
    }
}

@Composable
private fun AssetFormContent(
    title: String,
    confirmLabel: String,
    readOnly: Boolean,
    readOnlyMessage: String,
    uiState: AssetsUiState,
    onEvent: (AssetsEvent) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    // The asset editor now matches the entry editor: scrollable content with a fixed bottom action row.
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(16.dp))

            uiState.operationErrorMessage?.let { message ->
                AuthErrorCard(message)
                Spacer(Modifier.size(12.dp))
            }

            if (readOnly) {
                MonthCloseReadOnlyBanner(readOnlyMessage)
                Spacer(Modifier.size(12.dp))
            }

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
        }

        Surface(
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
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
                    enabled = !readOnly,
                    modifier = Modifier.weight(1f),
                    shape = AppButtonShape,
                ) {
                    Text(confirmLabel)
                }
            }
        }
    }
}

@Composable
private fun AssetEditorSuccessContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = IncomeGreen.copy(alpha = 0.15f),
            ) {}
            Text("✓", fontSize = 36.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.done_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.asset_done_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
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
