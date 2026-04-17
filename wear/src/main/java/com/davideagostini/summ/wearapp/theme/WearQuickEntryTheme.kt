package com.davideagostini.summ.wearapp.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

internal object WearThemeTokens {
    val fallback = ColorScheme().copy(
        background = Color(0xFFF2EEFB),
        onBackground = Color(0xFF2F2A39),
        surfaceContainer = Color(0xFFFFFFFF),
        surfaceContainerHigh = Color(0xFFE4DEF2),
        outlineVariant = Color(0xFFC7C0D8),
        primary = Color(0xFFB8ACE8),
        onPrimary = Color(0xFF2D2445),
        error = Color(0xFFBA1A1A),
    )

    val background: Color
        @Composable get() = MaterialTheme.colorScheme.background

    val surfaceContainer: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer

    val surfaceContainerHigh: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

    val outlineVariant: Color
        @Composable get() = MaterialTheme.colorScheme.outlineVariant

    val primary: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    val onPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimary

    val onBackground: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground

    val error: Color
        @Composable get() = MaterialTheme.colorScheme.error
}

@Composable
internal fun DunioWearTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = dynamicColorScheme(context) ?: WearThemeTokens.fallback
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
