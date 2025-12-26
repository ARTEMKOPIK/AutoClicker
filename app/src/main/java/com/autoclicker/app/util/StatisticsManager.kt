package com.autoclicker.app.util

import android.content.Context
import android.content.SharedPreferences
import com.autoclicker.app.data.DailyStatistics
import com.autoclicker.app.data.ScriptExecutionRecord
import com.autoclicker.app.data.ScriptStatistics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

/**
 * Менеджер для сбора и хранения статистики выполнения скриптов
 */
class StatisticsManager private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "statistics_prefs"
        private const val KEY_SCRIPT_STATS = "script_statistics"
        private const val KEY_EXECUTION_HISTORY = "execution_history"
        private const val KEY_DAILY_STATS = "daily_statistics"
        private const val MAX_HISTORY_SIZE = 1000 // Максимум записей в истории
        
        @Volatile
        private var instance: StatisticsManager? = null
        
        fun getInstance(context: Context): StatisticsManager {
            return instance ?: synchronized(this) {
                instance ?: StatisticsManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    /**
     * Записать начало выполнения скрипта
     */
    fun recordScriptStart(scriptName: String): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * Записать окончание выполнения скрипта
     */
    fun recordScriptEnd(scriptName: String, startTime: Long, success: Boolean, errorMessage: String? = null) {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Создаём запись о выполнении
        val record = ScriptExecutionRecord(
            id = System.currentTimeMillis(),
            scriptName = scriptName,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            success = success,
            errorMessage = errorMessage
        )
        
        // Добавляем в историю
        addExecutionRecord(record)
        
        // Обновляем общую статистику скрипта
        updateScriptStatistics(scriptName, duration, success)
        
        // Обновляем дневную статистику
        updateDailyStatistics(duration, success)
    }
    
    /**
     * Получить статистику по конкретному скрипту
     */
    fun getScriptStatistics(scriptName: String): ScriptStatistics? {
        val allStats = getAllScriptStatistics()
        return allStats.find { it.scriptName == scriptName }
    }
    
    /**
     * Получить статистику по всем скриптам
     */
    fun getAllScriptStatistics(): List<ScriptStatistics> {
        val json = prefs.getString(KEY_SCRIPT_STATS, null) ?: return emptyList()
        val type = object : TypeToken<List<ScriptStatistics>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Получить топ N самых используемых скриптов
     */
    fun getTopScripts(limit: Int = 5): List<ScriptStatistics> {
        return getAllScriptStatistics()
            .sortedByDescending { it.totalRuns }
            .take(limit)
    }
    
    /**
     * Получить историю выполнений
     */
    fun getExecutionHistory(limit: Int = 100): List<ScriptExecutionRecord> {
        val json = prefs.getString(KEY_EXECUTION_HISTORY, null) ?: return emptyList()
        val type = object : TypeToken<List<ScriptExecutionRecord>>() {}.type
        return try {
            val history: List<ScriptExecutionRecord> = gson.fromJson(json, type) ?: emptyList()
            history.sortedByDescending { it.startTime }.take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Получить дневную статистику за последние N дней
     */
    fun getDailyStatistics(days: Int = 7): List<DailyStatistics> {
        val json = prefs.getString(KEY_DAILY_STATS, null) ?: return emptyList()
        val type = object : TypeToken<List<DailyStatistics>>() {}.type
        return try {
            val stats: List<DailyStatistics> = gson.fromJson(json, type) ?: emptyList()
            stats.sortedByDescending { it.date }.take(days)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Получить общую статистику за всё время
     */
    fun getTotalStatistics(): TotalStatistics {
        val allStats = getAllScriptStatistics()
        val totalRuns = allStats.sumOf { it.totalRuns }
        val totalDuration = allStats.sumOf { it.totalDuration }
        val successfulRuns = allStats.sumOf { it.successfulRuns }
        val failedRuns = allStats.sumOf { it.failedRuns }
        
        return TotalStatistics(
            totalScripts = allStats.size,
            totalRuns = totalRuns,
            totalDuration = totalDuration,
            successfulRuns = successfulRuns,
            failedRuns = failedRuns
        )
    }
    
    /**
     * Очистить всю статистику
     */
    fun clearAllStatistics() {
        prefs.edit()
            .remove(KEY_SCRIPT_STATS)
            .remove(KEY_EXECUTION_HISTORY)
            .remove(KEY_DAILY_STATS)
            .apply()
    }
    
    /**
     * Очистить статистику конкретного скрипта
     */
    fun clearScriptStatistics(scriptName: String) {
        val allStats = getAllScriptStatistics().toMutableList()
        allStats.removeAll { it.scriptName == scriptName }
        saveScriptStatistics(allStats)
        
        // Также удаляем из истории
        val history = getExecutionHistory(MAX_HISTORY_SIZE).toMutableList()
        history.removeAll { it.scriptName == scriptName }
        saveExecutionHistory(history)
    }
    
    // Private методы
    
    private fun updateScriptStatistics(scriptName: String, duration: Long, success: Boolean) {
        val allStats = getAllScriptStatistics().toMutableList()
        val existingStats = allStats.find { it.scriptName == scriptName }
        
        if (existingStats != null) {
            // Обновляем существующую статистику
            val updated = existingStats.copy(
                totalRuns = existingStats.totalRuns + 1,
                totalDuration = existingStats.totalDuration + duration,
                lastRunTime = System.currentTimeMillis(),
                successfulRuns = if (success) existingStats.successfulRuns + 1 else existingStats.successfulRuns,
                failedRuns = if (!success) existingStats.failedRuns + 1 else existingStats.failedRuns
            )
            allStats.removeAll { it.scriptName == scriptName }
            allStats.add(updated)
        } else {
            // Создаём новую статистику
            allStats.add(
                ScriptStatistics(
                    scriptName = scriptName,
                    totalRuns = 1,
                    totalDuration = duration,
                    lastRunTime = System.currentTimeMillis(),
                    successfulRuns = if (success) 1 else 0,
                    failedRuns = if (!success) 1 else 0
                )
            )
        }
        
        saveScriptStatistics(allStats)
    }
    
    private fun updateDailyStatistics(duration: Long, success: Boolean) {
        val today = getTodayTimestamp()
        val allDailyStats = getDailyStatistics(365).toMutableList()
        val todayStats = allDailyStats.find { it.date == today }
        
        if (todayStats != null) {
            // Обновляем сегодняшнюю статистику
            val updated = todayStats.copy(
                totalRuns = todayStats.totalRuns + 1,
                totalDuration = todayStats.totalDuration + duration,
                successfulRuns = if (success) todayStats.successfulRuns + 1 else todayStats.successfulRuns,
                failedRuns = if (!success) todayStats.failedRuns + 1 else todayStats.failedRuns
            )
            allDailyStats.removeAll { it.date == today }
            allDailyStats.add(updated)
        } else {
            // Создаём новую запись для сегодня
            allDailyStats.add(
                DailyStatistics(
                    date = today,
                    totalRuns = 1,
                    totalDuration = duration,
                    successfulRuns = if (success) 1 else 0,
                    failedRuns = if (!success) 1 else 0
                )
            )
        }
        
        // Сохраняем только последние 365 дней
        val limited = allDailyStats.sortedByDescending { it.date }.take(365)
        saveDailyStatistics(limited)
    }
    
    private fun addExecutionRecord(record: ScriptExecutionRecord) {
        val history = getExecutionHistory(MAX_HISTORY_SIZE).toMutableList()
        history.add(0, record)
        
        // Ограничиваем размер истории
        if (history.size > MAX_HISTORY_SIZE) {
            history.subList(MAX_HISTORY_SIZE, history.size).clear()
        }
        
        saveExecutionHistory(history)
    }
    
    private fun saveScriptStatistics(stats: List<ScriptStatistics>) {
        val json = gson.toJson(stats)
        prefs.edit().putString(KEY_SCRIPT_STATS, json).apply()
    }
    
    private fun saveExecutionHistory(history: List<ScriptExecutionRecord>) {
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_EXECUTION_HISTORY, json).apply()
    }
    
    private fun saveDailyStatistics(stats: List<DailyStatistics>) {
        val json = gson.toJson(stats)
        prefs.edit().putString(KEY_DAILY_STATS, json).apply()
    }
    
    private fun getTodayTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Общая статистика за всё время
     */
    data class TotalStatistics(
        val totalScripts: Int,
        val totalRuns: Int,
        val totalDuration: Long,
        val successfulRuns: Int,
        val failedRuns: Int
    ) {
        fun getSuccessRate(): Float {
            return if (totalRuns > 0) (successfulRuns.toFloat() / totalRuns.toFloat()) * 100f else 0f
        }
        
        fun getAverageDuration(): Long {
            return if (totalRuns > 0) totalDuration / totalRuns else 0
        }
    }
}

