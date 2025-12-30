package com.autoclicker.app.util

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Монитор производительности для отслеживания времени выполнения операций
 * Помогает находить узкие места в приложении
 */
object PerformanceMonitor {
    private const val TAG = "PerformanceMonitor"
    private val measurements = ConcurrentHashMap<String, MutableList<Long>>()
    private val activeTimers = ConcurrentHashMap<String, Long>()
    
    private var isEnabled = true
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    /**
     * Начать измерение времени выполнения операции
     */
    fun startMeasure(operationName: String) {
        if (!isEnabled) return
        activeTimers[operationName] = SystemClock.elapsedRealtime()
    }
    
    /**
     * Закончить измерение и сохранить результат
     */
    fun endMeasure(operationName: String) {
        if (!isEnabled) return
        
        val startTime = activeTimers.remove(operationName) ?: return
        val duration = SystemClock.elapsedRealtime() - startTime
        
        measurements.getOrPut(operationName) { mutableListOf() }.add(duration)
        
        // Логируем если операция заняла больше 100мс
        if (duration > 100) {
            Log.w(TAG, "⚠️ Медленная операция '$operationName': ${duration}мс")
        }
    }
    
    /**
     * Выполнить блок кода с измерением времени
     */
    inline fun <T> measure(operationName: String, block: () -> T): T {
        startMeasure(operationName)
        try {
            return block()
        } finally {
            endMeasure(operationName)
        }
    }
    
    /**
     * Получить статистику по операции
     */
    fun getStats(operationName: String): OperationStats? {
        val durations = measurements[operationName] ?: return null
        if (durations.isEmpty()) return null
        
        return OperationStats(
            name = operationName,
            count = durations.size,
            totalTime = durations.sum(),
            avgTime = durations.average(),
            minTime = durations.minOrNull() ?: 0,
            maxTime = durations.maxOrNull() ?: 0
        )
    }
    
    /**
     * Получить статистику по всем операциям
     */
    fun getAllStats(): List<OperationStats> {
        return measurements.keys.mapNotNull { getStats(it) }
            .sortedByDescending { it.totalTime }
    }
    
    /**
     * Очистить все измерения
     */
    fun clear() {
        measurements.clear()
        activeTimers.clear()
    }
    
    /**
     * Вывести отчет в лог
     */
    fun printReport() {
        if (measurements.isEmpty()) {
            Log.i(TAG, "📊 Нет данных о производительности")
            return
        }
        
        Log.i(TAG, "📊 ===== ОТЧЕТ О ПРОИЗВОДИТЕЛЬНОСТИ =====")
        getAllStats().forEach { stats ->
            Log.i(TAG, "📈 ${stats.name}:")
            Log.i(TAG, "   Вызовов: ${stats.count}")
            Log.i(TAG, "   Среднее время: ${stats.avgTime.toInt()}мс")
            Log.i(TAG, "   Мин/Макс: ${stats.minTime}/${stats.maxTime}мс")
            Log.i(TAG, "   Общее время: ${stats.totalTime}мс")
        }
        Log.i(TAG, "========================================")
    }
    
    data class OperationStats(
        val name: String,
        val count: Int,
        val totalTime: Long,
        val avgTime: Double,
        val minTime: Long,
        val maxTime: Long
    )
}

