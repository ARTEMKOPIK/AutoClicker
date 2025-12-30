package com.autoclicker.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Утилита для тактильной обратной связи (вибрации)
 * Улучшает пользовательский опыт при взаимодействии с приложением
 */
object HapticFeedback {
    
    private var vibrator: Vibrator? = null
    private var isEnabled = true
    
    fun init(context: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    /**
     * Легкая вибрация (клик по кнопке)
     */
    fun light(view: View? = null) {
        if (!isEnabled) return
        
        view?.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
        
        vibrate(10)
    }
    
    /**
     * Средняя вибрация (успешное действие)
     */
    fun medium(view: View? = null) {
        if (!isEnabled) return
        
        view?.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
        
        vibrate(20)
    }
    
    /**
     * Сильная вибрация (важное событие)
     */
    fun heavy(view: View? = null) {
        if (!isEnabled) return
        
        view?.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
        
        vibrate(50)
    }
    
    /**
     * Вибрация успеха (двойная короткая)
     */
    fun success() {
        if (!isEnabled) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 50, 50, 50)
            val amplitudes = intArrayOf(0, 100, 0, 100)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 50, 50), -1)
        }
    }
    
    /**
     * Вибрация ошибки (длинная одиночная)
     */
    fun error() {
        if (!isEnabled) return
        vibrate(100)
    }
    
    /**
     * Вибрация предупреждения (три короткие)
     */
    fun warning() {
        if (!isEnabled) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 30, 50, 30, 50, 30)
            val amplitudes = intArrayOf(0, 100, 0, 100, 0, 100)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 30, 50, 30, 50, 30), -1)
        }
    }
    
    /**
     * Базовая вибрация с заданной длительностью
     */
    private fun vibrate(duration: Long) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(duration)
            }
        }
    }
    
    /**
     * Паттерн вибрации для запуска скрипта
     */
    fun scriptStart() {
        if (!isEnabled) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 30, 100, 50)
            val amplitudes = intArrayOf(0, 150, 0, 200)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 30, 100, 50), -1)
        }
    }
    
    /**
     * Паттерн вибрации для остановки скрипта
     */
    fun scriptStop() {
        if (!isEnabled) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 50, 50, 30)
            val amplitudes = intArrayOf(0, 200, 0, 100)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 50, 30), -1)
        }
    }
}

