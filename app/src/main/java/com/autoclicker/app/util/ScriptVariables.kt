package com.autoclicker.app.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Глобальные переменные для скриптов
 * Сохраняются между запусками
 */
object ScriptVariables {

    private lateinit var prefs: android.content.SharedPreferences
    private val gson = Gson()
    private val lock = Any()

    // In-memory cache
    private val cache = mutableMapOf<String, Any>()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("script_variables", Context.MODE_PRIVATE)
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        if (!::prefs.isInitialized) return
        synchronized(lock) {
            val json = prefs.getString("variables", null) ?: return
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val map: Map<String, String> = gson.fromJson(json, type) ?: return
                cache.clear()
                cache.putAll(map)
            } catch (e: Exception) {
                android.util.Log.e("ScriptVariables", "Error loading variables", e)
            }
        }
    }

    private fun saveToPrefs() {
        if (!::prefs.isInitialized) return
        synchronized(lock) {
            try {
                val stringMap = cache.mapValues { it.value.toString() }
                prefs.edit().putString("variables", gson.toJson(stringMap)).apply()
            } catch (e: Exception) {
                android.util.Log.e("ScriptVariables", "Error saving variables", e)
            }
        }
    }

    fun set(key: String, value: Any) {
        synchronized(lock) {
            cache[key] = value
            if (::prefs.isInitialized) {
                saveToPrefs()
            }
        }
    }

    fun get(key: String): Any? {
        synchronized(lock) {
            return cache[key]
        }
    }

    fun getString(key: String, default: String = ""): String {
        return get(key)?.toString() ?: default
    }

    fun getInt(key: String, default: Int = 0): Int {
        return get(key)?.toString()?.toIntOrNull() ?: default
    }

    fun getFloat(key: String, default: Float = 0f): Float {
        return get(key)?.toString()?.toFloatOrNull() ?: default
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        val value = get(key) ?: return default
        return when (value) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true)
            is Number -> value.toInt() != 0
            else -> default
        }
    }

    fun remove(key: String) {
        synchronized(lock) {
            cache.remove(key)
            if (::prefs.isInitialized) {
                saveToPrefs()
            }
        }
    }

    fun clear() {
        synchronized(lock) {
            cache.clear()
            if (::prefs.isInitialized) {
                saveToPrefs()
            }
        }
    }

    fun getAll(): Map<String, Any> {
        synchronized(lock) {
            return cache.toMap()
        }
    }

    fun increment(key: String, amount: Int = 1): Int {
        synchronized(lock) {
            val current = getInt(key, 0)
            val newValue = current + amount
            set(key, newValue)
            return newValue
        }
    }

    fun decrement(key: String, amount: Int = 1): Int {
        return increment(key, -amount)
    }
}
