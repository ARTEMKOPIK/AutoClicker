package com.autoclicker.app.update

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.autoclicker.app.MainActivity
import com.autoclicker.app.R

/**
 * Помощник для создания и управления уведомлениями обновлений
 */
object UpdateNotificationHelper {
    
    private const val CHANNEL_ID = "update_downloads"
    private const val CHANNEL_NAME = "Обновления приложения"
    private const val CHANNEL_DESCRIPTION = "Уведомления о загрузке и установке обновлений"
    
    const val NOTIFICATION_ID = 1001
    
    /**
     * Создаёт notification channel (требуется для Android O+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Создаёт foreground notification для Service
     */
    fun createForegroundNotification(
        context: Context,
        title: String = "Загрузка обновления",
        text: String = "Подготовка...",
        progress: Int = 0,
        indeterminate: Boolean = true
    ): Notification {
        createNotificationChannel(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, indeterminate)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }
    
    /**
     * Создаёт notification с прогрессом загрузки
     */
    fun createProgressNotification(
        context: Context,
        versionName: String,
        progress: Int,
        downloaded: Long,
        total: Long
    ): Notification {
        val downloadedMB = downloaded / (1024.0 * 1024.0)
        val totalMB = total / (1024.0 * 1024.0)
        val text = String.format("%.1f МБ из %.1f МБ", downloadedMB, totalMB)
        
        return createForegroundNotification(
            context,
            title = "Загрузка AutoClicker $versionName",
            text = text,
            progress = progress,
            indeterminate = false
        )
    }
    
    /**
     * Создаёт notification о завершении загрузки
     */
    fun createCompletedNotification(
        context: Context,
        versionName: String,
        apkUri: String
    ): Notification {
        createNotificationChannel(context)
        
        val installIntent = createInstallIntent(context, apkUri)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Обновление загружено")
            .setContentText("Нажмите для установки версии $versionName")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }
    
    /**
     * Создаёт notification об ошибке
     */
    fun createErrorNotification(
        context: Context,
        errorMessage: String
    ): Notification {
        createNotificationChannel(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Ошибка загрузки обновления")
            .setContentText(errorMessage)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()
    }
    
    /**
     * Обновляет существующее notification
     */
    fun updateNotification(context: Context, notification: Notification) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * Отменяет notification
     */
    fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
    
    /**
     * Создаёт Intent для установки APK
     */
    private fun createInstallIntent(context: Context, apkUri: String): Intent {
        // Этот Intent будет создан в UpdateDownloadService с правильным URI
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("install_apk", apkUri)
        }
    }
}

