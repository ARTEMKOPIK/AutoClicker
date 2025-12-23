package com.autoclicker.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Быстрые действия - сохранённые координаты для быстрого клика
 */
class QuickActions(context: Context) {

    private val prefs = context.getSharedPreferences("quick_actions", Context.MODE_PRIVATE)
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
                actions.add(action)
            }
            
            prefs.edit().putString("actions", gson.toJson(actions)).apply()
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
        val json = prefs.getString("actions", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<QuickAction>>() {}.type
            gson.fromJson<List<QuickAction>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteAction(id: String) {
        synchronized(lock) {
            val actions = getAllActionsInternal().filter { it.id != id }
            prefs.edit().putString("actions", gson.toJson(actions)).apply()
        }
    }

    fun clearAll() {
        synchronized(lock) {
            prefs.edit().remove("actions").apply()
        }
    }
}
