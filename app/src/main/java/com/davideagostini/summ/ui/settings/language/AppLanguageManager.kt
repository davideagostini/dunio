package com.davideagostini.summ.ui.settings.language

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class SupportedAppLanguage(val tag: String)

object AppLanguageManager {
    private const val PREFS_NAME = "summ_language_prefs"
    private const val PREF_LANGUAGE_KEY = "app_language"

    private val _currentLanguage = MutableStateFlow(SupportedAppLanguage("en"))
    val currentLanguage: StateFlow<SupportedAppLanguage> = _currentLanguage.asStateFlow()

    val supportedLanguages = listOf(
        SupportedAppLanguage("ar"),
        SupportedAppLanguage("de"),
        SupportedAppLanguage("en"),
        SupportedAppLanguage("es"),
        SupportedAppLanguage("fr"),
        SupportedAppLanguage("it"),
        SupportedAppLanguage("ja"),
        SupportedAppLanguage("nl"),
        SupportedAppLanguage("pt-BR"),
        SupportedAppLanguage("ru"),
        SupportedAppLanguage("zh-CN"),
    )

    fun initialize(context: Context) {
        val language = storedLanguage(context)
        _currentLanguage.value = language
    }

    fun storedLanguage(context: Context): SupportedAppLanguage {
        val savedTag = context
            .applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREF_LANGUAGE_KEY, null)

        val currentTag = savedTag ?: run {
            val appLocales = AppCompatDelegate.getApplicationLocales()
            when {
                !appLocales.isEmpty -> appLocales[0]?.toLanguageTag().orEmpty()
                else -> Locale.getDefault().toLanguageTag()
            }
        }

        return supportedLanguages.firstOrNull { matches(it.tag, currentTag) }
            ?: supportedLanguages.first()
    }

    fun setLanguage(context: Context, tag: String) {
        val selectedLanguage = supportedLanguages.firstOrNull { it.tag == tag } ?: return
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_LANGUAGE_KEY, selectedLanguage.tag)
            .apply()

        _currentLanguage.value = selectedLanguage
    }

    fun displayName(tag: String, displayLocale: Locale = Locale.getDefault()): String =
        Locale.forLanguageTag(tag).getDisplayName(displayLocale).replaceFirstCharIfNeeded(displayLocale)

    fun nativeDisplayName(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayName(locale).replaceFirstCharIfNeeded(locale)
    }

    private fun matches(supportedTag: String, currentTag: String): Boolean {
        if (supportedTag.equals(currentTag, ignoreCase = true)) return true

        val supportedLocale = Locale.forLanguageTag(supportedTag)
        val currentLocale = Locale.forLanguageTag(currentTag)
        return supportedLocale.language.equals(currentLocale.language, ignoreCase = true)
    }

    private fun String.replaceFirstCharIfNeeded(locale: Locale): String =
        replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
        }

    fun wrap(context: Context): Context {
        val language = storedLanguage(context)
        val locale = Locale.forLanguageTag(language.tag)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }
}
