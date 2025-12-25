package com.autoclicker.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.service.ColorPickerService
import com.autoclicker.app.service.FloatingWindowService
import com.autoclicker.app.service.ScreenCaptureService
import com.autoclicker.app.update.UpdateChecker
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : BaseActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvStatusHint: TextView
    private lateinit var statusCard: LinearLayout
    private lateinit var statusIndicator: View
    
    private lateinit var updateChecker: UpdateChecker

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { data ->
                ScreenCaptureService.startService(this, result.resultCode, data)
                updateStatus()
                Toast.makeText(this, "Захват экрана включён", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "Ошибка захвата экрана", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvStatusHint = findViewById(R.id.tvStatusHint)
        statusCard = findViewById(R.id.statusCard)
        statusIndicator = findViewById(R.id.statusIndicator)

        setupButtons()
        updateStatus()
        
        // Проверяем обновления при запуске
        updateChecker = UpdateChecker(this)
        updateChecker.checkOnStartup()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun setupButtons() {
        // Скрипты
        findViewById<LinearLayout>(R.id.btnScripts).setOnClickListener {
            try {
                startActivity(Intent(this, ScriptListActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                com.autoclicker.app.util.CrashHandler.logError("MainActivity", "Error opening ScriptListActivity", e)
            }
        }

        // Новый скрипт
        findViewById<LinearLayout>(R.id.btnNewScript).setOnClickListener {
            try {
                startActivity(Intent(this, ScriptEditorActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                com.autoclicker.app.util.CrashHandler.logError("MainActivity", "Error opening ScriptEditorActivity", e)
            }
        }

        // Визуальный редактор
        findViewById<LinearLayout>(R.id.btnVisualEditor).setOnClickListener {
            try {
                startActivity(Intent(this, com.autoclicker.app.visual.VisualEditorActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                com.autoclicker.app.util.CrashHandler.logError("MainActivity", "Error opening VisualEditorActivity", e)
            }
        }

        // Инструкция
        findViewById<LinearLayout>(R.id.btnHelp).setOnClickListener {
            showHelpDialog()
        }

        // Скриншот
        findViewById<LinearLayout>(R.id.btnScreenshot).setOnClickListener {
            if (!ScreenCaptureService.isRunning) {
                requestScreenCapture()
            } else {
                Toast.makeText(this, "Захват экрана уже активен", Toast.LENGTH_SHORT).show()
            }
        }

        // Пипетка
        findViewById<LinearLayout>(R.id.btnColorPicker).setOnClickListener {
            try {
                if (checkPermissions()) {
                    ColorPickerService.startService(this)
                    Toast.makeText(this, "Пипетка запущена", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                com.autoclicker.app.util.CrashHandler.logError("MainActivity", "Error starting ColorPickerService", e)
            }
        }

        // Telegram
        findViewById<LinearLayout>(R.id.btnTelegram).setOnClickListener {
            startActivity(Intent(this, TelegramSettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Запуск панели (FAB)
        findViewById<FloatingActionButton>(R.id.btnStartPanel).setOnClickListener {
            try {
                if (checkPermissions()) {
                    FloatingWindowService.startService(this)
                    Toast.makeText(this, "Панель запущена", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                com.autoclicker.app.util.CrashHandler.logError("MainActivity", "Error starting FloatingWindowService", e)
            }
        }

        // Домой
        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            startActivity(intent)
        }

        // Настройки
        findViewById<ImageView>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Планировщик
        findViewById<LinearLayout>(R.id.btnScheduler).setOnClickListener {
            try {
                startActivity(Intent(this, SchedulerActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                com.autoclicker.app.util.CrashHandler.logError("MainActivity", "Error opening SchedulerActivity", e)
            }
        }

        // Запись макроса
        findViewById<LinearLayout>(R.id.btnMacro).setOnClickListener {
            try {
                if (checkPermissions()) {
                    com.autoclicker.app.service.MacroRecorderService.startService(this)
                    Toast.makeText(this, "Запись макроса запущена", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                com.autoclicker.app.util.CrashHandler.logError("MainActivity", "Error starting MacroRecorderService", e)
            }
        }

        // Профили
        findViewById<LinearLayout>(R.id.btnProfiles).setOnClickListener {
            try {
                startActivity(Intent(this, ProfilesActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                com.autoclicker.app.util.CrashHandler.logError("MainActivity", "Error opening ProfilesActivity", e)
            }
        }

        // Логи
        findViewById<LinearLayout>(R.id.btnLogs)?.setOnClickListener {
            try {
                startActivity(Intent(this, LogsActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                com.autoclicker.app.util.CrashHandler.logError("MainActivity", "Error opening LogsActivity", e)
            }
        }

        // Остановить
        findViewById<LinearLayout>(R.id.btnStop).setOnClickListener {
            FloatingWindowService.stopService(this)
            ColorPickerService.stopService(this)
            ScreenCaptureService.stopService(this)
            Toast.makeText(this, "Сервисы остановлены", Toast.LENGTH_SHORT).show()
            updateStatus()
        }
        
        // Status card click - go to settings
        statusCard.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("Инструкция")
            .setMessage("""
                📱 ОСНОВНЫЕ ДЕЙСТВИЯ:
                • click(x, y) — клик
                • longClick(x, y) — долгий клик
                • tap(x, y, count) — множественный тап
                • swipe(x1, y1, x2, y2) — свайп
                • back() / home() / recents() — системные кнопки
                
                ⏱ ОЖИДАНИЕ:
                • sleep(ms) — задержка
                • waitForColor(x, y, color, timeout) — ждать цвет
                • waitForText(x1,y1,x2,y2, "text", timeout) — ждать текст
                
                📊 ДАННЫЕ:
                • getColor(x, y) — цвет пикселя
                • getText(x1,y1,x2,y2) — OCR текста
                • compareColor(x, y, color) — сравнить цвет
                • random(min, max) — случайное число
                
                💾 ГЛОБАЛЬНЫЕ ПЕРЕМЕННЫЕ:
                • setVar("key", value) — сохранить
                • getVar("key") — получить
                • incVar("key") / decVar("key") — +1/-1
                
                📤 ВЫВОД:
                • log("text") — в лог
                • toast("text") — уведомление
                • sendTelegram("text") — в Telegram
                • vibrate(ms) — вибрация
                
                🔧 УПРАВЛЕНИЕ:
                • while (!EXIT) { } — цикл
                • if (condition) { } — условие
                • fun name() { } — функция
                • EXIT = true — остановить
                
                💡 Тап по названию скрипта = мини-режим
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun requestScreenCapture() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun checkPermissions(): Boolean {
        // Check accessibility first - this is critical for script execution
        if (!isAccessibilityEnabled()) {
            showPermissionDialog("Accessibility Service", "Нужен для кликов по экрану") {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            return false
        }
        
        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            showPermissionDialog("Наложение поверх окон", "Нужно для плавающей кнопки") {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
            return false
        }
        
        // Re-check screen capture after showing dialog (may have changed during user interaction)
        // This prevents race condition where permissions are granted but state isn't updated yet
        val captureService = ScreenCaptureService.isRunning
        if (!captureService) {
            // Final atomic check before requesting
            if (ScreenCaptureService.isRunning) {
                return true // Permission was granted between check and now
            }
            showPermissionDialog("Захват экрана", "Нужен для скриншотов и OCR") {
                requestScreenCapture()
            }
            return false
        }
        
        // All permissions granted atomically
        return true
    }

    private fun showPermissionDialog(title: String, message: String, action: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Включить") { _, _ -> action() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any { it.id.contains(packageName) }
    }

    private fun updateStatus() {
        val accessibility = isAccessibilityEnabled()
        val overlay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)
        val capture = ScreenCaptureService.isRunning

        val allReady = accessibility && overlay && capture
        
        if (allReady) {
            tvStatus.text = "Готов к работе"
            tvStatusHint.text = "Все разрешения включены"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
            statusCard.background = ContextCompat.getDrawable(this, R.drawable.bg_status_banner)
            (statusIndicator.background as? GradientDrawable)?.setColor(ContextCompat.getColor(this, R.color.success))
        } else {
            val missing = mutableListOf<String>()
            if (!accessibility) missing.add("Accessibility")
            if (!overlay) missing.add("Overlay")
            if (!capture) missing.add("Capture")
            
            tvStatus.text = "Требуется настройка"
            tvStatusHint.text = "Включите: ${missing.joinToString(", ")}"
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.warning))
            statusCard.background = ContextCompat.getDrawable(this, R.drawable.bg_status_warning)
            (statusIndicator.background as? GradientDrawable)?.setColor(ContextCompat.getColor(this, R.color.warning))
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::updateChecker.isInitialized) {
            updateChecker.cleanup()
        }
    }
}
