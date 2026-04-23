package com.davideagostini.summ.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * Creates the future Navigation 3 state holder for the phone shell.
 *
 * Why this helper exists instead of constructing [AppNavigationState] inline:
 * - it keeps `AppNavGraph` smaller once the shell migration begins
 * - it centralizes how each top-level tab gets its own preserved back stack
 * - it makes the migration easier to review because all "remembered navigation
 *   state" logic lives in one focused file
 *
 * Why [rememberNavBackStack] is already used in phase 1
 * Even though the production UI still renders through Navigation 2, using the
 * Navigation 3 back-stack primitive here lets us validate the core state model
 * early. Each stack is process-death aware on Android as long as its keys are
 * serializable, which is why [AppNavKey] and all subtypes are declared with
 * `@Serializable`.
 */
@Composable
fun rememberAppNavigationState(
    initialTopLevel: TopLevelKey = DashboardKey,
): AppNavigationState {
    /**
     * One back stack per top-level tab.
     *
     * This layout mirrors the current app behavior more closely than a single
     * global stack would. The user expects Dashboard, Entries, Assets, and
     * Settings to retain their own local state when switching tabs.
     */
    val dashboardBackStack = rememberNavBackStack(DashboardKey)
    val entriesBackStack = rememberNavBackStack(EntriesKey)
    val assetsBackStack = rememberNavBackStack(AssetsKey)
    val settingsBackStack = rememberNavBackStack(SettingsKey)

    return remember(
        dashboardBackStack,
        entriesBackStack,
        assetsBackStack,
        settingsBackStack,
        initialTopLevel,
    ) {
        AppNavigationState(
            dashboardBackStack = dashboardBackStack,
            entriesBackStack = entriesBackStack,
            assetsBackStack = assetsBackStack,
            settingsBackStack = settingsBackStack,
            initialTopLevel = initialTopLevel,
        )
    }
}
