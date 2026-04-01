package com.davideagostini.summ.ui.settings.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SupportedAppTheme(
    val key: String,
    val nightMode: Int,
) {
    LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("dark", AppCompatDelegate.MODE_NIGHT_YES),
    SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
}

object AppThemeManager {
    private const val PREFS_NAME = "summ_theme_prefs"
    private const val PREF_THEME_KEY = "app_theme"

    private val _currentTheme = MutableStateFlow(SupportedAppTheme.SYSTEM)
    val currentTheme: StateFlow<SupportedAppTheme> = _currentTheme.asStateFlow()

    val supportedThemes = listOf(
        SupportedAppTheme.LIGHT,
        SupportedAppTheme.DARK,
        SupportedAppTheme.SYSTEM,
    )

    fun initialize(context: Context) {
        val theme = storedTheme(context)
        _currentTheme.value = theme
        applyTheme(theme)
    }

    fun storedTheme(context: Context): SupportedAppTheme {
        val savedKey = context
            .applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_THEME_KEY, SupportedAppTheme.SYSTEM.key)
            .orEmpty()

        return supportedThemes.firstOrNull { it.key == savedKey } ?: SupportedAppTheme.SYSTEM
    }

    fun setTheme(context: Context, theme: SupportedAppTheme) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_THEME_KEY, theme.key)
            .apply()

        _currentTheme.value = theme
        applyTheme(theme)
    }

    private fun applyTheme(theme: SupportedAppTheme) {
        AppCompatDelegate.setDefaultNightMode(theme.nightMode)
    }
}
