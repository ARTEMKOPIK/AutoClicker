package com.autoclicker.app.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Утилита для работы с SharedPreferences
 * Упрощает сохранение и загрузку настроек
 */
object PreferencesHelper {
    
    private const val PREFS_NAME = "autoclicker_prefs"
    private lateinit var prefs: SharedPreferences
    internal val gson = Gson()
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Сохранить строку
     */
    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    
    /**
     * Получить строку
     */
    fun getString(key: String, defaultValue: String = ""): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }
    
    /**
     * Сохранить число
     */
    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }
    
    /**
     * Получить число
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }
    
    /**
     * Сохранить Long число
     */
    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }
    
    /**
     * Получить Long число
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }
    
    /**
     * Сохранить Float число
     */
    fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }
    
    /**
     * Получить Float число
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return prefs.getFloat(key, defaultValue)
    }
    
    /**
     * Сохранить Boolean
     */
    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }
    
    /**
     * Получить Boolean
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }
    
    /**
     * Сохранить объект (сериализация в JSON)
     */
    fun <T> putObject(key: String, value: T) {
        val json = gson.toJson(value)
        putString(key, json)
    }
    
    /**
     * Получить объект (десериализация из JSON)
     */
    inline fun <reified T> getObject(key: String, defaultValue: T? = null): T? {
        val json = getString(key, "")
        return if (json.isEmpty()) {
            defaultValue
        } else {
            try {
                gson.fromJson(json, T::class.java)
            } catch (e: Exception) {
                defaultValue
            }
        }
    }
    
    /**
     * Сохранить список (сериализация в JSON)
     */
    fun <T> putList(key: String, list: List<T>) {
        val json = gson.toJson(list)
        putString(key, json)
    }
    
    /**
     * Получить список (десериализация из JSON)
     */
    inline fun <reified T> getList(key: String): List<T> {
        val json = getString(key, "")
        return if (json.isEmpty()) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<T>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Сохранить Set строк
     */
    fun putStringSet(key: String, values: Set<String>) {
        prefs.edit().putStringSet(key, values).apply()
    }
    
    /**
     * Получить Set строк
     */
    fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String> {
        return prefs.getStringSet(key, defaultValue) ?: defaultValue
    }
    
    /**
     * Проверить существование ключа
     */
    fun contains(key: String): Boolean {
        return prefs.contains(key)
    }
    
    /**
     * Удалить значение по ключу
     */
    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
    
    /**
     * Очистить все настройки
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Получить все ключи
     */
    fun getAllKeys(): Set<String> {
        return prefs.all.keys
    }
    
    /**
     * Получить количество сохраненных значений
     */
    fun getCount(): Int {
        return prefs.all.size
    }
    
    /**
     * Экспортировать все настройки в JSON
     */
    fun exportToJson(): String {
        return gson.toJson(prefs.all)
    }
    
    /**
     * Импортировать настройки из JSON
     */
    fun importFromJson(json: String): Boolean {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(json, type)
            
            val editor = prefs.edit()
            map.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Double -> editor.putFloat(key, value.toFloat())
                }
            }
            editor.apply()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Класс Builder для группового сохранения
     */
    class Editor {
        private val editor: SharedPreferences.Editor = prefs.edit()
        
        fun putString(key: String, value: String) = apply { editor.putString(key, value) }
        fun putInt(key: String, value: Int) = apply { editor.putInt(key, value) }
        fun putLong(key: String, value: Long) = apply { editor.putLong(key, value) }
        fun putFloat(key: String, value: Float) = apply { editor.putFloat(key, value) }
        fun putBoolean(key: String, value: Boolean) = apply { editor.putBoolean(key, value) }
        fun remove(key: String) = apply { editor.remove(key) }
        
        fun apply() {
            editor.apply()
        }
        
        fun commit(): Boolean {
            return editor.commit()
        }
    }
    
    /**
     * Создать Editor для группового сохранения
     */
    fun edit(): Editor {
        return Editor()
    }
}
