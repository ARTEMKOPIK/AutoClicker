package com.autoclicker.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import com.autoclicker.app.util.Constants.PREFS_KEY_ACTIONS
import com.autoclicker.app.util.Constants.PREFS_KEY_QUICK_ACTIONS_BACKUP
import com.autoclicker.app.util.Constants.QUICK_ACTIONS_KEY

/**
 * Быстрые действия - сохранённые координаты для быстрого клика
 */
class QuickActions(context: Context) {

    private val prefs = context.getSharedPreferences(QUICK_ACTIONS_KEY, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val lock = Any()

    data class QuickAction(
        val id: String = java.util.UUID.randomUUID().toString(),
        val name: String,
        val x: Int,
        val y: Int,
        val type: ActionType = ActionType.CLICK,
        val color: Int = 0xFFFF5722.toInt()
    )

    enum class ActionType {
        CLICK,
        LONG_CLICK,
        DOUBLE_TAP
    }

    fun saveAction(action: QuickAction) {
        synchronized(lock) {
            val actions = getAllActionsInternal().toMutableList()
            val existingIndex = actions.indexOfFirst { it.id == action.id }
            
            if (existingIndex >= 0) {
                actions[existingIndex] = action
            } else {
                if (actions.size < Constants.MAX_QUICK_ACTIONS) {
                    actions.add(action)
                } else {
                    CrashHandler.logWarning("QuickActions", "Max quick actions reached (${Constants.MAX_QUICK_ACTIONS})")
                }
            }
            
            val json = gson.toJson(actions)
            prefs.edit().putString(PREFS_KEY_ACTIONS, json).apply()
            // Делаем бэкап при каждом сохранении
            prefs.edit().putString(PREFS_KEY_QUICK_ACTIONS_BACKUP, json).apply()
        }
    }

    fun getAction(id: String): QuickAction? {
        synchronized(lock) {
            return getAllActionsInternal().find { it.id == id }
        }
    }

    fun getAllActions(): List<QuickAction> {
        synchronized(lock) {
            return getAllActionsInternal()
        }
    }
    
    private fun getAllActionsInternal(): List<QuickAction> {
        val json = prefs.getString(PREFS_KEY_ACTIONS, null)
        if (json == null) {
            // Пытаемся восстановить из бэкапа
            val backupJson = prefs.getString(PREFS_KEY_QUICK_ACTIONS_BACKUP, null)
            if (backupJson != null) {
                CrashHandler.logWarning("QuickActions", "Restoring from backup")
                return parseJson(backupJson)
            }
            return emptyList()
        }

        return try {
            parseJson(json)
        } catch (e: Exception) {
            CrashHandler.logError("QuickActions", "Failed to parse actions JSON, trying backup", e)
            // Пытаемся восстановить из бэкапа
            val backupJson = prefs.getString(PREFS_KEY_QUICK_ACTIONS_BACKUP, null)
            if (backupJson != null) {
                try {
                    return parseJson(backupJson)
                } catch (be: Exception) {
                    CrashHandler.logError("QuickActions", "Backup also corrupted", be)
                }
            }
            emptyList()
        }
    }

    private fun parseJson(json: String): List<QuickAction> {
        val type = object : TypeToken<List<QuickAction>>() {}.type
        return gson.fromJson<List<QuickAction>>(json, type) ?: emptyList()
    }

    fun deleteAction(id: String) {
        synchronized(lock) {
            val actions = getAllActionsInternal().filter { it.id != id }
            val json = gson.toJson(actions)
            prefs.edit().putString(PREFS_KEY_ACTIONS, json).apply()
            prefs.edit().putString(PREFS_KEY_QUICK_ACTIONS_BACKUP, json).apply()
        }
    }

    fun clearAll() {
        synchronized(lock) {
            prefs.edit().remove(PREFS_KEY_ACTIONS).apply()
            prefs.edit().remove(PREFS_KEY_QUICK_ACTIONS_BACKUP).apply()
        }
    }
}
