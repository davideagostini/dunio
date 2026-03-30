package com.davideagostini.summ.ui.settings.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

data class SupportedAppLanguage(val tag: String)

object AppLanguageManager {
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

    fun currentLanguage(): SupportedAppLanguage {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val currentTag = when {
            !appLocales.isEmpty -> appLocales[0]?.toLanguageTag().orEmpty()
            else -> Locale.getDefault().toLanguageTag()
        }

        return supportedLanguages.firstOrNull { matches(it.tag, currentTag) }
            ?: supportedLanguages.first()
    }

    fun setLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
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
}
