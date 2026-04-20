/**
 * Reusable UI components for the Wear OS quick-entry flow.
 *
 * This file contains shared composables used across multiple step screens:
 * - [WearActionButton]: full-width button with optional emoji icon and secondary label.
 * - [BackTextButton]: lightweight "Back" button using the child-button style.
 * - [AmountField]: styled text input for entering monetary amounts.
 * - [QueueStatusChip]: pill-shaped chip showing the count of pending entries.
 * - [SummaryPanel]: bordered card used on the confirmation screen.
 * - [ConfirmValue]: label–value row used inside [SummaryPanel].
 *
 * All composables are `internal` to the Wear app module.
 */
package com.davideagostini.summ.wearapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.davideagostini.summ.wearapp.R
import com.davideagostini.summ.wearapp.theme.WearThemeTokens

/**
 * Full-width action button used throughout the quick-entry wizard.
 *
 * Displays a primary [label], an optional [secondaryLabel] below it,
 * and an optional [iconEmoji] inside a circular badge to the left.
 * The button is designed to fill the available width so it is easy to
 * tap on a small watch screen.
 *
 * @param label          Primary text displayed on the button.
 * @param secondaryLabel Optional second line of text (e.g. a hint). Not shown if null.
 * @param iconEmoji      Optional emoji rendered inside a circular icon badge.
 * @param centeredLabel  When true, centres the label text instead of left-aligning it.
 * @param onClick        Callback invoked when the button is tapped.
 * @param colors         Button colour configuration; falls back to filled-tonal defaults if null.
 */
@Composable
internal fun WearActionButton(
    label: String,
    secondaryLabel: String?,
    iconEmoji: String? = null,
    centeredLabel: Boolean = false,
    onClick: () -> Unit,
    colors: androidx.wear.compose.material3.ButtonColors?,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Text(
                text = label,
                maxLines = 1,
                textAlign = if (centeredLabel) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        icon = iconEmoji?.let {
            {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            color = WearThemeTokens.onBackground.copy(alpha = 0.14f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = it,
                        fontSize = 18.sp,
                        color = WearThemeTokens.onBackground,
                    )
                }
            }
        },
        secondaryLabel = secondaryLabel?.let {
            {
                Text(
                    text = it,
                    maxLines = 1,
                    color = WearThemeTokens.onBackground.copy(alpha = 0.72f),
                )
            }
        },
        colors = colors ?: ButtonDefaults.filledTonalButtonColors(
            containerColor = WearThemeTokens.surfaceContainerHigh,
            contentColor = WearThemeTokens.onBackground,
            secondaryContentColor = WearThemeTokens.onBackground.copy(alpha = 0.72f),
            iconColor = WearThemeTokens.onBackground,
        ),
    )
}

/**
 * Lightweight centred "Back" button for navigation within the wizard.
 *
 * Uses the Wear Material 3 child-button style (smaller tap target,
 * secondary visual weight) so it does not compete with the primary
 * action on each screen.
 *
 * @param onClick Callback invoked when the button is tapped.
 */
@Composable
internal fun BackTextButton(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.childButtonColors(
                contentColor = WearThemeTokens.onBackground,
                secondaryContentColor = WearThemeTokens.onBackground.copy(alpha = 0.72f),
                iconColor = WearThemeTokens.onBackground,
            ),
            label = {
                Text(
                    text = stringResource(R.string.wear_back),
                    textAlign = TextAlign.Center,
                )
            },
        )
    }
}

/**
 * Styled text input field for entering a monetary amount.
 *
 * Features:
 * - Rounded-corner bordered container matching the theme surface.
 * - Placeholder text shown when the field is empty.
 * - Centre-aligned, semi-bold text at 22 sp for readability on small screens.
 * - Decimal keyboard type to guide the system IME.
 * - Local [TextFieldValue] state synced with the external [value] via
 *   [LaunchedEffect] so cursor position is preserved during recompositions.
 *
 * The text colour adapts to the surface luminance (white on dark, black on light)
 * to remain legible regardless of the active colour scheme.
 *
 * @param value        Current text value controlled by the parent.
 * @param onValueChange Callback invoked on every keystroke with the new text.
 * @param placeholder  Hint text shown when the field is blank.
 */
@Composable
internal fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    val inputContentColor = if (WearThemeTokens.surfaceContainer.luminance() < 0.5f) {
        Color.White
    } else {
        Color.Black.copy(alpha = 0.92f)
    }

    /**
     * Syncs the external [value] into the local [TextFieldValue] whenever
     * they diverge. This happens when the parent resets the amount (e.g.
     * after a successful save) or when the ViewModel clears the field.
     */
    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = fieldValue.copy(text = value)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WearThemeTokens.outlineVariant, RoundedCornerShape(22.dp))
            .background(WearThemeTokens.surfaceContainer, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (fieldValue.text.isBlank()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.displaySmall,
                color = inputContentColor.copy(alpha = 0.34f),
                textAlign = TextAlign.Center,
            )
        }
        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                fieldValue = newValue
                onValueChange(newValue.text)
            },
            singleLine = true,
            cursorBrush = SolidColor(inputContentColor),
            textStyle = TextStyle(
                color = inputContentColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Pill-shaped chip that displays the number of entries currently queued
 * for deferred synchronisation with the phone.
 *
 * The chip is conditionally shown on the type-selection screen when
 * [pendingCount] is greater than zero. It uses a schedule icon to
 * visually communicate "pending / waiting" status.
 *
 * @param pendingCount Number of pending entries to display.
 */
@Composable
internal fun QueueStatusChip(
    pendingCount: Int,
) {
    Row(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = WearThemeTokens.outlineVariant,
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = WearThemeTokens.primary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.wear_queue_status, pendingCount),
            style = MaterialTheme.typography.labelSmall,
            color = WearThemeTokens.onBackground,
        )
    }
}

/**
 * Bordered card panel used on the confirmation screen to display
 * the entry summary (type, amount, category).
 *
 * Provides a rounded-corner container with a themed background and
 * a subtle border. Content is laid out vertically via [ColumnScope].
 *
 * @param content Composable content (typically a series of [ConfirmValue] rows).
 */
@Composable
internal fun SummaryPanel(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WearThemeTokens.surfaceContainer, RoundedCornerShape(22.dp))
            .border(1.dp, WearThemeTokens.outlineVariant, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content,
    )
}

/**
 * A single label–value row used inside [SummaryPanel] on the confirmation screen.
 *
 * The label is drawn on the left in a smaller, muted style and the value
 * on the right in the standard body style. This creates a key-value layout
 * that is easy to scan on the small watch display.
 *
 * @param label The field name (e.g. "Type", "Amount", "Category").
 * @param value The field value (e.g. "Expense", "€12.50", "🛒 Groceries").
 */
@Composable
internal fun ConfirmValue(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = WearThemeTokens.onBackground.copy(alpha = 0.68f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = WearThemeTokens.onBackground,
            textAlign = TextAlign.End,
        )
    }
}
