package com.davideagostini.summ.ui.categories.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davideagostini.summ.R
import com.davideagostini.summ.ui.auth.components.AuthErrorCard
import com.davideagostini.summ.ui.categories.CategoriesEvent
import com.davideagostini.summ.ui.categories.CategoriesUiState
import com.davideagostini.summ.ui.categories.CategorySheetMode
import com.davideagostini.summ.ui.theme.AppButtonShape
import com.davideagostini.summ.ui.theme.IncomeGreen

@Composable
internal fun CategoryEditorScreen(
    uiState: CategoriesUiState,
    onEvent: (CategoriesEvent) -> Unit,
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
                CategorySheetMode.Add -> CategoryFormContent(
                    title = stringResource(R.string.category_new_title),
                    confirmLabel = stringResource(R.string.action_create),
                    uiState = uiState,
                    onEvent = onEvent,
                    onSave = { onEvent(CategoriesEvent.SaveAdd) },
                    onCancel = onDismiss,
                )

                CategorySheetMode.Edit -> CategoryFormContent(
                    title = stringResource(R.string.category_edit_title),
                    confirmLabel = stringResource(R.string.action_save),
                    uiState = uiState,
                    onEvent = onEvent,
                    onSave = { onEvent(CategoriesEvent.SaveEdit) },
                    onCancel = onDismiss,
                )

                // Save success remains in the fullscreen presentation so it never collapses back into a sheet.
                CategorySheetMode.Success -> CategoryEditorSuccessContent()
                else -> Unit
            }
        }
    }
}

@Composable
private fun CategoryFormContent(
    title: String,
    confirmLabel: String,
    uiState: CategoriesUiState,
    onEvent: (CategoriesEvent) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    // The form body scrolls independently so the bottom actions remain fixed and reachable.
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(16.dp))

            uiState.operationErrorMessage?.let { message ->
                AuthErrorCard(message)
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = uiState.editName,
                onValueChange = { onEvent(CategoriesEvent.UpdateFormName(it)) },
                label = { Text(stringResource(R.string.category_name_label)) },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { msg -> { Text(msg) } },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.category_emoji_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            // The current emoji stays visible while the user keeps scrolling through the picker grid.
            if (uiState.editEmoji.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(uiState.editEmoji, fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.category_selected_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(4.dp))

            EmojiPickerGrid(
                selectedEmoji = uiState.editEmoji,
                onEmojiSelected = { onEvent(CategoriesEvent.UpdateFormEmoji(it)) },
            )

            Spacer(Modifier.height(16.dp))
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
                    shape = AppButtonShape,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = onSave,
                    shape = AppButtonShape,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(confirmLabel)
                }
            }
        }
    }
}

@Composable
private fun CategoryEditorSuccessContent() {
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

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.done_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.category_done_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
