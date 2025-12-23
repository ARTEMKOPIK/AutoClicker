package com.autoclicker.app.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Preference manager for AutoClicker application settings.
 * 
 * Provides thread-safe access to SharedPreferences with both individual
 * and batch operations. All write operations use apply() for async commits
 * except batch operations which support both apply() and commit().
 * 
 * Thread-safety: All operations are thread-safe. For batch operations,
 * use synchronized blocks or the provided batch edit methods.
 */
class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("autoclicker_prefs", Context.MODE_PRIVATE)

    // Telegram
    var telegramToken: String
        get() = prefs.getString("telegram_token", "") ?: ""
        set(value) = prefs.edit().putString("telegram_token", value).apply()

    var telegramChatId: String
        get() = prefs.getString("telegram_chat_id", "") ?: ""
        set(value) = prefs.edit().putString("telegram_chat_id", value).apply()

    // Скрипты
    var lastScriptId: String
        get() = prefs.getString("last_script_id", "") ?: ""
        set(value) = prefs.edit().putString("last_script_id", value).apply()

    // Позиция панели
    var panelX: Int
        get() = prefs.getInt("panel_x", Constants.DEFAULT_FLOATING_WINDOW_X)
        set(value) = prefs.edit().putInt("panel_x", value).apply()

    var panelY: Int
        get() = prefs.getInt("panel_y", Constants.DEFAULT_FLOATING_WINDOW_Y)
        set(value) = prefs.edit().putInt("panel_y", value).apply()

    // Мини-режим панели
    var panelMiniMode: Boolean
        get() = prefs.getBoolean("panel_mini_mode", false)
        set(value) = prefs.edit().putBoolean("panel_mini_mode", value).apply()

    // Позиция пипетки
    var pickerX: Int
        get() = prefs.getInt("picker_x", Constants.DEFAULT_PICKER_X)
        set(value) = prefs.edit().putInt("picker_x", value).apply()

    var pickerY: Int
        get() = prefs.getInt("picker_y", Constants.DEFAULT_PICKER_Y)
        set(value) = prefs.edit().putInt("picker_y", value).apply()

    // Тема
    var darkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()

    var accentColor: Int
        get() = prefs.getInt("accent_color", Constants.DEFAULT_ACCENT_COLOR)
        set(value) = prefs.edit().putInt("accent_color", value).apply()

    // Логирование
    var saveLogsToFile: Boolean
        get() = prefs.getBoolean("save_logs_to_file", false)
        set(value) = prefs.edit().putBoolean("save_logs_to_file", value).apply()

    // Автоперезапуск захвата
    var autoRestartCapture: Boolean
        get() = prefs.getBoolean("auto_restart_capture", true)
        set(value) = prefs.edit().putBoolean("auto_restart_capture", value).apply()

    /**
     * Perform batch edit operations atomically to prevent race conditions.
     * 
     * @param block Lambda with SharedPreferences.Editor operations
     * @param useCommit If true, uses commit() for synchronous execution, otherwise apply()
     * @return true if commit() succeeded, false if useCommit==false
     */
    @Synchronized
    fun batchEdit(useCommit: Boolean = false, block: SharedPreferences.Editor.() -> Unit): Boolean {
        val editor = prefs.edit()
        editor.block()
        return if (useCommit) {
            editor.commit()
        } else {
            editor.apply()
            false
        }
    }

    /**
     * Reset all preferences to defaults.
     * 
     * @param useCommit If true, uses commit() for synchronous execution
     * @return true if commit() succeeded, false if useCommit==false
     */
    @Synchronized
    fun clearAll(useCommit: Boolean = false): Boolean {
        return batchEdit(useCommit) {
            clear()
        }
    }
}
