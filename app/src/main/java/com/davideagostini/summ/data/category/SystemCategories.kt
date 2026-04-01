package com.davideagostini.summ.data.category

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.StringRes
import com.davideagostini.summ.R
import com.davideagostini.summ.data.entity.Category
import com.davideagostini.summ.ui.settings.language.AppLanguageManager
import java.util.Locale

data class SystemCategoryDefinition(
    val key: String,
    @param:StringRes val labelResId: Int,
    val emoji: String,
)

object SystemCategories {
    const val FOOD = "food"
    const val TRANSPORT = "transport"
    const val HOME = "home"
    const val HEALTH = "health"
    const val LEISURE = "leisure"
    const val WORK = "work"
    const val OTHER = "other"

    private val supportedLocales = listOf(
        Locale.ENGLISH,
        Locale.ITALIAN,
        Locale.FRENCH,
        Locale.GERMAN,
        Locale.forLanguageTag("nl"),
        Locale.forLanguageTag("es"),
        Locale.forLanguageTag("pt-BR"),
        Locale.forLanguageTag("ru"),
        Locale.forLanguageTag("ar"),
        Locale.SIMPLIFIED_CHINESE,
        Locale.JAPANESE,
    )

    val definitions = listOf(
        SystemCategoryDefinition(FOOD, R.string.default_category_food, "🍕"),
        SystemCategoryDefinition(TRANSPORT, R.string.default_category_transport, "🚗"),
        SystemCategoryDefinition(HOME, R.string.default_category_home, "🏠"),
        SystemCategoryDefinition(HEALTH, R.string.default_category_health, "💊"),
        SystemCategoryDefinition(LEISURE, R.string.default_category_leisure, "🎉"),
        SystemCategoryDefinition(WORK, R.string.default_category_work, "💼"),
        SystemCategoryDefinition(OTHER, R.string.default_category_other, "📦"),
    )

    fun defaultCategories(context: Context): List<Category> =
        definitions.map { definition ->
            Category(
                id = definition.key,
                name = localizedName(context, definition.key),
                emoji = definition.emoji,
                systemKey = definition.key,
                usesDefaultTranslation = true,
            )
        }

    fun localizedName(context: Context, key: String): String {
        val definition = definitions.firstOrNull { it.key == key } ?: return key
        return AppLanguageManager.wrap(context).getString(definition.labelResId)
    }

    fun inferSystemKey(context: Context, name: String, emoji: String? = null): String? {
        val normalizedName = normalize(name)
        if (normalizedName.isEmpty()) return null

        return definitions.firstOrNull { definition ->
            (emoji.isNullOrBlank() || emoji == definition.emoji) &&
                knownLabels(context, definition).contains(normalizedName)
        }?.key
    }

    fun shouldUseDefaultTranslation(context: Context, key: String?, name: String): Boolean {
        if (key == null) return false
        return knownLabels(context, key).contains(normalize(name))
    }

    fun displayName(
        context: Context,
        storedName: String,
        systemKey: String?,
        usesDefaultTranslation: Boolean,
    ): String =
        if (usesDefaultTranslation && systemKey != null) {
            localizedName(context, systemKey)
        } else {
            storedName
        }

    private fun knownLabels(context: Context, key: String): Set<String> {
        val definition = definitions.firstOrNull { it.key == key } ?: return emptySet()
        return knownLabels(context, definition)
    }

    private fun knownLabels(context: Context, definition: SystemCategoryDefinition): Set<String> =
        buildSet {
            supportedLocales.forEach { locale ->
                add(normalize(localizedString(context, locale, definition.labelResId)))
            }
        }

    private fun localizedString(context: Context, locale: Locale, @StringRes resId: Int): String {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration).getString(resId)
    }

    private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT)
}
