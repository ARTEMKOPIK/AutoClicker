package com.autoclicker.app.visual

import android.content.Context
import com.autoclicker.app.util.CrashHandler
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Хранилище визуальных скриптов.
 * Thread-safety: All operations are synchronized using a lock object.
 */
class VisualScriptStorage private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("visual_scripts", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val lock = Any()

    companion object {
        @Volatile
        private var instance: VisualScriptStorage? = null

        fun getInstance(context: Context): VisualScriptStorage {
            return instance ?: synchronized(this) {
                instance ?: VisualScriptStorage(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveScript(script: VisualScript) {
        synchronized(lock) {
            try {
                val scripts = getAllScriptsInternal().toMutableList()
                val existingIndex = scripts.indexOfFirst { it.id == script.id }

                if (existingIndex >= 0) {
                    scripts[existingIndex] = script
                } else {
                    scripts.add(script)
                }

                val json = gson.toJson(scripts)
                prefs.edit().putString("scripts_list", json).apply()
            } catch (e: Exception) {
                CrashHandler.logError("VisualScriptStorage", "Error saving script", e)
            }
        }
    }

    fun getScript(id: String): VisualScript? {
        synchronized(lock) {
            return getAllScriptsInternal().find { it.id == id }
        }
    }

    fun getAllScripts(): List<VisualScript> {
        synchronized(lock) {
            return getAllScriptsInternal()
        }
    }

    private fun getAllScriptsInternal(): List<VisualScript> {
        val json = prefs.getString("scripts_list", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<VisualScript>>() {}.type
            gson.fromJson<List<VisualScript>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            CrashHandler.logError("VisualScriptStorage", "Error loading scripts", e)
            emptyList()
        }
    }

    fun deleteScript(id: String) {
        synchronized(lock) {
            try {
                val scripts = getAllScriptsInternal().filter { it.id != id }
                val json = gson.toJson(scripts)
                prefs.edit().putString("scripts_list", json).apply()
            } catch (e: Exception) {
                CrashHandler.logError("VisualScriptStorage", "Error deleting script", e)
            }
        }
    }
}
