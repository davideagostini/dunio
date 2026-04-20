/**
 * Theme infrastructure for the Wear OS quick-entry module.
 *
 * This file provides:
 * - [WearThemeTokens]: a centralised set of semantic colour tokens that
 *   reference [MaterialTheme.colorScheme]. This indirection allows every
 *   composable to use named tokens (e.g. `WearThemeTokens.surfaceContainerHigh`)
 *   instead of hardcoded colours, making future theme changes trivial.
 * - [DunioWearTheme]: the top-level theme composable that installs the
 *   dark colour scheme via Wear Compose Material 3's [MaterialTheme].
 *
 * The colour palette is designed for dark backgrounds on round Wear displays,
 * with high-contrast foreground colours for readability at small sizes.
 */
package com.davideagostini.summ.wearapp.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/**
 * Semantic colour tokens used across the Wear quick-entry UI.
 *
 * Each property reads from [MaterialTheme.colorScheme] so that the actual
 * colour values are controlled in a single place ([fallback]). If dynamic
 * colour is ever enabled (e.g. Material You on Wear), the tokens will
 * automatically pick up the system palette.
 *
 * The [fallback] property defines the hard-coded dark palette used in V1.
 */
internal object WearThemeTokens {

    /**
     * Hard-coded dark colour scheme for the Wear app.
     *
     * These colours were chosen to provide:
     * - A very dark background (0xFF0E1116) that blends with the watch bezel.
     * - High-contrast text (0xFFF1F4F8) for legibility at small sizes.
     * - Subtle surface elevation through progressively lighter greys.
     * - A soft lavender primary (0xFFB8ACE8) that works well on dark surfaces.
     * - A warm error colour (0xFFFFB4AB) for validation messages.
     */
    val fallback = ColorScheme().copy(
        background = Color(0xFF0E1116),
        onBackground = Color(0xFFF1F4F8),
        surfaceContainer = Color(0xFF1B212A),
        surfaceContainerHigh = Color(0xFF262D38),
        outlineVariant = Color(0xFF464F5D),
        primary = Color(0xFFB8ACE8),
        onPrimary = Color(0xFF2D2445),
        secondaryContainer = Color(0xFF313646),
        onSecondaryContainer = Color(0xFFF1F4F8),
        error = Color(0xFFFFB4AB),
    )

    /** Screen background colour. */
    val background: Color
        @Composable get() = MaterialTheme.colorScheme.background

    /** Lower-elevation surface colour (used for cards, input fields). */
    val surfaceContainer: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainer

    /** Higher-elevation surface colour (used for action buttons). */
    val surfaceContainerHigh: Color
        @Composable get() = MaterialTheme.colorScheme.surfaceContainerHigh

    /** Border / divider colour. */
    val outlineVariant: Color
        @Composable get() = MaterialTheme.colorScheme.outlineVariant

    /** Primary accent colour (used for icons, highlights). */
    val primary: Color
        @Composable get() = MaterialTheme.colorScheme.primary

    /** Foreground colour on primary surfaces. */
    val onPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onPrimary

    /** Main text / foreground colour on background surfaces. */
    val onBackground: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground

    /** Error text colour (used for validation and transport errors). */
    val error: Color
        @Composable get() = MaterialTheme.colorScheme.error
}

/**
 * Top-level theme composable for the Dunio Wear app.
 *
 * Wraps all content with Wear Compose Material 3's [MaterialTheme],
 * applying the [WearThemeTokens.fallback] dark colour scheme. This is
 * the single place where the colour scheme is installed; all children
 * access colours through [WearThemeTokens] or [MaterialTheme.colorScheme].
 *
 * @param content The composable content tree to theme.
 */
@Composable
internal fun DunioWearTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = WearThemeTokens.fallback,
        content = content,
    )
}
