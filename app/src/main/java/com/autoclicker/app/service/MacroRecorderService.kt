package com.autoclicker.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.autoclicker.app.R

import java.util.concurrent.CopyOnWriteArrayList

/**
 * Сервис для записи макросов (действий пользователя)
 */
class MacroRecorderService : Service() {

    companion object {
        private const val CHANNEL_ID = "macro_recorder_channel"

        @Volatile
        var isRecording = false
            private set

        var recordedActions: List<RecordedAction> = CopyOnWriteArrayList()
            private set

        fun startService(context: Context) {
            val intent = Intent(context, MacroRecorderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, MacroRecorderService::class.java))
        }

        fun generateScript(): String {
            val currentActions = recordedActions
            if (currentActions.isEmpty()) return ""

            val builder = StringBuilder()
            builder.appendLine("// Записанный макрос")
            builder.appendLine("// ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
            builder.appendLine()
            builder.appendLine("log(\"Макрос запущен\")")
            builder.appendLine()

            var lastTime = currentActions.first().timestamp

            for (action in currentActions) {
                val rawDelay = action.timestamp - lastTime
                val delay = if (rawDelay > com.autoclicker.app.util.Constants.MAX_DELAY_MS) {
                    com.autoclicker.app.util.CrashHandler.logWarning("MacroRecorder", "Large delay detected: $rawDelay ms, limiting to ${com.autoclicker.app.util.Constants.MAX_DELAY_MS}")
                    com.autoclicker.app.util.Constants.MAX_DELAY_MS
                } else rawDelay

                if (delay > 50) {
                    builder.appendLine("sleep($delay)")
                }

                when (action.type) {
                    ActionType.CLICK -> {
                        builder.appendLine("click(${action.x.toInt()}, ${action.y.toInt()})")
                    }
                    ActionType.LONG_CLICK -> {
                        builder.appendLine("longClick(${action.x.toInt()}, ${action.y.toInt()}, ${action.duration})")
                    }
                    ActionType.SWIPE -> {
                        builder.appendLine("swipe(${action.x.toInt()}, ${action.y.toInt()}, ${action.x2.toInt()}, ${action.y2.toInt()}, ${action.duration})")
                    }
                }

                lastTime = action.timestamp + (action.duration ?: 0)
            }

            builder.appendLine()
            builder.appendLine("log(\"Макрос завершён\")")

            return builder.toString()
        }
    }

    enum class ActionType {
        CLICK, LONG_CLICK, SWIPE
    }

    data class RecordedAction(
        val type: ActionType,
        val x: Float,
        val y: Float,
        val x2: Float = 0f,
        val y2: Float = 0f,
        val timestamp: Long,
        val duration: Long? = null
    )

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var controlView: View? = null

    private val actions = mutableListOf<RecordedAction>()
    private var recordStartTime = 0L
    private var touchStartTime = 0L
    private var touchStartX = 0f
    private var touchStartY = 0f

    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        // Устанавливаем обработчик ошибок для этого сервиса
        Thread.currentThread().setUncaughtExceptionHandler { thread, throwable ->
            com.autoclicker.app.util.CrashHandler.logCritical(
                "MacroRecorderService",
                "Критическая ошибка в записи макроса: ${throwable.message}",
                throwable
            )
            com.autoclicker.app.util.CrashHandler.getInstance()?.uncaughtException(thread, throwable)
        }
        
        createNotificationChannel()
        startForeground(com.autoclicker.app.util.Constants.NOTIFICATION_ID_MACRO_RECORDER, createNotification())
        setupOverlay()
        isRecording = true
        recordStartTime = SystemClock.elapsedRealtime()
        actions.clear()
        Toast.makeText(this, "Запись макроса начата", Toast.LENGTH_SHORT).show()
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Прозрачный оверлей для записи касаний
        overlayView = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        // Панель управления записью
        controlView = LayoutInflater.from(this).inflate(R.layout.macro_recorder_control, null)

        val controlParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 100
        }

        windowManager.addView(overlayView, overlayParams)
        windowManager.addView(controlView, controlParams)

        setupTouchListener()
        setupControlButtons()
    }

    private fun setupTouchListener() {
        overlayView?.setOnTouchListener { _, event ->
            if (actions.size >= com.autoclicker.app.util.Constants.MAX_MACRO_ACTIONS) {
                Toast.makeText(this, "Достигнут лимит действий (${com.autoclicker.app.util.Constants.MAX_MACRO_ACTIONS})", Toast.LENGTH_SHORT).show()
                stopRecording()
                return@setOnTouchListener false
            }
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartTime = SystemClock.elapsedRealtime()
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                }
                MotionEvent.ACTION_UP -> {
                    val duration = SystemClock.elapsedRealtime() - touchStartTime
                    val dx = event.rawX - touchStartX
                    val dy = event.rawY - touchStartY
                    val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                    val timestamp = touchStartTime - recordStartTime

                    when {
                        distance > 50 -> {
                            // Свайп
                            actions.add(RecordedAction(
                                type = ActionType.SWIPE,
                                x = touchStartX,
                                y = touchStartY,
                                x2 = event.rawX,
                                y2 = event.rawY,
                                timestamp = timestamp,
                                duration = duration
                            ))
                            updateCounter()
                        }
                        duration > 500 -> {
                            // Долгий клик
                            actions.add(RecordedAction(
                                type = ActionType.LONG_CLICK,
                                x = touchStartX,
                                y = touchStartY,
                                timestamp = timestamp,
                                duration = duration
                            ))
                            updateCounter()
                        }
                        else -> {
                            // Обычный клик
                            actions.add(RecordedAction(
                                type = ActionType.CLICK,
                                x = touchStartX,
                                y = touchStartY,
                                timestamp = timestamp
                            ))
                            updateCounter()
                        }
                    }
                }
            }
            false // Пропускаем событие дальше
        }
    }

    private fun setupControlButtons() {
        controlView?.findViewById<ImageButton>(R.id.btnStopRecording)?.setOnClickListener {
            stopRecording()
        }

        controlView?.findViewById<ImageButton>(R.id.btnCancelRecording)?.setOnClickListener {
            actions.clear()
            stopSelf()
        }
    }

    private fun updateCounter() {
        controlView?.findViewById<TextView>(R.id.tvActionCount)?.text = "${actions.size}"
    }

    private fun stopRecording() {
        recordedActions = actions.toList()
        isRecording = false
        
        val actionsCount = actions.size
        if (actionsCount == 0) {
            Toast.makeText(this, "Нет записанных действий", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }
        
        // Открываем ScriptEditorActivity с записанным макросом
        val intent = Intent(this, com.autoclicker.app.ScriptEditorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("macro_script", generateScript())
            putExtra("macro_actions_count", actionsCount)
        }
        startActivity(intent)
        
        Toast.makeText(this, "Записано $actionsCount действий", Toast.LENGTH_SHORT).show()
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Macro Recorder",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Запись макроса")
            .setContentText("Выполняйте действия на экране")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        handler.removeCallbacksAndMessages(null) // Очищаем все callbacks
        try {
            overlayView?.setOnTouchListener(null)
            overlayView?.let { windowManager.removeView(it) }
            controlView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            // Views already removed
        }
        overlayView = null
        controlView = null
    }
}
