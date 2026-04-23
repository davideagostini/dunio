package com.davideagostini.summ.ui.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator

/**
 * Explicit navigation state holder for the future Navigation 3 shell.
 *
 * The current app still renders through Navigation 2, so this class is not yet
 * wired into [AppNavGraph]. It exists now to make the migration incremental:
 * we can first model the state cleanly, then swap the UI host in a later phase.
 *
 * Responsibilities of this class
 * 1. keep track of the currently selected top-level tab
 * 2. keep one back stack per top-level tab
 * 3. expose helpers for the basic shell operations the current app performs:
 *    - switch tab
 *    - push a child destination
 *    - pop the current stack
 *    - reset the current stack to its root
 *    - expose the list of entries that should currently be rendered by NavDisplay
 *
 * What this class does *not* do
 * - it does not render any UI
 * - it does not know about auth gating
 * - it does not manage overlays such as the quick-entry sheet or month picker
 * - it does not yet replicate the full bottom-bar visibility rules
 *
 * Keeping those concerns out of this class is intentional: the goal is to make
 * the back-stack model easy to understand and easy to test in isolation.
 */
@Stable
class AppNavigationState(
    dashboardBackStack: NavBackStack<NavKey>,
    entriesBackStack: NavBackStack<NavKey>,
    assetsBackStack: NavBackStack<NavKey>,
    settingsBackStack: NavBackStack<NavKey>,
    initialTopLevel: TopLevelKey,
) {
    /** The tab that represents the app's conceptual home. */
    private val startTopLevel: TopLevelKey = DashboardKey

    /**
     * In-memory mapping between a top-level tab and the stack that belongs to it.
     *
     * Each stack is preserved while the user moves between tabs. This mirrors the
     * current Navigation 2 behavior where tab navigation uses `saveState` and
     * `restoreState` to avoid throwing away each feature's internal UI state.
     */
    private val backStacks: Map<TopLevelKey, NavBackStack<NavKey>> = mapOf(
        DashboardKey to dashboardBackStack,
        EntriesKey to entriesBackStack,
        AssetsKey to assetsBackStack,
        SettingsKey to settingsBackStack,
    )

    /**
     * Currently selected top-level destination.
     *
     * In phase 1 this value is kept in regular Compose state because the shell is
     * not yet migrated. Once Navigation 3 replaces the production host, this can
     * be promoted to a saveable representation if we decide the selected tab must
     * survive process death independently from the back stacks themselves.
     */
    var selectedTopLevel by mutableStateOf(initialTopLevel)
        private set

    /**
     * Back stack associated with the currently selected top-level tab.
     *
     * Future `NavDisplay` integration will render this stack directly.
     */
    val currentBackStack: NavBackStack<NavKey>
        get() = backStacks.getValue(selectedTopLevel)

    /**
     * Current destination key for the active tab.
     *
     * Returns `null` only if a stack were unexpectedly empty. Under normal usage
     * each top-level stack always contains at least its root key.
     */
    val currentKeyOrNull: AppNavKey?
        get() = currentBackStack.lastOrNull() as? AppNavKey

    /**
     * Non-null convenience view over [currentKeyOrNull].
     *
     * Every stack is created with a root element, so a missing current key would
     * indicate a programming error in the state holder rather than a normal runtime
     * condition.
     */
    val currentKey: AppNavKey
        get() = checkNotNull(currentKeyOrNull) {
            "Navigation stack for ${selectedTopLevel.stableId} is unexpectedly empty."
        }

    /**
     * Switches the active top-level tab without mutating any stack contents.
     *
     * This reproduces the user's mental model of the bottom bar: tapping a tab
     * changes which preserved stack is being displayed, but it should not throw
     * away in-progress UI state inside the previously selected tab.
     */
    fun selectTopLevel(target: TopLevelKey) {
        selectedTopLevel = target
    }

    /**
     * Pushes a child destination onto the currently selected stack.
     *
     * This helper is the Navigation 3 equivalent of `navController.navigate(...)`
     * for secondary screens such as Currency, Members, or Month Close.
     *
     * We keep the method narrow on purpose:
     * - callers pass a typed key
     * - the state holder decides which stack receives it
     */
    fun push(key: AppNavKey) {
        currentBackStack.add(key)
    }

    /**
     * Pops the current stack if there is a child destination above the root.
     *
     * @return `true` when something was removed, `false` when the active stack was
     * already at its root destination.
     *
     * The root key is never removed here because top-level navigation should stay
     * stable: the shell is expected to decide separately what "back from a root
     * tab" means for the overall app.
     */
    fun popCurrent(): Boolean {
        if (currentBackStack.size <= 1) return false
        currentBackStack.removeAt(currentBackStack.lastIndex)
        return true
    }

    /**
     * Handles app-level "back" behavior using the official Navigation 3 recipe's
     * "exit through home" pattern.
     *
     * Behavior:
     * - if the current stack has child destinations, pop one
     * - if we are at the root of a non-home tab, switch back to Dashboard
     * - if we are already at Dashboard root, report that back is no longer handled
     *
     * @return `true` if the state holder consumed the back action, `false` if the
     * shell should delegate to the Activity (which will usually finish the screen).
     */
    fun goBack(): Boolean {
        val currentStack = currentBackStack
        return when {
            currentStack.size > 1 -> {
                currentStack.removeAt(currentStack.lastIndex)
                true
            }

            selectedTopLevel != startTopLevel -> {
                selectedTopLevel = startTopLevel
                true
            }

            else -> false
        }
    }

    /**
     * Resets the active tab's stack to its root key.
     *
     * This is the Navigation 3 equivalent of "reselect current tab and jump back
     * to its start destination". It will be useful for behaviors such as tapping
     * the already-selected tab or dismissing deep settings flows back to the tab
     * root in one operation.
     */
    fun resetSelectedTopLevel() {
        val root = selectedTopLevel
        val stack = currentBackStack
        stack.clear()
        stack.add(root)
    }

    /**
     * Returns the preserved stack associated with a specific top-level tab.
     *
     * This method is intentionally read-only from the caller's perspective. It is
     * mainly useful for future debugging, previews, or test helpers that need to
     * inspect whether a tab is retaining its own navigation history correctly.
     */
    fun backStackFor(key: TopLevelKey): NavBackStack<NavKey> = backStacks.getValue(key)

    /**
     * Returns the list of top-level stacks that currently contribute visible
     * entries to the shell.
     *
     * We follow the official Navigation 3 multiple-back-stack recipe's "exit
     * through home" pattern:
     * - Dashboard is always retained as the first visible stack
     * - when another tab is selected, the rendered entries become
     *   Dashboard + SelectedTab
     *
     * This gives us predictable back behavior without discarding off-screen tab
     * state and keeps the migration aligned with the official guidance.
     */
    private fun topLevelRoutesInUse(): List<TopLevelKey> =
        if (selectedTopLevel == startTopLevel) {
            listOf(startTopLevel)
        } else {
            listOf(startTopLevel, selectedTopLevel)
        }

    /**
     * Builds the decorated Navigation 3 entries that should currently be rendered.
     *
     * This function is composable because each top-level back stack needs its own
     * remembered decorator chain. That is especially important for:
     * - `rememberSaveable` state inside screens
     * - `ViewModelStoreOwner` scoping required by `hiltViewModel()`
     *
     * We decorate every top-level stack independently, then concatenate only the
     * stacks that are currently "in use" according to [topLevelRoutesInUse].
     * This mirrors the official multiple-back-stack recipe and preserves tab state
     * even when a tab is not the one currently shown to the user.
     */
    @Composable
    fun toDecoratedEntries(
        entryProvider: (NavKey) -> NavEntry<NavKey>,
    ): List<NavEntry<NavKey>> {
        val decoratedEntries = backStacks.mapValues { (_, stack) ->
            val decorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                rememberViewModelStoreNavEntryDecorator<NavKey>(),
            )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider,
            )
        }

        return topLevelRoutesInUse()
            .flatMap { topLevel -> decoratedEntries[topLevel].orEmpty() }
    }
}
