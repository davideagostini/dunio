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
        )
    }
}

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
            textAlign = TextAlign.End,
        )
    }
}
