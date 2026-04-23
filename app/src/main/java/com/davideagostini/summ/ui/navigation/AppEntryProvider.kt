package com.davideagostini.summ.ui.navigation

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.davideagostini.summ.data.session.SessionState
import com.davideagostini.summ.ui.assets.AssetsRouteScreen
import com.davideagostini.summ.ui.categories.CategoriesScreen
import com.davideagostini.summ.ui.dashboard.DashboardScreen
import com.davideagostini.summ.ui.entries.EntriesRouteScreen
import com.davideagostini.summ.ui.settings.SettingsScreen
import com.davideagostini.summ.ui.settings.currency.CurrencyScreen
import com.davideagostini.summ.ui.settings.export.ExportScreen
import com.davideagostini.summ.ui.settings.language.LanguageScreen
import com.davideagostini.summ.ui.settings.members.MembersScreen
import com.davideagostini.summ.ui.settings.monthclose.MonthCloseScreen
import com.davideagostini.summ.ui.settings.recurring.RecurringScreen
import com.davideagostini.summ.ui.settings.theme.ThemeScreen

/**
 * Typed Navigation 3 entry mapping for the phone app.
 *
 * This file is the replacement for the old `NavHost { composable(...) }` block.
 * Each `entry<...>` declares:
 * - which typed key activates the destination
 * - which screen composable must be rendered
 * - which navigation callbacks mutate [AppNavigationState]
 *
 * Keeping the mapping here gives us a cleaner separation of concerns:
 * - [AppNavGraph] owns shell layout, auth gating, and overlays
 * - [AppNavigationState] owns back-stack data
 * - this file owns key-to-screen resolution
 */
fun appEntryProvider(
    navigationState: AppNavigationState,
    readyState: SessionState.Ready,
    sessionUiStateIsSubmitting: Boolean,
    sessionUiStateErrorMessage: String?,
    onUpdateHouseholdCurrency: (String) -> Unit,
    onConsumeSessionError: () -> Unit,
    onSignOut: () -> Unit,
    onOpenQuickEntry: () -> Unit,
    onEntriesFullscreenEditVisibilityChanged: (Boolean) -> Unit,
    onAssetsFullscreenEditVisibilityChanged: (Boolean) -> Unit,
    onMonthPickerVisibilityChanged: (Boolean) -> Unit,
    onDashboardGetStartedVisibilityChanged: (Boolean) -> Unit,
    openAssetsAddOnLaunch: Boolean,
    onAssetsAddConsumed: () -> Unit,
): (NavKey) -> NavEntry<NavKey> = entryProvider(
    fallback = { key ->
        NavEntry(key) {
            error("No Navigation 3 entry registered for key=$key")
        }
    }
) {
    /**
     * Dashboard remains a top-level root destination.
     *
     * Important note for future changes:
     * this is where the shell-level action "open asset add flow and switch tab"
     * is translated from UI intent into navigation-state mutation.
     */
    entry<DashboardKey> {
        DashboardScreen(
            onGetStartedVisibilityChanged = onDashboardGetStartedVisibilityChanged,
            onOpenQuickEntry = onOpenQuickEntry,
            onOpenNewAsset = {
                navigationState.selectTopLevel(AssetsKey)
            },
            onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
        )
    }

    /**
     * Entries top-level tab.
     *
     * Entries keeps its own fullscreen-edit state outside navigation because that
     * UI behaves like a shell-owned overlay rather than a route transition.
     */
    entry<EntriesKey> {
        EntriesRouteScreen(
            onFullscreenEditVisibilityChanged = onEntriesFullscreenEditVisibilityChanged,
            onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
        )
    }

    /**
     * Assets top-level tab.
     *
     * The "open add on next visit" behavior stays a shell concern and is simply
     * passed through when the Assets root is rendered.
     */
    entry<AssetsKey> {
        AssetsRouteScreen(
            onFullscreenEditVisibilityChanged = onAssetsFullscreenEditVisibilityChanged,
            onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
            openAddOnLaunch = openAssetsAddOnLaunch,
            onOpenAddConsumed = onAssetsAddConsumed,
        )
    }

    /**
     * Settings root.
     *
     * Child settings screens are now pushed onto the currently selected stack via
     * typed keys instead of string routes.
     */
    entry<SettingsKey> {
        SettingsScreen(
            onNavigateCurrency = { navigationState.push(CurrencyKey) },
            onNavigateLanguage = { navigationState.push(LanguageKey) },
            onNavigateTheme = { navigationState.push(ThemeKey) },
            onNavigateExport = { navigationState.push(ExportDataKey) },
            onNavigateCategories = { navigationState.push(CategoriesKey) },
            onNavigateMembers = { navigationState.push(MembersKey) },
            onNavigateRecurring = { navigationState.push(RecurringKey) },
            onNavigateMonthClose = { navigationState.push(MonthCloseKey) },
            onSignOut = onSignOut,
            householdName = readyState.household.name,
            householdId = readyState.household.id,
            householdCurrency = readyState.household.currency,
            userName = readyState.user.name,
            userPhotoUrl = readyState.user.photoUrl,
        )
    }

    /** Currency settings detail screen. */
    entry<CurrencyKey> {
        CurrencyScreen(
            selectedCurrency = readyState.household.currency,
            isUpdatingCurrency = sessionUiStateIsSubmitting,
            errorMessage = sessionUiStateErrorMessage,
            onSelectCurrency = onUpdateHouseholdCurrency,
            onDismissError = onConsumeSessionError,
            onBack = { navigationState.goBack() },
        )
    }

    /** Language settings detail screen. */
    entry<LanguageKey> {
        LanguageScreen(
            onBack = { navigationState.goBack() },
        )
    }

    /** Theme settings detail screen. */
    entry<ThemeKey> {
        ThemeScreen(
            onBack = { navigationState.goBack() },
        )
    }

    /** Data export detail screen. */
    entry<ExportDataKey> {
        ExportScreen(
            onBack = { navigationState.goBack() },
        )
    }

    /** Household members detail screen. */
    entry<MembersKey> {
        MembersScreen(
            householdId = readyState.household.id,
            currentUserId = readyState.user.uid,
            currentUserEmail = readyState.user.email,
            onBack = { navigationState.goBack() },
        )
    }

    /** Category management detail screen. */
    entry<CategoriesKey> {
        CategoriesScreen(
            onBack = { navigationState.goBack() },
        )
    }

    /** Recurring entries detail screen. */
    entry<RecurringKey> {
        RecurringScreen(
            onBack = { navigationState.goBack() },
        )
    }

    /** Month close detail screen. */
    entry<MonthCloseKey> {
        MonthCloseScreen(
            onBack = { navigationState.goBack() },
            onMonthPickerVisibilityChanged = onMonthPickerVisibilityChanged,
        )
    }
}
