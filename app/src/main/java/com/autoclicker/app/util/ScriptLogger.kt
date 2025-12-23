package com.autoclicker.app.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Система логирования для скриптов
 * Поддерживает уровни логов, сохранение в файл, и real-time обновления
 */
object ScriptLogger {

    enum class Level(val tag: String, val color: Int) {
        DEBUG("DEBUG", 0xFF9E9E9E.toInt()),
        INFO("INFO", 0xFF4CAF50.toInt()),
        WARN("WARN", 0xFFFF9800.toInt()),
        ERROR("ERROR", 0xFFF44336.toInt()),
        SUCCESS("OK", 0xFF00E676.toInt())
    }

    data class LogEntry(
        val timestamp: Long = System.currentTimeMillis(),
        val level: Level,
        val message: String,
        val scriptName: String? = null
    ) {
        fun format(): String {
            val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
            val script = scriptName?.let { "[$it] " } ?: ""
            return "[$time] [${level.tag}] $script$message"
        }
    }

    interface LogListener {
        fun onLog(entry: LogEntry)
        fun onClear()
    }

    private val logs = CopyOnWriteArrayList<LogEntry>()
    private val listeners = CopyOnWriteArrayList<LogListener>()
    private var logFile: File? = null
    private var currentScriptName: String? = null
    
    // CRITICAL: Слушатели должны быть удалены вручную в onDestroy() активностей/сервисов
    // Это предотвращает утечки памяти (memory leaks) в глобальном singleton
    
    // Настройки
    var maxLogEntries = 1000
    var minLevel = Level.DEBUG
    var saveToFile = false

    fun init(context: Context) {
        logFile = File(context.filesDir, "script_logs.txt")
    }

    fun setScriptName(name: String?) {
        currentScriptName = name
    }

    fun debug(message: String) = log(Level.DEBUG, message)
    fun info(message: String) = log(Level.INFO, message)
    fun warn(message: String) = log(Level.WARN, message)
    fun error(message: String) = log(Level.ERROR, message)
    fun success(message: String) = log(Level.SUCCESS, message)

    fun log(level: Level, message: String) {
        if (level.ordinal < minLevel.ordinal) return

        val entry = LogEntry(
            level = level,
            message = message,
            scriptName = currentScriptName
        )

        logs.add(entry)

        // Ограничиваем размер
        while (logs.size > maxLogEntries) {
            logs.removeAt(0)
        }

        // Уведомляем слушателей
        listeners.forEach { it.onLog(entry) }

        // Сохраняем в файл
        if (saveToFile) {
            appendToFile(entry)
        }
    }

    fun getLogs(): List<LogEntry> = logs.toList()

    fun getLogsFiltered(level: Level? = null, scriptName: String? = null): List<LogEntry> {
        return logs.filter { entry ->
            (level == null || entry.level == level) &&
            (scriptName == null || entry.scriptName == scriptName)
        }
    }

    fun clear() {
        logs.clear()
        listeners.forEach { it.onClear() }
    }

    fun addListener(listener: LogListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: LogListener) {
        listeners.remove(listener)
    }
    
    // Remove all listeners - useful in onDestroy() to prevent memory leaks
    fun removeAllListeners() {
        listeners.clear()
    }
    
    // Clear all logs and notify listeners
    fun clearAll() {
        logs.clear()
        listeners.forEach { it.onClear() }
    }

    private fun appendToFile(entry: LogEntry) {
        val file = logFile ?: return
        try {
            file.appendText(entry.format() + "\n")
        } catch (e: Exception) {
            // Ignore file errors
        }
    }

    fun exportLogs(): String {
        return logs.joinToString("\n") { it.format() }
    }

    fun getLogFile(): File? = logFile
}
