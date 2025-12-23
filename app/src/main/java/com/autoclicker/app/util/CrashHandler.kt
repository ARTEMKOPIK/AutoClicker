package com.autoclicker.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Global crash handler and error reporter.
 * 
 * Captures uncaught exceptions and sends them to Telegram via Bot API.
 * Also provides static methods for logging at different levels.
 * 
 * УЛУЧШЕНИЯ:
 * - Очередь сообщений для гарантированной доставки
 * - Retry механизм при неудачной отправке
 * - Отслеживание ANR (Application Not Responding)
 * - Батчинг сообщений для уменьшения нагрузки
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
        .retryOnConnectionFailure(true)
        .build()
    
    // Очередь для гарантированной доставки сообщений
    private val messageQueue = ConcurrentLinkedQueue<QueuedMessage>()
    private val isProcessingQueue = AtomicBoolean(false)
    private val failedAttempts = AtomicInteger(0)
    private val maxRetries = 3
    
    // ANR detection
    private var anrWatchdog: Thread? = null
    private val anrThresholdMs = 5000L // 5 секунд
    @Volatile
    private var lastMainThreadResponse = System.currentTimeMillis()
    
    data class QueuedMessage(
        val report: String,
        val timestamp: Long = System.currentTimeMillis(),
        var retryCount: Int = 0
    )

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
                        instance?.startAnrWatchdog()
                        Log.i("CrashHandler", "Crash handler initialized successfully with ANR detection")
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
         * Отправить ошибку (Exception) - ГАРАНТИРОВАННАЯ ДОСТАВКА
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
         * Отправить любое исключение - ГАРАНТИРОВАННАЯ ДОСТАВКА
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
        
        /**
         * Отправить критическую ошибку с немедленной синхронной отправкой
         * Используется для ошибок которые могут привести к крашу
         */
        fun logCritical(tag: String, message: String, throwable: Throwable? = null) {
            getInstance()?.let { handler ->
                handler.reportCritical(tag, message, throwable)
            } ?: run {
                Log.e(tag, "CRITICAL - CrashHandler not initialized: $message", throwable)
            }
        }
    }

    enum class ErrorLevel(val emoji: String, val label: String) {
        DEBUG("🔍", "DEBUG"),
        INFO("ℹ️", "INFO"),
        WARNING("⚠️", "WARNING"),
        ERROR("❌", "ERROR"),
        CRITICAL("🆘", "CRITICAL"),
        ANR("🐌", "ANR"),
        CRASH("🔴", "CRASH")
    }
    
    /**
     * Запуск ANR watchdog для отслеживания зависаний главного потока
     */
    private fun startAnrWatchdog() {
        anrWatchdog = Thread({
            while (!Thread.currentThread().isInterrupted) {
                try {
                    lastMainThreadResponse = 0L
                    
                    // Отправляем ping в главный поток
                    android.os.Handler(Looper.getMainLooper()).post {
                        lastMainThreadResponse = System.currentTimeMillis()
                    }
                    
                    Thread.sleep(anrThresholdMs)
                    
                    // Проверяем ответил ли главный поток
                    if (lastMainThreadResponse == 0L) {
                        // ANR detected!
                        val stackTraces = buildAnrStackTrace()
                        reportAnr(stackTraces)
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.e("CrashHandler", "ANR watchdog error", e)
                }
            }
        }, "ANR-Watchdog")
        anrWatchdog?.isDaemon = true
        anrWatchdog?.start()
    }
    
    private fun buildAnrStackTrace(): String {
        val sb = StringBuilder()
        val mainThread = Looper.getMainLooper().thread
        
        sb.appendLine("=== MAIN THREAD ===")
        mainThread.stackTrace.forEach { element ->
            sb.appendLine("    at $element")
        }
        
        sb.appendLine()
        sb.appendLine("=== OTHER THREADS ===")
        Thread.getAllStackTraces().forEach { (thread, stack) ->
            if (thread != mainThread && thread.name != "ANR-Watchdog") {
                sb.appendLine("Thread: ${thread.name} (${thread.state})")
                stack.take(5).forEach { element ->
                    sb.appendLine("    at $element")
                }
                sb.appendLine()
            }
        }
        
        return sb.toString()
    }
    
    private fun reportAnr(stackTraces: String) {
        val report = buildReport(
            level = ErrorLevel.ANR,
            tag = "ANR-Watchdog",
            message = "Приложение не отвечает более ${anrThresholdMs}ms",
            stackTrace = stackTraces,
            extras = mapOf(
                "Threshold" to "${anrThresholdMs}ms",
                "Memory" to getMemoryInfo()
            )
        )
        
        // ANR отправляем синхронно в отдельном потоке
        Thread {
            sendToTelegramSync(report)
            saveCrashLocally(report)
        }.start()
    }
    
    private fun getMemoryInfo(): String {
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMem = runtime.maxMemory() / 1024 / 1024
        return "${usedMem}MB / ${maxMem}MB"
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val crashReport = buildCrashReport(thread, throwable)
            
            // Пробуем отправить синхронно с retry
            var sent = false
            for (i in 0 until maxRetries) {
                if (sendToTelegramSync(crashReport)) {
                    sent = true
                    break
                }
                Thread.sleep(500) // Небольшая пауза между попытками
            }
            
            if (!sent) {
                // Сохраняем для отправки при следующем запуске
                savePendingReport(crashReport)
            }
            
            saveCrashLocally(crashReport)
            Thread.sleep(Constants.CRASH_REPORT_DELAY_MS)
        } catch (e: Exception) {
            // Even crash handler can fail, use safe fallback
            Log.e("CrashHandler", "Crash handler failed", e)
            throwable.printStackTrace()
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }
    
    /**
     * Критическая ошибка - синхронная отправка
     */
    fun reportCritical(tag: String, message: String, throwable: Throwable?) {
        if (BOT_TOKEN.isEmpty() || CHAT_ID.isEmpty()) return
        
        val stackTrace = throwable?.let {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            it.printStackTrace(pw)
            sw.toString()
        }
        
        val report = buildReport(
            level = ErrorLevel.CRITICAL,
            tag = tag,
            message = message,
            stackTrace = stackTrace,
            extras = mapOf("Memory" to getMemoryInfo())
        )
        
        Log.e(tag, message, throwable)
        
        // Синхронная отправка в отдельном потоке
        Thread {
            sendToTelegramSync(report)
            saveCrashLocally(report)
        }.start()
    }
    
    /**
     * Сохранение отчёта для отправки при следующем запуске
     */
    private fun savePendingReport(report: String) {
        try {
            val file = context.getFileStreamPath("pending_crash_reports.txt")
            file.appendText("\n\n${"=".repeat(50)}\n\n$report")
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to save pending report", e)
        }
    }
    
    /**
     * Отправка отложенных отчётов (вызывается при запуске приложения)
     */
    fun sendPendingReports() {
        Thread {
            try {
                val file = context.getFileStreamPath("pending_crash_reports.txt")
                if (file.exists() && file.length() > 0) {
                    val content = file.readText()
                    if (sendToTelegramSync("📤 ОТЛОЖЕННЫЕ ОТЧЁТЫ:\n\n$content")) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("CrashHandler", "Failed to send pending reports", e)
            }
        }.start()
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
            extras = mapOf(
                "Thread" to thread.name,
                "Memory" to getMemoryInfo()
            )
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
            ErrorLevel.ERROR, ErrorLevel.CRASH, ErrorLevel.CRITICAL, ErrorLevel.ANR -> Log.e(tag, message, throwable)
        }

        // Для ERROR и выше добавляем в очередь с гарантированной доставкой
        if (level.ordinal >= ErrorLevel.ERROR.ordinal) {
            addToQueue(report)
        } else {
            sendToTelegramAsync(report)
        }
    }
    
    /**
     * Добавление сообщения в очередь с гарантированной доставкой
     */
    private fun addToQueue(report: String) {
        messageQueue.offer(QueuedMessage(report))
        processQueue()
    }
    
    /**
     * Обработка очереди сообщений
     */
    private fun processQueue() {
        if (!isProcessingQueue.compareAndSet(false, true)) {
            return // Уже обрабатывается
        }
        
        Thread {
            try {
                while (messageQueue.isNotEmpty()) {
                    val message = messageQueue.peek() ?: break
                    
                    if (sendToTelegramSync(message.report)) {
                        messageQueue.poll() // Успешно отправлено, удаляем
                        failedAttempts.set(0)
                    } else {
                        message.retryCount++
                        if (message.retryCount >= maxRetries) {
                            // Сохраняем для отправки позже
                            savePendingReport(message.report)
                            messageQueue.poll()
                        } else {
                            // Ждём перед повторной попыткой
                            Thread.sleep(1000L * message.retryCount)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CrashHandler", "Queue processing error", e)
            } finally {
                isProcessingQueue.set(false)
            }
        }.start()
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

    private fun sendToTelegramSync(report: String): Boolean {
        if (BOT_TOKEN.isEmpty() || CHAT_ID.isEmpty()) return false

        val request = buildTelegramRequest(report)
        return try {
            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            response.close()
            success
        } catch (e: IOException) {
            Log.e("CrashHandler", "Failed to send report sync", e)
            false
        }
    }

    private fun sendToTelegramAsync(report: String) {
        if (BOT_TOKEN.isEmpty() || CHAT_ID.isEmpty()) return

        val request = buildTelegramRequest(report)
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CrashHandler", "Failed to send report async", e)
                // При неудаче добавляем в очередь
                addToQueue(report)
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
            
            // Ограничиваем размер файла
            if (file.length() > 500 * 1024) { // 500KB
                val content = file.readText()
                file.writeText(content.takeLast(400 * 1024))
            }
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to save crash locally", e)
        }
    }
    
    /**
     * Остановка ANR watchdog при уничтожении
     */
    fun shutdown() {
        anrWatchdog?.interrupt()
        anrWatchdog = null
    }
}
