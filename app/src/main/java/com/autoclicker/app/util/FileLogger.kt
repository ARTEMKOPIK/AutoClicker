package com.autoclicker.app.util

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Логирование в файл для отладки
 */
class FileLogger(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val lock = Any()

    private val logDir: File?
        get() = context.filesDir?.let { File(it, "logs").also { dir -> dir.mkdirs() } }

    private val currentLogFile: File?
        get() = logDir?.let { File(it, "log_${fileDateFormat.format(Date())}.txt") }

    fun log(tag: String, message: String) {
        synchronized(lock) {
            val logFile = currentLogFile ?: return
            try {
                // Проверка размера файла перед записью
                if (logFile.exists() && logFile.length() > Constants.MAX_LOG_FILE_SIZE) {
                    rotateLogFile(logFile)
                }

                val timestamp = dateFormat.format(Date())
                val logLine = "[$timestamp] [$tag] $message\n"

                FileWriter(logFile, true).use { writer ->
                    writer.write(logLine)
                }

                // Очищаем старые логи (старше 7 дней)
                cleanOldLogs()
            } catch (e: Exception) {
                android.util.Log.e("FileLogger", "Error writing log", e)
            }
        }
    }

    private fun rotateLogFile(file: File) {
        try {
            val backupFile = File(file.absolutePath + ".old")
            if (backupFile.exists()) backupFile.delete()
            file.renameTo(backupFile)
        } catch (e: Exception) {
            android.util.Log.e("FileLogger", "Error rotating log file", e)
        }
    }

    fun getLogContent(): String {
        return try {
            val logFile = currentLogFile
            if (logFile != null && logFile.exists()) {
                logFile.readText()
            } else {
                "Логи пусты"
            }
        } catch (e: Exception) {
            "Ошибка чтения логов: ${e.message}"
        }
    }

    fun getAllLogs(): String {
        val builder = StringBuilder()
        try {
            logDir?.listFiles()
                ?.sortedByDescending { it.name }
                ?.take(3) // Последние 3 дня
                ?.forEach { file ->
                    builder.append("=== ${file.name} ===\n")
                    builder.append(file.readText())
                    builder.append("\n\n")
                }
        } catch (e: Exception) {
            builder.append("Ошибка: ${e.message}")
        }
        return builder.toString()
    }

    fun clearLogs() {
        synchronized(lock) {
            try {
                logDir?.listFiles()?.forEach { it.delete() }
            } catch (e: Exception) {
                android.util.Log.e("FileLogger", "Error clearing logs", e)
            }
        }
    }

    private fun cleanOldLogs() {
        try {
            val cutoffTime = System.currentTimeMillis() - Constants.LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000L
            logDir?.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Логируем ошибку при очистке старых логов вместо игнорирования
            // Это помогает при отладке проблем с файловой системой
            android.util.Log.w("FileLogger", "Failed to clean old logs", e)
        }
    }

    companion object {
        @Volatile
        private var instance: FileLogger? = null

        fun getInstance(context: Context): FileLogger {
            return instance ?: synchronized(this) {
                instance ?: FileLogger(context.applicationContext).also { instance = it }
            }
        }
    }
}
