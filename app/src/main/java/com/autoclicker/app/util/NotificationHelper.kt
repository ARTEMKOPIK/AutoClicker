package com.autoclicker.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Утилита для создания и управления уведомлениями
 * Упрощает работу с Android Notification API
 */
object NotificationHelper {
    
    private const val CHANNEL_ID_DEFAULT = "autoclicker_default"
    private const val CHANNEL_ID_SCRIPT = "autoclicker_script"
    private const val CHANNEL_ID_ERROR = "autoclicker_error"
    
    private var notificationId = 1000
    
    /**
     * Инициализация каналов уведомлений
     */
    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Основной канал
            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                "Общие уведомления",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Основные уведомления приложения"
            }
            
            // Канал для скриптов
            val scriptChannel = NotificationChannel(
                CHANNEL_ID_SCRIPT,
                "Выполнение скриптов",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о работе скриптов"
            }
            
            // Канал для ошибок
            val errorChannel = NotificationChannel(
                CHANNEL_ID_ERROR,
                "Ошибки",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Критические ошибки и предупреждения"
            }
            
            notificationManager.createNotificationChannel(defaultChannel)
            notificationManager.createNotificationChannel(scriptChannel)
            notificationManager.createNotificationChannel(errorChannel)
        }
    }
    
    /**
     * Показать простое уведомление
     */
    fun showNotification(
        context: Context,
        title: String,
        message: String,
        icon: Int = android.R.drawable.ic_dialog_info,
        autoCancel: Boolean = true
    ): Int {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_DEFAULT)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(autoCancel)
        
        val id = notificationId++
        NotificationManagerCompat.from(context).notify(id, notificationBuilder.build())
        return id
    }
    
    /**
     * Показать уведомление о запуске скрипта
     */
    fun showScriptStarted(
        context: Context,
        scriptName: String,
        icon: Int = android.R.drawable.ic_media_play
    ): Int {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_SCRIPT)
            .setSmallIcon(icon)
            .setContentTitle("Скрипт запущен")
            .setContentText(scriptName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
        
        val id = notificationId++
        NotificationManagerCompat.from(context).notify(id, notificationBuilder.build())
        return id
    }
    
    /**
     * Показать уведомление о завершении скрипта
     */
    fun showScriptCompleted(
        context: Context,
        scriptName: String,
        duration: Long,
        icon: Int = android.R.drawable.ic_media_pause
    ): Int {
        val durationText = formatDuration(duration)
        
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_SCRIPT)
            .setSmallIcon(icon)
            .setContentTitle("Скрипт завершен")
            .setContentText("$scriptName • $durationText")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
        
        val id = notificationId++
        NotificationManagerCompat.from(context).notify(id, notificationBuilder.build())
        return id
    }
    
    /**
     * Показать уведомление об ошибке
     */
    fun showError(
        context: Context,
        title: String,
        message: String,
        icon: Int = android.R.drawable.ic_dialog_alert
    ): Int {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_ERROR)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        
        val id = notificationId++
        NotificationManagerCompat.from(context).notify(id, notificationBuilder.build())
        return id
    }
    
    /**
     * Показать прогресс уведомление
     */
    fun showProgress(
        context: Context,
        title: String,
        message: String,
        progress: Int,
        max: Int = 100,
        icon: Int = android.R.drawable.ic_popup_sync
    ): Int {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_DEFAULT)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(max, progress, false)
            .setOngoing(true)
        
        val id = notificationId++
        NotificationManagerCompat.from(context).notify(id, notificationBuilder.build())
        return id
    }
    
    /**
     * Обновить прогресс уведомление
     */
    fun updateProgress(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        progress: Int,
        max: Int = 100,
        icon: Int = android.R.drawable.ic_popup_sync
    ) {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_DEFAULT)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(max, progress, false)
            .setOngoing(true)
        
        NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
    }
    
    /**
     * Показать уведомление с действиями
     */
    fun showWithActions(
        context: Context,
        title: String,
        message: String,
        actions: List<NotificationAction>,
        icon: Int = android.R.drawable.ic_dialog_info
    ): Int {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_DEFAULT)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        
        actions.forEach { action ->
            notificationBuilder.addAction(
                action.icon,
                action.title,
                action.pendingIntent
            )
        }
        
        val id = notificationId++
        NotificationManagerCompat.from(context).notify(id, notificationBuilder.build())
        return id
    }
    
    /**
     * Отменить уведомление
     */
    fun cancel(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
    
    /**
     * Отменить все уведомления
     */
    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
    
    /**
     * Форматировать длительность в читаемый вид
     */
    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}ч ${minutes % 60}м"
            minutes > 0 -> "${minutes}м ${seconds % 60}с"
            else -> "${seconds}с"
        }
    }
    
    /**
     * Класс для действия уведомления
     */
    data class NotificationAction(
        val icon: Int,
        val title: String,
        val pendingIntent: PendingIntent
    )
}

