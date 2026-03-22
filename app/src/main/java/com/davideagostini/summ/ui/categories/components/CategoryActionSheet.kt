package com.davideagostini.summ.ui.categories.components

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.davideagostini.summ.ui.categories.CategoriesEvent
import com.davideagostini.summ.ui.categories.CategoriesUiState
import com.davideagostini.summ.ui.categories.CategorySheetMode
import com.davideagostini.summ.ui.theme.ExpenseRed
import com.davideagostini.summ.ui.theme.IncomeGreen

@Composable
internal fun CategoryActionSheet(
    uiState: CategoriesUiState,
    onEvent: (CategoriesEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
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
            AnimatedContent(
                targetState    = uiState.sheetMode,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    if (forward) {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                label = "category_sheet_mode",
            ) { mode ->
                when (mode) {
                    CategorySheetMode.Action  -> ActionContent(uiState = uiState, onEvent = onEvent)
                    CategorySheetMode.Add     -> CategoryFormContent(
                        title         = stringResource(R.string.category_new_title),
                        confirmLabel  = stringResource(R.string.action_create),
                        uiState       = uiState,
                        onEvent       = onEvent,
                        onSave        = { onEvent(CategoriesEvent.SaveAdd) },
                        onCancel      = onDismiss,
                    )
                    CategorySheetMode.Edit    -> CategoryFormContent(
                        title         = stringResource(R.string.category_edit_title),
                        confirmLabel  = stringResource(R.string.action_save),
                        uiState       = uiState,
                        onEvent       = onEvent,
                        onSave        = { onEvent(CategoriesEvent.SaveEdit) },
                        onCancel      = onDismiss,
                    )
                    CategorySheetMode.Success -> SuccessContent()
                    CategorySheetMode.Hidden  -> Unit
                }
            }
        }
    }
}

// ── Action view ───────────────────────────────────────────────────────────────

@Composable
private fun ActionContent(uiState: CategoriesUiState, onEvent: (CategoriesEvent) -> Unit) {
    val cat = uiState.selectedCategory ?: return

    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape    = CircleShape,
            color    = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(cat.emoji, fontSize = 32.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text       = cat.name,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick  = { onEvent(CategoriesEvent.RequestDelete) },
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.action_delete))
            }

            Button(
                onClick  = { onEvent(CategoriesEvent.StartEdit) },
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.action_edit))
            }
        }
    }
}

// ── Shared form (Add + Edit) ──────────────────────────────────────────────────

@Composable
private fun CategoryFormContent(
    title: String,
    confirmLabel: String,
    uiState: CategoriesUiState,
    onEvent: (CategoriesEvent) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value          = uiState.editName,
            onValueChange  = { onEvent(CategoriesEvent.UpdateFormName(it)) },
            label          = { Text(stringResource(R.string.category_name_label)) },
            isError        = uiState.nameError != null,
            supportingText = uiState.nameError?.let { msg -> { Text(msg) } },
            singleLine     = true,
            shape          = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier       = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text  = stringResource(R.string.category_emoji_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(4.dp))

        // Preview of currently selected emoji
        if (uiState.editEmoji.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp),
            ) {
                Surface(
                    shape    = RoundedCornerShape(10.dp),
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(uiState.editEmoji, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = stringResource(R.string.category_selected_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(4.dp))

        EmojiPickerGrid(
            selectedEmoji    = uiState.editEmoji,
            onEmojiSelected  = { onEvent(CategoriesEvent.UpdateFormEmoji(it)) },
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick  = onCancel,
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.action_cancel)) }

            Button(
                onClick  = onSave,
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            ) { Text(confirmLabel) }
        }
    }
}

// ── Success ───────────────────────────────────────────────────────────────────

@Composable
private fun SuccessContent() {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier         = Modifier
                .size(72.dp)
                .background(IncomeGreen.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", fontSize = 36.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
        }

        Text(text = stringResource(R.string.done_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Text(
            text      = stringResource(R.string.category_done_message),
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))
    }
}
