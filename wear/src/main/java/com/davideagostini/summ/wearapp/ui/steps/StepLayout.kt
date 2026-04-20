/**
 * Shared layout constants for the quick-entry step screens.
 *
 * [WearStepTopInset] provides a uniform top padding applied by every step
 * composable to avoid content being clipped by the Wear OS time indicator
 * or the screen scaffold top inset.
 */
package com.davideagostini.summ.wearapp.ui.steps

import androidx.compose.ui.unit.dp

/**
 * Vertical padding added between the scaffold top inset and the first
 * item in each step's [TransformingLazyColumn]. This ensures consistent
 * spacing across all wizard screens.
 */
internal val WearStepTopInset = 16.dp
