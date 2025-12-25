package com.autoclicker.app.service

import android.app.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.autoclicker.app.R
import com.autoclicker.app.util.CrashHandler
import com.autoclicker.app.util.PrefsManager

class ColorPickerService : Service() {

    companion object {
        private const val CHANNEL_ID = "color_picker_channel"
        private const val HISTORY_VERSION = "v1"

        fun startService(context: Context) {
            val intent = Intent(context, ColorPickerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, ColorPickerService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var pickerView: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var vibrator: Vibrator

    private var crosshairView: View? = null
    private var crosshairParams: WindowManager.LayoutParams? = null

    private lateinit var colorPreviewLarge: View
    private lateinit var colorDot: View
    private lateinit var tvHexColor: TextView
    private lateinit var tvCoordinates: TextView
    private lateinit var tvRgbColor: TextView
    private lateinit var tvIntColor: TextView
    private lateinit var tvPickHint: TextView
    private lateinit var btnPickColor: ImageButton
    private lateinit var historyContainer: LinearLayout

    @Volatile private var currentColor = 0
    @Volatile private var currentX = 0
    @Volatile private var currentY = 0
    @Volatile private var isPicking = false
    private var lastVibrationColor = 0

    private val colorHistory = mutableListOf<ColorEntry>()

    data class ColorEntry(val color: Int, val x: Int, val y: Int)

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isPicking) {
                captureColorAt(currentX, currentY)
                handler.postDelayed(this, 50)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // Устанавливаем обработчик ошибок для этого сервиса
        Thread.currentThread().setUncaughtExceptionHandler { thread, throwable ->
            com.autoclicker.app.util.CrashHandler.logCritical(
                "ColorPickerService",
                "Критическая ошибка в пипетке: ${throwable.message}",
                throwable
            )
            com.autoclicker.app.util.CrashHandler.getInstance()?.uncaughtException(thread, throwable)
        }

        // Проверяем разрешение на overlay ПЕРЕД созданием окна
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            com.autoclicker.app.util.CrashHandler.logError(
                "ColorPickerService",
                "Нет разрешения на overlay"
            )
            handler.post {
                Toast.makeText(this, "Включите разрешение 'Поверх других приложений'", Toast.LENGTH_LONG).show()
            }
            stopSelf()
            return
        }

        createNotificationChannel()
        startForeground(com.autoclicker.app.util.Constants.NOTIFICATION_ID_COLOR_PICKER, createNotification())
        initVibrator()
        loadColorHistory()

        try {
            setupFloatingPicker()
        } catch (e: Exception) {
            com.autoclicker.app.util.CrashHandler.logError(
                "ColorPickerService",
                "Ошибка создания окна пипетки: ${e.message}",
                e
            )
            handler.post {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
            stopSelf()
        }
    }

    private fun loadColorHistory() {
        try {
            val prefs = getSharedPreferences(com.autoclicker.app.util.Constants.PREFS_COLOR_HISTORY_KEY, Context.MODE_PRIVATE)
            val historyJson = prefs.getString(com.autoclicker.app.util.Constants.PREFS_KEY_HISTORY, null) ?: return

            // Проверка версии
            if (!historyJson.startsWith(HISTORY_VERSION + "|")) {
                CrashHandler.logWarning("ColorPickerService", "Old history version detected or corrupted, clearing")
                return
            }

            val data = historyJson.substring(HISTORY_VERSION.length + 1)
            val entries = data.split(";").mapNotNull { entry ->
                val parts = entry.split(",")
                if (parts.size == 3) {
                    try {
                        ColorEntry(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                    } catch (e: NumberFormatException) {
                        null
                    }
                } else null
            }
            colorHistory.clear()
            colorHistory.addAll(entries.take(com.autoclicker.app.util.Constants.MAX_HISTORY_COLOR))
        } catch (e: Exception) {
            CrashHandler.logWarning("ColorPickerService", "Failed to load color history", e)
            colorHistory.clear()
        }
    }

    private fun saveColorHistory() {
        try {
            val prefs = getSharedPreferences(com.autoclicker.app.util.Constants.PREFS_COLOR_HISTORY_KEY, Context.MODE_PRIVATE)
            val historyData = colorHistory.joinToString(";") { "${it.color},${it.x},${it.y}" }
            val historyJson = "$HISTORY_VERSION|$historyData"
            prefs.edit().putString(com.autoclicker.app.util.Constants.PREFS_KEY_HISTORY, historyJson).apply()
        } catch (e: Exception) {
            CrashHandler.logError("ColorPickerService", "Error saving color history", e)
        }
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    private fun setupFloatingPicker() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        pickerView = LayoutInflater.from(this).inflate(R.layout.floating_color_picker, null)

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
            gravity = Gravity.TOP or Gravity.END
            val prefs = PrefsManager(this@ColorPickerService)
            x = prefs.pickerX
            y = prefs.pickerY
        }

        windowManager.addView(pickerView, params)
        setupDragListener()
        setupButtonListeners()
        setupPickerButton()
    }

    private fun initViews() {
        colorPreviewLarge = pickerView.findViewById(R.id.colorPreviewLarge)
        colorDot = pickerView.findViewById(R.id.colorDot)
        tvHexColor = pickerView.findViewById(R.id.tvHexColor)
        tvCoordinates = pickerView.findViewById(R.id.tvCoordinates)
        tvRgbColor = pickerView.findViewById(R.id.tvRgbColor)
        tvIntColor = pickerView.findViewById(R.id.tvIntColor)
        tvPickHint = pickerView.findViewById(R.id.tvPickHint)
        btnPickColor = pickerView.findViewById(R.id.btnPickColor)
        historyContainer = pickerView.findViewById(R.id.historyContainer)
    }

    private fun setupDragListener() {
        val headerPanel = pickerView.findViewById<View>(R.id.headerPanel)
        
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        headerPanel.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(pickerView, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupButtonListeners() {
        pickerView.findViewById<ImageButton>(R.id.btnClosePicker).setOnClickListener {
            stopSelf()
        }

        pickerView.findViewById<Button>(R.id.btnCopyHex).setOnClickListener {
            if (currentColor != 0) {
                copyToClipboard(String.format("#%06X", 0xFFFFFF and currentColor))
                showToast("HEX скопирован")
            }
        }

        pickerView.findViewById<Button>(R.id.btnCopyInt).setOnClickListener {
            if (currentColor != 0) {
                copyToClipboard(currentColor.toString())
                showToast("INT скопирован")
            }
        }

        pickerView.findViewById<Button>(R.id.btnCopyCoords).setOnClickListener {
            if (currentX != 0 || currentY != 0) {
                copyToClipboard("$currentX, $currentY")
                showToast("Координаты скопированы")
            }
        }
    }

    private fun setupPickerButton() {
        btnPickColor.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startPicking()
                    currentX = event.rawX.toInt()
                    currentY = event.rawY.toInt()
                    showCrosshair(currentX, currentY)
                    vibrate()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    currentX = event.rawX.toInt()
                    currentY = event.rawY.toInt()
                    moveCrosshair(currentX, currentY)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopPicking()
                    hideCrosshair()
                    addToHistory(currentColor, currentX, currentY)
                    vibrate()
                    true
                }
                else -> false
            }
        }
    }

    private fun startPicking() {
        isPicking = true
        lastVibrationColor = 0
        tvPickHint.text = "Выбираю..."
        tvPickHint.setTextColor(Color.parseColor("#FF9800"))
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }

    private fun stopPicking() {
        isPicking = false
        handler.removeCallbacks(updateRunnable)
        tvPickHint.text = "Зажми и води\nпо экрану"
        tvPickHint.setTextColor(Color.parseColor("#888888"))
    }

    private fun showCrosshair(x: Int, y: Int) {
        if (crosshairView != null) return

        crosshairView = View(this).apply {
            setBackgroundResource(R.drawable.crosshair_indicator)
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val sizePx = (48 * resources.displayMetrics.density).toInt()
        val halfSize = sizePx / 2
        
        crosshairParams = WindowManager.LayoutParams(
            sizePx, sizePx,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x - halfSize
            this.y = y - halfSize
        }

        windowManager.addView(crosshairView, crosshairParams)
    }

    private fun moveCrosshair(x: Int, y: Int) {
        val sizePx = (48 * resources.displayMetrics.density).toInt()
        val halfSize = sizePx / 2
        crosshairParams?.let { p ->
            p.x = x - halfSize
            p.y = y - halfSize
            crosshairView?.let { windowManager.updateViewLayout(it, p) }
        }
    }

    private fun hideCrosshair() {
        crosshairView?.let {
            windowManager.removeView(it)
            crosshairView = null
            crosshairParams = null
        }
    }

    private fun captureColorAt(x: Int, y: Int) {
        val captureService = ScreenCaptureService.instance
        if (captureService == null) {
            handler.post {
                tvPickHint.text = "Включите\nзахват экрана"
                tvPickHint.setTextColor(Color.parseColor("#F44336"))
            }
            return
        }
        
        val color = captureService.getPixelColor(x, y)
        if (color == 0) return
        
        // Вибрация при смене цвета
        if (color != lastVibrationColor) {
            lastVibrationColor = color
        }
        
        currentColor = color
        currentX = x
        currentY = y

        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        val hexColor = String.format("#%06X", 0xFFFFFF and color)

        handler.post {
            updateColorPreview(color)
            tvHexColor.text = hexColor
            tvCoordinates.text = "📍 X: $x  Y: $y"
            tvRgbColor.text = "🎨 R: $red  G: $green  B: $blue"
            tvIntColor.text = "🔢 Int: $color"

            val brightness = (red * 299 + green * 587 + blue * 114) / 1000
            tvHexColor.setTextColor(if (brightness > 128) Color.BLACK else Color.WHITE)
        }
    }

    private fun addToHistory(color: Int, x: Int, y: Int) {
        if (color == 0) return
        
        // Удаляем дубликат если есть
        colorHistory.removeAll { it.color == color }
        
        // Добавляем в начало
        colorHistory.add(0, ColorEntry(color, x, y))
        
        // Ограничиваем размер
        while (colorHistory.size > com.autoclicker.app.util.Constants.MAX_HISTORY_COLOR) {
            colorHistory.removeAt(colorHistory.size - 1)
        }
        
        updateHistoryUI()
        saveColorHistory()
    }

    private fun updateHistoryUI() {
        historyContainer.removeAllViews()
        
        for (entry in colorHistory) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(28, 28).apply {
                    marginEnd = 6
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(entry.color)
                    setStroke(1, Color.parseColor("#444444"))
                }
                setOnClickListener {
                    // Восстанавливаем цвет из истории (без повторного захвата)
                    currentColor = entry.color
                    currentX = entry.x
                    currentY = entry.y
                    
                    // Проверяем что views ещё существуют
                    if (!this@ColorPickerService::colorPreviewLarge.isInitialized) return@setOnClickListener
                    
                    updateColorPreview(entry.color)
                    
                    val red = Color.red(entry.color)
                    val green = Color.green(entry.color)
                    val blue = Color.blue(entry.color)
                    tvHexColor.text = String.format("#%06X", 0xFFFFFF and entry.color)
                    tvCoordinates.text = "📍 X: ${entry.x}  Y: ${entry.y}"
                    tvRgbColor.text = "🎨 R: $red  G: $green  B: $blue"
                    tvIntColor.text = "🔢 Int: ${entry.color}"
                    
                    // Обновляем цвет текста в зависимости от яркости
                    val brightness = (red * 299 + green * 587 + blue * 114) / 1000
                    tvHexColor.setTextColor(if (brightness > 128) Color.BLACK else Color.WHITE)
                    
                    vibrate()
                }
            }
            historyContainer.addView(dot)
        }
        
        historyContainer.visibility = if (colorHistory.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun updateColorPreview(color: Int) {
        val previewDrawable = GradientDrawable().apply {
            setColor(color)
            cornerRadius = 8 * resources.displayMetrics.density
            setStroke(2, Color.parseColor("#333333"))
        }
        colorPreviewLarge.background = previewDrawable

        val dotDrawable = GradientDrawable().apply {
            setColor(color)
            shape = GradientDrawable.OVAL
        }
        colorDot.background = dotDrawable
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("color", text)
        clipboard.setPrimaryClip(clip)
        vibrate()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Color Picker", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Color Picker")
            .setContentText("Зажмите прицел и водите по экрану")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        isPicking = false
        handler.removeCallbacks(updateRunnable)
        handler.removeCallbacksAndMessages(null) // Удаляем ВСЕ callbacks
        
        try {
            hideCrosshair()
        } catch (e: Exception) {
            CrashHandler.logError("ColorPickerService", "Error removing crosshair view", e)
        }
        
        // Сохраняем позицию пипетки
        if (::params.isInitialized) {
            try {
                val prefs = PrefsManager(this)
                prefs.pickerX = params.x
                prefs.pickerY = params.y
            } catch (e: Exception) {
                // Игнорируем ошибки при сохранении
                android.util.Log.e("ColorPickerService", "Error saving picker position", e)
            }
        }
        
        try {
            if (::pickerView.isInitialized) {
                windowManager.removeView(pickerView)
            }
        } catch (e: Exception) {
            CrashHandler.logError("ColorPickerService", "Error removing picker view", e)
        }
        
        super.onDestroy()
    }
}
