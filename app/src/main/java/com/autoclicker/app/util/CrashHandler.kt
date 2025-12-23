package com.autoclicker.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.autoclicker.app.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Global crash handler and error reporter.
 * 
 * Captures uncaught exceptions and sends them to Telegram via Bot API.
 * Also provides static methods for logging at different levels.
 * 
 * Thread-safety: The singleton implementation uses double-checked locking
 * with @Volatile for thread-safe initialization. Static methods are safe
 * to call from any thread as they handle null instances gracefully.
 * 
 * JSON escaping uses JsonEscaper utility for consistent and correct escaping order.
 * 
 * @property context Application context for accessing package info and file storage
 */
class CrashHandler private constructor(
    private val context: Context
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    private val client = OkHttpClient.Builder()
        .connectTimeout(Constants.CRASH_REPORT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        .writeTimeout(Constants.CRASH_REPORT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        .readTimeout(Constants.CRASH_REPORT_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        .build()

    companion object {
        private const val BOT_TOKEN = BuildConfig.CRASH_BOT_TOKEN
        private const val CHAT_ID = BuildConfig.CRASH_CHAT_ID

        @Volatile
        private var instance: CrashHandler? = null

        /**
         * Initialize the crash handler singleton.
         * This method is thread-safe and can be called multiple times.
         * 
         * @param context Application context (extracts applicationContext automatically)
         */
        @Synchronized
        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = CrashHandler(context.applicationContext)
                        Thread.setDefaultUncaughtExceptionHandler(instance)
                        Log.i("CrashHandler", "Crash handler initialized successfully")
                    }
                }
            }
        }

        /**
         * Get the singleton instance.
         * 
         * @return CrashHandler instance or null if not initialized
         */
        fun getInstance(): CrashHandler? {
            return instance
        }

        // === Удобные статические методы для логирования ===
        
        /**
         * Отправить ошибку (Exception)
         * @param tag Источник ошибки (класс или модуль)
         * @param message Сообщение об ошибке
         * @param throwable Опциональное исключение
         */
        fun logError(tag: String, message: String, throwable: Throwable? = null) {
            // Null-safe: works even if crash handler is not initialized
            getInstance()?.let { handler ->
                handler.reportError(tag, message, throwable, ErrorLevel.ERROR)
            } ?: run {
                // Fallback to Log.e if crash handler not initialized
                Log.e(tag, "CrashHandler not initialized: $message", throwable)
            }
        }

        /**
         * Отправить предупреждение
         * @param tag Источник предупреждения
         * @param message Сообщение предупреждения
         * @param throwable Опциональное исключение
         */
        fun logWarning(tag: String, message: String, throwable: Throwable? = null) {
            getInstance()?.let { handler ->
                handler.reportError(tag, message, throwable, ErrorLevel.WARNING)
            } ?: run {
                Log.w(tag, "CrashHandler not initialized: $message", throwable)
            }
        }

        /**
         * Отправить информационное сообщение
         * @param tag Источник информации
         * @param message Информационное сообщение
         */
        fun logInfo(tag: String, message: String) {
            getInstance()?.let { handler ->
                handler.reportError(tag, message, null, ErrorLevel.INFO)
            } ?: run {
                Log.i(tag, "CrashHandler not initialized: $message")
            }
        }

        /**
         * Отправить debug сообщение
         * @param tag Источник отладки
         * @param message Debug сообщение
         */
        fun logDebug(tag: String, message: String) {
            getInstance()?.let { handler ->
                handler.reportError(tag, message, null, ErrorLevel.DEBUG)
            } ?: run {
                Log.d(tag, "CrashHandler not initialized: $message")
            }
        }

        /**
         * Отправить любое исключение
         * @param throwable Исключение для логирования
         */
        fun logException(throwable: Throwable) {
            val tag = throwable.stackTrace.firstOrNull()?.className?.substringAfterLast('.') ?: "Unknown"
            getInstance()?.let { handler ->
                handler.reportError(tag, throwable.message ?: "No message", throwable, ErrorLevel.ERROR)
            } ?: run {
                Log.e(tag, "CrashHandler not initialized", throwable)
            }
        }
    }

    enum class ErrorLevel(val emoji: String, val label: String) {
        DEBUG("🔍", "DEBUG"),
        INFO("ℹ️", "INFO"),
        WARNING("⚠️", "WARNING"),
        ERROR("❌", "ERROR"),
        CRASH("🔴", "CRASH")
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val crashReport = buildCrashReport(thread, throwable)
            sendToTelegramSync(crashReport)
            saveCrashLocally(crashReport)
            Thread.sleep(Constants.CRASH_REPORT_DELAY_MS)
        } catch (e: Exception) {
            // Even crash handler can fail, use safe fallback
            Log.e("CrashHandler", "Crash handler failed", e)
            throwable.printStackTrace()
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun buildCrashReport(thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        return buildReport(
            level = ErrorLevel.CRASH,
            tag = "UncaughtException",
            message = "${throwable.javaClass.simpleName}: ${throwable.message}",
            stackTrace = stackTrace,
            extras = mapOf("Thread" to thread.name)
        )
    }

    fun reportError(tag: String, message: String, throwable: Throwable? = null, level: ErrorLevel = ErrorLevel.ERROR) {
        if (BOT_TOKEN.isEmpty() || CHAT_ID.isEmpty()) return

        val stackTrace = throwable?.let {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            it.printStackTrace(pw)
            sw.toString()
        }

        val report = buildReport(
            level = level,
            tag = tag,
            message = message,
            stackTrace = stackTrace
        )

        // Также логируем локально
        when (level) {
            ErrorLevel.DEBUG -> Log.d(tag, message, throwable)
            ErrorLevel.INFO -> Log.i(tag, message, throwable)
            ErrorLevel.WARNING -> Log.w(tag, message, throwable)
            ErrorLevel.ERROR, ErrorLevel.CRASH -> Log.e(tag, message, throwable)
        }

        sendToTelegramAsync(report)
    }

    private fun buildReport(
        level: ErrorLevel,
        tag: String,
        message: String,
        stackTrace: String? = null,
        extras: Map<String, String> = emptyMap()
    ): String {
        val dateFormat = SimpleDateFormat(Constants.CRASH_REPORT_DATE_PATTERN, Locale.getDefault())
        val timestamp = dateFormat.format(Date())

        val appVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pInfo.versionName} (${pInfo.longVersionCode})"
        } catch (e: PackageManager.NameNotFoundException) {
            "Unknown"
        }

        return buildString {
            appendLine("${level.emoji} ${level.label} REPORT")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine()
            appendLine("📱 Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("🤖 Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("📦 App: $appVersion")
            appendLine("⏰ Time: $timestamp")
            appendLine("🏷 Tag: $tag")
            
            extras.forEach { (key, value) ->
                appendLine("🧵 $key: $value")
            }
            
            appendLine()
            appendLine("📝 Message:")
            appendLine(message.take(Constants.MAX_LOG_LENGTH))
            
            if (!stackTrace.isNullOrEmpty()) {
                appendLine()
                appendLine("📋 Stack Trace:")
                appendLine(stackTrace.take(Constants.MAX_STACK_TRACE_LENGTH))
            }
        }
    }

    private fun sendToTelegramSync(report: String) {
        if (BOT_TOKEN.isEmpty() || CHAT_ID.isEmpty()) return

        val request = buildTelegramRequest(report)
        try {
            client.newCall(request).execute().close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun sendToTelegramAsync(report: String) {
        if (BOT_TOKEN.isEmpty() || CHAT_ID.isEmpty()) return

        val request = buildTelegramRequest(report)
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CrashHandler", "Failed to send report", e)
            }
            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }

    private fun buildTelegramRequest(report: String): Request {
        val url = "https://api.telegram.org/bot$BOT_TOKEN/sendMessage"
        
        val escapedText = JsonEscaper.escape(report)
        val json = """{"chat_id":"$CHAT_ID","text":"$escapedText","parse_mode":""}"""

        return Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
    }

    private fun saveCrashLocally(report: String) {
        try {
            val file = context.getFileStreamPath(Constants.CRASH_LOG_FILENAME)
            file.appendText("\n\n${"=".repeat(50)}\n\n$report")
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to save crash locally", e)
        }
    }
}
