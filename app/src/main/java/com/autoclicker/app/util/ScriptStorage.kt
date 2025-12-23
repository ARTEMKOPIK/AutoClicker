package com.autoclicker.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class ScriptStorage(context: Context) {

    private val prefs = context.getSharedPreferences("scripts", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val lock = Any()
    
    companion object {
        private const val CURRENT_VERSION = 1
        private const val KEY_SCRIPTS_LIST = "scripts_list"
        private const val KEY_DATA_VERSION = "data_version"
    }

    data class Script(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val code: String,
        val date: String = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date()),
        val version: Int = CURRENT_VERSION
    )
    
    init {
        migrateDataIfNeeded()
    }
    
    /**
     * Migrate data to current version if needed.
     */
    private fun migrateDataIfNeeded() {
        val storedVersion = prefs.getInt(KEY_DATA_VERSION, 0)
        if (storedVersion < CURRENT_VERSION) {
            synchronized(lock) {
                try {
                    // Для будущих миграций добавлять логику здесь
                    // when (storedVersion) {
                    //     0 -> migrateV0toV1()
                    // }
                    prefs.edit().putInt(KEY_DATA_VERSION, CURRENT_VERSION).apply()
                    Log.i("ScriptStorage", "Data migrated from v$storedVersion to v$CURRENT_VERSION")
                } catch (e: Exception) {
                    Log.e("ScriptStorage", "Migration failed", e)
                    CrashHandler.logError("ScriptStorage", "Ошибка миграции данных", e)
                }
            }
        }
    }

    fun saveScript(script: Script) {
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
                android.util.Log.e("ScriptStorage", "Error saving script", e)
                CrashHandler.logError("ScriptStorage", "Ошибка сохранения скрипта '${script.name}'", e)
            }
        }
    }

    fun getScript(id: String): Script? {
        synchronized(lock) {
            return getAllScriptsInternal().find { it.id == id }
        }
    }

    fun getAllScripts(): List<Script> {
        synchronized(lock) {
            return getAllScriptsInternal()
        }
    }
    
    /**
     * Get all scripts from storage (internal method).
     * 
     * Thread-safety: Must be called from within synchronized block.
     * Error handling: Logs corruption with context, returns empty list on parse error.
     * Provides backup mechanism for data recovery.
     * 
     * @return List of all scripts, empty list if corrupted or not found
     */
    private fun getAllScriptsInternal(): List<Script> {
        val json = prefs.getString("scripts_list", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Script>>() {}.type
            gson.fromJson<List<Script>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            // Log corrupted data with comprehensive context
            val errorMessage = """
                ScriptStorage data corruption detected!
                
                JSON Length: ${json.length}
                JSON Preview: ${json.take(200)}
                First 500 chars (escaped): ${json.take(500).replace("\n", "\\n")}
                Error: ${e.javaClass.simpleName}: ${e.message}
                Stack Trace: ${e.stackTrace.take(5).joinToString("\n")}
            """.trimIndent()
            
            android.util.Log.e("ScriptStorage", errorMessage, e)
            CrashHandler.logError("ScriptStorage", errorMessage, e)
            
            // Try to create a backup of corrupted data for manual recovery
            try {
                val backupKey = "scripts_list_backup_${System.currentTimeMillis()}"
                prefs.edit().putString(backupKey, json).apply()
                Log.i("ScriptStorage", "Created backup at key: $backupKey")
            } catch (backupEx: Exception) {
                Log.e("ScriptStorage", "Failed to create backup", backupEx)
            }
            
            // Return empty list to prevent app crash
            // User data is lost, but app remains functional
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
                android.util.Log.e("ScriptStorage", "Error deleting script", e)
                CrashHandler.logError("ScriptStorage", "Ошибка удаления скрипта", e)
            }
        }
    }
}
