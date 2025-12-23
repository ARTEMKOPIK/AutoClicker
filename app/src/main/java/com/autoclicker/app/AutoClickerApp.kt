package com.autoclicker.app

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Process
import android.os.StrictMode
import com.autoclicker.app.util.CrashHandler
import java.io.File

class AutoClickerApp : Application() {
    
    companion object {
        @Volatile
        private var instance: AutoClickerApp? = null
        
        fun getInstance(): AutoClickerApp? = instance
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Инициализируем обработчик крашей ПЕРВЫМ
        CrashHandler.init(this)
        
        // Регистрируем обработчик для всех потоков
        setupGlobalExceptionHandlers()
        
        // Проверяем предыдущие краши при запуске
        checkPreviousCrash()
        
        // Отправляем информацию о запуске приложения
        CrashHandler.logInfo("AutoClickerApp", "🚀 Приложение запущено")
    }
    
    private fun setupGlobalExceptionHandlers() {
        // Обработчик для главного потока уже установлен в CrashHandler.init()
        
        // Дополнительный обработчик для ANR и других проблем
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            CrashHandler.getInstance()?.let { handler ->
                // Вызываем стандартный обработчик
                handler.uncaughtException(thread, throwable)
            } ?: run {
                // Fallback если CrashHandler не инициализирован
                throwable.printStackTrace()
            }
        }
    }
    
    private fun checkPreviousCrash() {
        try {
            val crashFile = getFileStreamPath("crash_log.txt")
            if (crashFile.exists() && crashFile.length() > 0) {
                // Отправляем информацию о предыдущем краше
                val lastModified = crashFile.lastModified()
                val timeSinceCrash = System.currentTimeMillis() - lastModified
                
                // Если краш был менее 5 минут назад, отправляем уведомление
                if (timeSinceCrash < 5 * 60 * 1000) {
                    CrashHandler.logWarning(
                        "AutoClickerApp",
                        "⚠️ Обнаружен предыдущий краш (${timeSinceCrash / 1000} сек назад)"
                    )
                }
            }
        } catch (e: Exception) {
            // Игнорируем ошибки при проверке
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        CrashHandler.logWarning("AutoClickerApp", "⚠️ Мало памяти! Возможны проблемы")
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                CrashHandler.logWarning("AutoClickerApp", "🔴 Критически мало памяти!")
            }
            TRIM_MEMORY_RUNNING_LOW -> {
                CrashHandler.logWarning("AutoClickerApp", "🟠 Мало памяти")
            }
        }
    }
}
