package com.autoclicker.app.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Менеджер для управления локализацией приложения
 * 
 * Поддерживаемые языки:
 * - Системный (по умолчанию)
 * - Русский (ru)
 * - Английский (en)
 */
object LocaleManager {
    
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    
    const val LANGUAGE_SYSTEM = "system"
    const val LANGUAGE_RUSSIAN = "ru"
    const val LANGUAGE_ENGLISH = "en"
    
    /**
     * Получить текущий выбранный язык
     */
    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
    }
    
    /**
     * Установить язык приложения
     */
    fun setLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }
    
    /**
     * Применить локализацию к контексту
     * Нужно вызывать в attachBaseContext() каждой Activity
     */
    fun applyLocale(context: Context): Context {
        val language = getLanguage(context)
        
        // Если выбран системный язык, возвращаем исходный контекст
        if (language == LANGUAGE_SYSTEM) {
            return context
        }
        
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * Получить список доступных языков
     */
    fun getAvailableLanguages(): List<Language> {
        return listOf(
            Language(LANGUAGE_SYSTEM, "System default", "Системный"),
            Language(LANGUAGE_RUSSIAN, "Русский", "Russian"),
            Language(LANGUAGE_ENGLISH, "English", "Английский")
        )
    }
    
    /**
     * Получить отображаемое имя языка
     */
    fun getLanguageDisplayName(context: Context, languageCode: String): String {
        return when (languageCode) {
            LANGUAGE_SYSTEM -> context.getString(R.string.settings_language_system)
            LANGUAGE_RUSSIAN -> context.getString(R.string.settings_language_russian)
            LANGUAGE_ENGLISH -> context.getString(R.string.settings_language_english)
            else -> languageCode
        }
    }
    
    /**
     * Класс для представления языка
     */
    data class Language(
        val code: String,
        val nativeName: String,
        val englishName: String
    )
}

