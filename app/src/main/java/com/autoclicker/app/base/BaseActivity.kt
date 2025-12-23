package com.autoclicker.app.base

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.autoclicker.app.R
import com.autoclicker.app.util.CrashHandler

/**
 * Базовый класс Activity с анимациями переходов и обработкой ошибок
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Устанавливаем обработчик ошибок для UI потока этой Activity
        Thread.currentThread().setUncaughtExceptionHandler { thread, throwable ->
            CrashHandler.logCritical(
                this::class.java.simpleName,
                "Критическая ошибка в Activity: ${throwable.message}",
                throwable
            )
            // Передаём дальше стандартному обработчику
            CrashHandler.getInstance()?.uncaughtException(thread, throwable)
        }
        
        super.onCreate(savedInstanceState)
    }

    override fun startActivity(intent: Intent?) {
        try {
            super.startActivity(intent)
            applyEnterTransition()
        } catch (e: Exception) {
            CrashHandler.logError(this::class.java.simpleName, "Ошибка запуска Activity", e)
            throw e
        }
    }

    override fun finish() {
        try {
            super.finish()
            applyExitTransition()
        } catch (e: Exception) {
            CrashHandler.logError(this::class.java.simpleName, "Ошибка закрытия Activity", e)
        }
    }

    private fun applyEnterTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun applyExitTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
    
    /**
     * Безопасное выполнение кода с отправкой ошибок в Telegram
     */
    protected fun safeExecute(tag: String = this::class.java.simpleName, action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            CrashHandler.logError(tag, "Ошибка выполнения: ${e.message}", e)
        }
    }
}
