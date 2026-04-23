package com.davideagostini.summ.ui.navigation

/**
 * Visibility rules for shell-owned chrome such as the bottom bar.
 *
 * Today these rules are still evaluated by [AppNavGraph], but the business rule
 * is no longer expressed as a pile of route-string checks. Instead, visibility
 * is derived from typed navigation keys plus a small number of shell overlays.
 *
 * This makes future work easier in two ways:
 * 1. the rules read like product behavior rather than low-level route plumbing
 * 2. once Navigation 3 fully owns rendering, the same rules can survive with
 *    little or no change because they already operate on [AppNavKey]
 */

/**
 * Whether a destination is considered a "bottom bar root" from the shell's
 * point of view.
 *
 * Top-level tabs show the bottom bar. Child settings screens do not, because
 * they are detail flows layered above the settings root.
 */
fun AppNavKey.showsBottomBar(): Boolean = when (this) {
    DashboardKey,
    EntriesKey,
    AssetsKey,
    SettingsKey -> true

    CurrencyKey,
    LanguageKey,
    ThemeKey,
    ExportDataKey,
    MembersKey,
    CategoriesKey,
    RecurringKey,
    MonthCloseKey -> false
}

/**
 * Centralized bottom-bar visibility policy.
 *
 * The bottom bar must disappear not only for child destinations, but also when
 * shell-level overlays compete for attention. Keeping that policy in one helper
 * prevents it from being re-implemented differently across the codebase.
 */
fun shouldShowBottomBar(
    currentKey: AppNavKey?,
    showDashboardGetStarted: Boolean,
    showMonthPickerOverlay: Boolean,
    showEntriesFullscreenEditor: Boolean,
    showAssetsFullscreenEditor: Boolean,
): Boolean {
    val destinationAllowsBottomBar = currentKey?.showsBottomBar() ?: false
    return destinationAllowsBottomBar &&
        !showDashboardGetStarted &&
        !showMonthPickerOverlay &&
        !showEntriesFullscreenEditor &&
        !showAssetsFullscreenEditor
}
