package com.autoclicker.app.data

/**
 * Модель статистики выполнения скрипта
 */
data class ScriptStatistics(
    val scriptName: String,
    val totalRuns: Int = 0,
    val totalDuration: Long = 0, // в миллисекундах
    val lastRunTime: Long = 0, // timestamp
    val successfulRuns: Int = 0,
    val failedRuns: Int = 0,
    val averageDuration: Long = 0
) {
    /**
     * Получить среднюю длительность выполнения
     */
    fun getAverageDurationMs(): Long {
        return if (totalRuns > 0) totalDuration / totalRuns else 0
    }
    
    /**
     * Получить процент успешных запусков
     */
    fun getSuccessRate(): Float {
        return if (totalRuns > 0) (successfulRuns.toFloat() / totalRuns.toFloat()) * 100f else 0f
    }
    
    /**
     * Форматированная длительность
     */
    fun getFormattedDuration(): String {
        val avgMs = getAverageDurationMs()
        return when {
            avgMs < 1000 -> "${avgMs}ms"
            avgMs < 60000 -> String.format("%.1fs", avgMs / 1000f)
            else -> String.format("%.1fm", avgMs / 60000f)
        }
    }
}

/**
 * Запись о запуске скрипта
 */
data class ScriptExecutionRecord(
    val id: Long = 0,
    val scriptName: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * Дневная статистика
 */
data class DailyStatistics(
    val date: Long, // timestamp начала дня
    val totalRuns: Int,
    val totalDuration: Long,
    val successfulRuns: Int,
    val failedRuns: Int
)
