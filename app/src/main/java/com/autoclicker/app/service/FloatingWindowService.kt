package com.autoclicker.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.autoclicker.app.R
import com.autoclicker.app.script.ScriptEngine
import com.autoclicker.app.util.ScriptStorage
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class FloatingWindowService : Service() {

    companion object {
        private const val CHANNEL_ID = "floating_channel"
        private const val NOTIFICATION_ID = 1002
        private const val EXTRA_SCRIPT_ID = "script_id"

        fun startService(context: Context, scriptId: String? = null) {
            val intent = Intent(context, FloatingWindowService::class.java).apply {
                scriptId?.let { putExtra(EXTRA_SCRIPT_ID, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, FloatingWindowService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var storage: ScriptStorage
    private lateinit var prefs: com.autoclicker.app.util.PrefsManager
    
    // Thread-safety: scriptJob is accessed from multiple threads, mark as @Volatile
    @Volatile
    private var scriptJob: Job? = null
    private var serviceScope: CoroutineScope? = null
    private var currentEngine: ScriptEngine? = null
    @Volatile
    private var isRunning = false
    @Volatile
    private var logsVisible = false
    @Volatile
    private var scriptsVisible = false
    @Volatile
    private var isMiniMode = false
    @Volatile
    private var isDestroyed = false
    private var currentScriptId: String? = null
    private var currentScriptName: String = ""

    private lateinit var btnPlay: ImageButton
    private lateinit var btnLogs: ImageButton
    private lateinit var btnClose: ImageButton
    private lateinit var btnSelectScript: ImageButton
    private lateinit var btnClearLogs: ImageButton
    private lateinit var logsContainer: LinearLayout
    private lateinit var scriptsContainer: LinearLayout
    private lateinit var tvLogs: TextView
    private lateinit var tvScriptName: TextView
    private lateinit var logsScrollView: ScrollView
    private lateinit var controlPanel: LinearLayout
    
    private val startScriptLock = Any()

    private val handler = Handler(Looper.getMainLooper())
    private val logBuilder = StringBuilder()
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        
        // Устанавливаем обработчик ошибок для этого сервиса
        Thread.currentThread().setUncaughtExceptionHandler { thread, throwable ->
            com.autoclicker.app.util.CrashHandler.logCritical(
                "FloatingWindowService",
                "Критическая ошибка в сервисе: ${throwable.message}",
                throwable
            )
            com.autoclicker.app.util.CrashHandler.getInstance()?.uncaughtException(thread, throwable)
        }
        
        // Проверяем разрешение на overlay ПЕРЕД созданием окна
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            com.autoclicker.app.util.CrashHandler.logError(
                "FloatingWindowService",
                "Нет разрешения на overlay, сервис не может запуститься"
            )
            handler.post {
                Toast.makeText(this, "Включите разрешение 'Поверх других приложений'", Toast.LENGTH_LONG).show()
            }
            stopSelf()
            return
        }
        
        storage = ScriptStorage(this)
        prefs = com.autoclicker.app.util.PrefsManager(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Панель управления активна"))
        
        try {
            setupFloatingWindow()
        } catch (e: Exception) {
            com.autoclicker.app.util.CrashHandler.logError(
                "FloatingWindowService",
                "Ошибка создания окна: ${e.message}",
                e
            )
            handler.post {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra(EXTRA_SCRIPT_ID)?.let { scriptId ->
            selectScript(scriptId)
        }
        return START_NOT_STICKY
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_panel, null)

        initViews()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.panelX
            y = prefs.panelY
        }

        windowManager.addView(floatingView, params)
        setupTouchListener()
        setupClickListeners()
        
        // Восстанавливаем мини-режим
        isMiniMode = prefs.panelMiniMode
        if (isMiniMode) {
            applyMiniMode(true)
        }
        
        // Загружаем последний скрипт или первый
        val lastId = prefs.lastScriptId
        if (lastId.isNotEmpty()) {
            selectScript(lastId)
        } else {
            storage.getAllScripts().firstOrNull()?.let { selectScript(it.id) }
        }
    }

    private fun initViews() {
        btnPlay = floatingView.findViewById(R.id.btnPlay)
        btnLogs = floatingView.findViewById(R.id.btnLogs)
        btnClose = floatingView.findViewById(R.id.btnClose)
        btnSelectScript = floatingView.findViewById(R.id.btnSelectScript)
        btnClearLogs = floatingView.findViewById(R.id.btnClearLogs)
        logsContainer = floatingView.findViewById(R.id.logsContainer)
        scriptsContainer = floatingView.findViewById(R.id.scriptsContainer)
        tvLogs = floatingView.findViewById(R.id.tvLogs)
        tvScriptName = floatingView.findViewById(R.id.tvScriptName)
        logsScrollView = floatingView.findViewById(R.id.logsScrollView)
        controlPanel = floatingView.findViewById(R.id.controlPanel)
    }

    private fun setupTouchListener() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false
        val clickThreshold = 10

        // Drag по названию скрипта (как header в пипетке)
        tvScriptName.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoving = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = kotlin.math.abs(event.rawX - initialTouchX)
                    val dy = kotlin.math.abs(event.rawY - initialTouchY)
                    
                    if (dx > clickThreshold || dy > clickThreshold) {
                        isMoving = true
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Сохраняем позицию
                    prefs.panelX = params.x
                    prefs.panelY = params.y
                    !isMoving
                }
                else -> false
            }
        }
    }

    private fun setupClickListeners() {
        btnPlay.setOnClickListener {
            if (isRunning) {
                stopScript()
            } else {
                startScript()
            }
        }

        btnLogs.setOnClickListener { toggleLogs() }
        btnClose.setOnClickListener { stopSelf() }
        btnClearLogs.setOnClickListener { clearLogs() }
        btnSelectScript.setOnClickListener { toggleScriptsList() }
        
        // Двойной тап на название для мини-режима
        tvScriptName.setOnClickListener {
            toggleMiniMode()
        }
    }

    private fun toggleMiniMode() {
        isMiniMode = !isMiniMode
        prefs.panelMiniMode = isMiniMode
        applyMiniMode(isMiniMode)
    }

    private fun applyMiniMode(mini: Boolean) {
        if (mini) {
            // Скрываем всё кроме кнопки Play
            btnSelectScript.visibility = View.GONE
            btnLogs.visibility = View.GONE
            btnClose.visibility = View.GONE
            tvScriptName.visibility = View.GONE
            logsContainer.visibility = View.GONE
            scriptsContainer.visibility = View.GONE
            logsVisible = false
            scriptsVisible = false
        } else {
            // Показываем всё
            btnSelectScript.visibility = View.VISIBLE
            btnLogs.visibility = View.VISIBLE
            btnClose.visibility = View.VISIBLE
            tvScriptName.visibility = View.VISIBLE
        }
        windowManager.updateViewLayout(floatingView, params)
    }

    private fun toggleLogs() {
        logsVisible = !logsVisible
        scriptsVisible = false
        logsContainer.visibility = if (logsVisible) View.VISIBLE else View.GONE
        scriptsContainer.visibility = View.GONE
        windowManager.updateViewLayout(floatingView, params)
    }

    private fun toggleScriptsList() {
        scriptsVisible = !scriptsVisible
        logsVisible = false
        logsContainer.visibility = View.GONE
        scriptsContainer.visibility = if (scriptsVisible) View.VISIBLE else View.GONE
        
        if (scriptsVisible) {
            updateScriptsList()
        }
        windowManager.updateViewLayout(floatingView, params)
    }

    private fun updateScriptsList() {
        scriptsContainer.removeAllViews()
        
        val scripts = storage.getAllScripts()
        
        if (scripts.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "Нет скриптов"
                setTextColor(Color.parseColor("#888888"))
                textSize = 12f
                setPadding(8, 8, 8, 8)
            }
            scriptsContainer.addView(emptyText)
            return
        }

        for (script in scripts) {
            val item = TextView(this).apply {
                text = script.name
                setTextColor(if (script.id == currentScriptId) 
                    Color.parseColor("#FF9800") else Color.WHITE)
                textSize = 13f
                setPadding(12, 10, 12, 10)
                background = GradientDrawable().apply {
                    cornerRadius = 6f
                    if (script.id == currentScriptId) {
                        setColor(Color.parseColor("#33FF9800"))
                    }
                }
                setOnClickListener {
                    selectScript(script.id)
                    toggleScriptsList()
                }
            }
            scriptsContainer.addView(item)
        }
    }

    private fun selectScript(scriptId: String) {
        currentScriptId = scriptId
        prefs.lastScriptId = scriptId
        val script = storage.getScript(scriptId)
        currentScriptName = script?.name ?: "Скрипт"
        tvScriptName.text = currentScriptName
        tvScriptName.setTextColor(Color.parseColor("#FF9800"))
        addLog("📝 Выбран: $currentScriptName")
    }

    private fun clearLogs() {
        logBuilder.clear()
        tvLogs.text = "Логи очищены"
    }

    private fun addLog(message: String) {
        if (isDestroyed) return
        handler.post {
            if (isDestroyed) return@post
            val time = dateFormat.format(Date())
            logBuilder.append("[$time] $message\n")
            
            if (logBuilder.length > 5000) {
                logBuilder.delete(0, logBuilder.length - 4000)
            }
            
            tvLogs.text = logBuilder.toString()
            logsScrollView.post { logsScrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun startScript() {
        synchronized(startScriptLock) {
            // Предотвращаем повторный запуск
            if (isRunning) {
                addLog("⚠️ Скрипт уже запущен")
                return
            }

            // Проверяем ScreenCapture
            if (!ScreenCaptureService.isRunning) {
                addLog("❌ Захват экрана не активен!")
                handler.post {
                    Toast.makeText(this, "Включите захват экрана", Toast.LENGTH_SHORT).show()
                }
                return
            }

            // Проверяем Accessibility
            if (ClickerAccessibilityService.instance == null) {
                addLog("❌ Accessibility не включён!")
                handler.post {
                    Toast.makeText(this, "Включите Accessibility Service", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val scriptId = currentScriptId
            val script = if (scriptId != null) {
                storage.getScript(scriptId)
            } else {
                storage.getAllScripts().firstOrNull()
            }
            
            val code = script?.code ?: ""
            currentScriptName = script?.name ?: "Скрипт"

            if (code.isEmpty()) {
                addLog("❌ Скрипт пустой или не найден")
                return
            }

            // Останавливаем предыдущий скрипт если есть
            scriptJob?.cancel()
            serviceScope?.cancel()

            isRunning = true
            handler.post {
                btnPlay.setImageResource(R.drawable.ic_pause)
            }
            updateNotification("▶️ $currentScriptName")
            addLog("▶️ Запущен: $currentScriptName")

            serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            scriptJob = serviceScope?.launch {
                try {
                    val engine = ScriptEngine(
                        context = this@FloatingWindowService,
                        logCallback = { log -> addLog(log) },
                        scriptName = currentScriptName
                    )
                    currentEngine = engine
                    engine.execute(code)
                    addLog("✅ Скрипт завершён")
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // Нормальная отмена, не логируем как ошибку
                    addLog("⏹️ Скрипт отменён")
                } catch (e: OutOfMemoryError) {
                    addLog("❌ Недостаточно памяти!")
                    com.autoclicker.app.util.CrashHandler.logCritical(
                        "FloatingWindowService",
                        "OutOfMemoryError при выполнении скрипта '$currentScriptName'",
                        e
                    )
                } catch (e: Exception) {
                    addLog("❌ Ошибка: ${e.message}")
                    com.autoclicker.app.util.CrashHandler.logError(
                        "FloatingWindowService",
                        "Ошибка выполнения скрипта '$currentScriptName': ${e.message}",
                        e
                    )
                } catch (e: Error) {
                    // Ловим все Error (включая StackOverflowError и т.д.)
                    addLog("❌ Критическая ошибка: ${e.message}")
                    com.autoclicker.app.util.CrashHandler.logCritical(
                        "FloatingWindowService",
                        "Критическая ошибка (Error) в скрипте '$currentScriptName': ${e.message}",
                        e
                    )
                } finally {
                    currentEngine = null
                    handler.post {
                        isRunning = false
                        btnPlay.setImageResource(R.drawable.ic_play)
                        updateNotification("Панель управления активна")
                    }
                }
            }
        }
    }

    private fun stopScript() {
        synchronized(startScriptLock) {
            if (!isRunning) return
            
            val stoppingScriptName = currentScriptName
            
            // Устанавливаем флаг EXIT в ScriptEngine ПЕРВЫМ
            currentEngine?.EXIT = true
            
            isRunning = false
            addLog("⏹️ Скрипт остановлен")
            
            // Сохраняем ссылки перед обнулением
            val job = scriptJob
            val scope = serviceScope
            
            // Обнуляем ссылки
            scriptJob = null
            serviceScope = null
            currentEngine = null
            
            // Отменяем job и scope
            job?.cancel()
            scope?.cancel()
            
            handler.post {
                if (!isDestroyed) {
                    btnPlay.setImageResource(R.drawable.ic_play)
                }
            }
            updateNotification("Панель управления активна")
            
            // Мониторинг: проверяем что скрипт действительно остановился через 3 секунды
            // Используем WeakReference чтобы не держать ссылку на job после уничтожения сервиса
            val jobRef = java.lang.ref.WeakReference(job)
            handler.postDelayed({
                if (isDestroyed) return@postDelayed
                val weakJob = jobRef.get()
                if (weakJob?.isActive == true) {
                    val message = "⚠️ Скрипт '$stoppingScriptName' не остановился после команды STOP"
                    addLog(message)
                    com.autoclicker.app.util.CrashHandler.logWarning(
                        "FloatingWindowService",
                        message
                    )
                }
            }, 3000)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Floating Button", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoClicker")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        isDestroyed = true
        stopScript()
        handler.removeCallbacksAndMessages(null) // Удаляем ВСЕ callbacks
        try {
            if (::floatingView.isInitialized) {
                windowManager.removeView(floatingView)
            }
        } catch (e: Exception) {
            // View already removed
        }
        
        // НЕ останавливаем другие сервисы автоматически
        // Пользователь может хотеть использовать ColorPicker или ScreenCapture отдельно
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Очищаем логи при нехватке памяти
        logBuilder.clear()
        tvLogs.text = "Логи очищены (мало памяти)"
    }
}
