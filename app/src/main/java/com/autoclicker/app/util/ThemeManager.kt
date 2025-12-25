package com.autoclicker.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * Менеджер тем приложения
 * 
 * Управляет выбором и применением темы (светлая/тёмная/системная).
 * Использует SharedPreferences для хранения выбора пользователя.
 * 
 * @example
 * ```kotlin
 * // Установить тему
 * ThemeManager.setThemeMode(context, ThemeMode.LIGHT)
 * 
 * // Применить сохранённую тему
 * ThemeManager.applyTheme(context)
 * 
 * // Получить текущую тему
 * val currentTheme = ThemeManager.getThemeMode(context)
 * ```
 */
object ThemeManager {
    
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    
    /**
     * Режимы темы приложения
     */
    enum class ThemeMode {
        /** Светлая тема */
        LIGHT,
        
        /** Тёмная тема */
        DARK,
        
        /** Следовать системной теме */
        SYSTEM;
        
        companion object {
            /**
             * Получить ThemeMode из строки
             * @param value Строковое представление (LIGHT/DARK/SYSTEM)
             * @return ThemeMode или SYSTEM по умолчанию
             */
            fun fromString(value: String?): ThemeMode {
                return try {
                    valueOf(value ?: "SYSTEM")
                } catch (e: IllegalArgumentException) {
                    CrashHandler.logWarning("ThemeManager", "Invalid theme mode: $value, using SYSTEM")
                    SYSTEM
                }
            }
        }
    }
    
    /**
     * Получить SharedPreferences для темы
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Получить текущий режим темы
     * 
     * @param context Контекст приложения
     * @return Текущий ThemeMode (по умолчанию SYSTEM)
     */
    fun getThemeMode(context: Context): ThemeMode {
        val prefs = getPrefs(context)
        val modeString = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return ThemeMode.fromString(modeString)
    }
    
    /**
     * Установить режим темы
     * 
     * Сохраняет выбор в SharedPreferences и применяет тему немедленно.
     * 
     * @param context Контекст приложения
     * @param mode Режим темы для применения
     */
    fun setThemeMode(context: Context, mode: ThemeMode) {
        try {
            val prefs = getPrefs(context)
            prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
            applyTheme(context)
            
            CrashHandler.logInfo("ThemeManager", "Theme mode changed to: $mode")
        } catch (e: Exception) {
            CrashHandler.logError("ThemeManager", "Failed to set theme mode: $mode", e)
        }
    }
    
    /**
     * Применить сохранённую тему
     * 
     * Читает сохранённый выбор темы и применяет через AppCompatDelegate.
     * Вызывается автоматически при запуске приложения.
     * 
     * @param context Контекст приложения
     */
    fun applyTheme(context: Context) {
        try {
            val mode = getThemeMode(context)
            val nightMode = when (mode) {
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            
            AppCompatDelegate.setDefaultNightMode(nightMode)
        } catch (e: Exception) {
            CrashHandler.logError("ThemeManager", "Failed to apply theme", e)
            // Fallback to system default
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    
    /**
     * Получить локализованное название темы
     * 
     * @param mode Режим темы
     * @return Строка с названием на русском
     */
    fun getThemeName(mode: ThemeMode): String {
        return when (mode) {
            ThemeMode.LIGHT -> "Светлая"
            ThemeMode.DARK -> "Тёмная"
            ThemeMode.SYSTEM -> "Системная"
        }
    }
    
    /**
     * Получить все доступные режимы тем с названиями
     * 
     * @return Список пар (режим, название)
     */
    fun getAllThemeModes(): List<Pair<ThemeMode, String>> {
        return listOf(
            ThemeMode.LIGHT to getThemeName(ThemeMode.LIGHT),
            ThemeMode.DARK to getThemeName(ThemeMode.DARK),
            ThemeMode.SYSTEM to getThemeName(ThemeMode.SYSTEM)
        )
    }
}

