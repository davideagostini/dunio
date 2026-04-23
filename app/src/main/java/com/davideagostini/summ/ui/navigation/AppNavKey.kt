package com.davideagostini.summ.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Typed navigation keys for the phone app.
 *
 * The current production shell still uses Navigation 2 with string routes inside
 * [AppNavGraph]. This file is the first migration step toward Navigation 3:
 *
 * 1. all destinations that participate in the phone shell are declared in one place
 * 2. each destination is expressed as a Kotlin type instead of a raw route string
 * 3. the future Navigation 3 state holder can reason about top-level tabs and
 *    secondary settings screens without relying on fragile string comparisons
 *
 * Why these keys are `@Serializable`
 * Navigation 3 persists the back stack through `rememberNavBackStack(...)`.
 * The official API requires each key to implement [NavKey] and be serializable
 * so the stack can survive configuration changes and process death.
 *
 * Why the hierarchy is sealed
 * A sealed hierarchy keeps the set of valid destinations closed and discoverable.
 * This matters both for compile-time safety and for future serialization of the
 * back stack: all supported destinations are known in this file.
 */
@Serializable
sealed interface AppNavKey : NavKey

/**
 * Top-level tabs owned by the bottom navigation shell.
 *
 * These correspond to the four primary destinations the user can switch between
 * directly from the bottom bar. During the Navigation 3 migration, each of these
 * will get its own preserved back stack so tab switches can keep their local state
 * the same way the current Navigation 2 setup uses `saveState` / `restoreState`.
 */
@Serializable
sealed interface TopLevelKey : AppNavKey

/**
 * Root key for the dashboard tab.
 *
 * This is the app's conceptual "home" destination after auth/session gating.
 */
@Serializable
data object DashboardKey : TopLevelKey

/**
 * Root key for the entries tab.
 *
 * The entries feature already manages additional UI state internally, so the
 * shell only needs to know that this is the active top-level destination.
 */
@Serializable
data object EntriesKey : TopLevelKey

/**
 * Root key for the assets tab.
 *
 * This key anchors the stack that contains the assets list and any future
 * assets-specific child destinations we may decide to model as real navigation.
 */
@Serializable
data object AssetsKey : TopLevelKey

/**
 * Root key for the settings tab.
 *
 * All secondary settings destinations conceptually sit on top of this key.
 */
@Serializable
data object SettingsKey : TopLevelKey

/**
 * Secondary destinations that currently live "under" the settings tab.
 *
 * In the current Navigation 2 implementation these screens are addressed by
 * string routes such as `"currency"` or `"members"`. Moving them into a typed
 * hierarchy makes the future back-stack logic far easier to reason about.
 */
@Serializable
sealed interface SettingsChildKey : AppNavKey

/** Screen used to change the household currency. */
@Serializable
data object CurrencyKey : SettingsChildKey

/** Screen used to choose the app language. */
@Serializable
data object LanguageKey : SettingsChildKey

/** Screen used to choose the app theme. */
@Serializable
data object ThemeKey : SettingsChildKey

/** Screen used to export household data. */
@Serializable
data object ExportDataKey : SettingsChildKey

/** Screen used to manage household members. */
@Serializable
data object MembersKey : SettingsChildKey

/** Screen used to edit income and expense categories. */
@Serializable
data object CategoriesKey : SettingsChildKey

/** Screen used to manage recurring entries. */
@Serializable
data object RecurringKey : SettingsChildKey

/** Screen used to review and apply month-close actions. */
@Serializable
data object MonthCloseKey : SettingsChildKey

/**
 * Stable identifier for each top-level tab.
 *
 * This helper intentionally mirrors the current string-based shell semantics
 * without exposing those raw strings to the rest of the migration work. It gives
 * future UI code a simple way to compare tabs, save selected-tab state, or map a
 * key to analytics / logging labels without pattern-matching every time.
 */
val TopLevelKey.stableId: String
    get() = when (this) {
        DashboardKey -> "dashboard"
        EntriesKey -> "entries"
        AssetsKey -> "assets"
        SettingsKey -> "settings"
    }

