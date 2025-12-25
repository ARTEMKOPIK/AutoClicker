package com.autoclicker.app.update

import android.app.DownloadManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import java.io.File

/**
 * Foreground Service для надёжной загрузки обновлений
 * Гарантирует, что загрузка не будет прервана системой
 */
class UpdateDownloadService : Service() {
    
    companion object {
        private const val EXTRA_VERSION_NAME = "version_name"
        private const val EXTRA_DOWNLOAD_URL = "download_url"
        private const val EXTRA_FILE_SIZE = "file_size"
        
        const val ACTION_START_DOWNLOAD = "com.autoclicker.app.START_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.autoclicker.app.CANCEL_DOWNLOAD"
        
        // Broadcasts для отправки состояния
        const val BROADCAST_DOWNLOAD_PROGRESS = "com.autoclicker.app.DOWNLOAD_PROGRESS"
        const val BROADCAST_DOWNLOAD_COMPLETED = "com.autoclicker.app.DOWNLOAD_COMPLETED"
        const val BROADCAST_DOWNLOAD_FAILED = "com.autoclicker.app.DOWNLOAD_FAILED"
        
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_DOWNLOADED = "downloaded"
        const val EXTRA_TOTAL = "total"
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_ERROR = "error"
        
        /**
         * Запускает загрузку обновления
         */
        fun startDownload(context: Context, updateInfo: UpdateInfo) {
            val intent = Intent(context, UpdateDownloadService::class.java).apply {
                action = ACTION_START_DOWNLOAD
                putExtra(EXTRA_VERSION_NAME, updateInfo.versionName)
                putExtra(EXTRA_DOWNLOAD_URL, updateInfo.downloadUrl)
                putExtra(EXTRA_FILE_SIZE, updateInfo.fileSize)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * Отменяет загрузку
         */
        fun cancelDownload(context: Context) {
            val intent = Intent(context, UpdateDownloadService::class.java).apply {
                action = ACTION_CANCEL_DOWNLOAD
            }
            context.startService(intent)
        }
    }
    
    private val downloadManager by lazy { getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
    private val prefs by lazy { getSharedPreferences("update_prefs", Context.MODE_PRIVATE) }
    
    private var downloadId: Long = -1
    private var versionName: String = ""
    private var totalSize: Long = 0
    
    private var downloadReceiver: BroadcastReceiver? = null
    private var progressJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        // Создаём notification channel
        UpdateNotificationHelper.createNotificationChannel(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_DOWNLOAD -> {
                versionName = intent.getStringExtra(EXTRA_VERSION_NAME) ?: ""
                val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL) ?: ""
                totalSize = intent.getLongExtra(EXTRA_FILE_SIZE, 0)
                
                startForeground(
                    UpdateNotificationHelper.NOTIFICATION_ID,
                    UpdateNotificationHelper.createForegroundNotification(
                        this,
                        "Загрузка AutoClicker $versionName",
                        "Начало загрузки...",
                        0,
                        true
                    )
                )
                
                startDownload(downloadUrl)
            }
            
            ACTION_CANCEL_DOWNLOAD -> {
                cancelDownload()
                stopSelfAndCleanup()
            }
        }
        
        // START_STICKY гарантирует перезапуск Service при завершении системой
        return START_STICKY
    }
    
    private fun startDownload(downloadUrl: String) {
        try {
            // Удаляем старые APK
            cleanOldApks()
            
            val fileName = "AutoClicker-${versionName}.apk"
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("Обновление AutoClicker")
                .setDescription("Загрузка версии $versionName")
                // КРИТИЧЕСКОЕ ИЗМЕНЕНИЕ: используем VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                // вместо VISIBILITY_VISIBLE для надёжности
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(this, android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setRequiresCharging(false)
            
            downloadId = downloadManager.enqueue(request)
            prefs.edit().putLong("download_id", downloadId).apply()
            
            // Регистрируем receiver для завершения загрузки
            registerDownloadReceiver()
            
            // Запускаем отслеживание прогресса в Service scope
            startProgressTracking()
            
        } catch (e: Exception) {
            onDownloadFailed("Ошибка начала загрузки: ${e.message}")
        }
    }
    
    private fun registerDownloadReceiver() {
        downloadReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
                if (id != downloadId) return
                
                // Останавливаем отслеживание прогресса
                progressJob?.cancel()
                
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            onDownloadCompleted(uri)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            val errorMsg = getDownloadErrorMessage(reason)
                            onDownloadFailed(errorMsg)
                        }
                    }
                }
                cursor.close()
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(downloadReceiver, filter)
        }
    }
    
    private fun startProgressTracking() {
        progressJob?.cancel()
        
        // Используем Service scope для защиты от завершения
        progressJob = serviceScope.launch {
            while (isActive && downloadId != -1L) {
                try {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        
                        when (status) {
                            DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PENDING -> {
                                val downloaded = cursor.getLong(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                )
                                val total = cursor.getLong(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                )
                                val actualTotal = if (total > 0) total else totalSize
                                val progress = if (actualTotal > 0) {
                                    ((downloaded * 100) / actualTotal).toInt()
                                } else 0
                                
                                // Обновляем notification
                                withContext(Dispatchers.Main) {
                                    val notification = UpdateNotificationHelper.createProgressNotification(
                                        this@UpdateDownloadService,
                                        versionName,
                                        progress,
                                        downloaded,
                                        actualTotal
                                    )
                                    UpdateNotificationHelper.updateNotification(
                                        this@UpdateDownloadService,
                                        notification
                                    )
                                    
                                    // Отправляем broadcast для UI
                                    sendProgressBroadcast(progress, downloaded, actualTotal)
                                }
                            }
                            DownloadManager.STATUS_PAUSED -> {
                                // Загрузка приостановлена
                                withContext(Dispatchers.Main) {
                                    val notification = UpdateNotificationHelper.createForegroundNotification(
                                        this@UpdateDownloadService,
                                        "Загрузка приостановлена",
                                        "Ожидание подключения...",
                                        0,
                                        true
                                    )
                                    UpdateNotificationHelper.updateNotification(
                                        this@UpdateDownloadService,
                                        notification
                                    )
                                }
                            }
                        }
                    }
                    cursor.close()
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                // Обновляем каждые 500 мс для плавного прогресса
                delay(500)
            }
        }
    }
    
    private fun onDownloadCompleted(fileUri: String) {
        // Показываем notification о завершении
        val notification = UpdateNotificationHelper.createCompletedNotification(
            this,
            versionName,
            fileUri
        )
        UpdateNotificationHelper.updateNotification(this, notification)
        
        // Отправляем broadcast
        sendCompletedBroadcast(fileUri)
        
        // Запускаем установку
        installUpdate(fileUri)
        
        // Останавливаем Service
        stopSelfAndCleanup()
    }
    
    private fun onDownloadFailed(error: String) {
        // Показываем notification об ошибке
        val notification = UpdateNotificationHelper.createErrorNotification(this, error)
        UpdateNotificationHelper.updateNotification(this, notification)
        
        // Отправляем broadcast
        sendFailedBroadcast(error)
        
        // Останавливаем Service
        stopSelfAndCleanup()
    }
    
    private fun installUpdate(filePath: String) {
        try {
            val file = if (filePath.startsWith("file://")) {
                val uri = Uri.parse(filePath)
                val path = uri.path
                if (path == null) {
                    com.autoclicker.app.util.CrashHandler.logError(
                        "UpdateDownloadService",
                        "Failed to parse file path from URI: $filePath",
                        null
                    )
                    showNotification("Ошибка", "Не удалось получить путь к файлу обновления", false)
                    return
                }
                File(path)
            } else {
                File(filePath)
            }
            
            if (!file.exists()) {
                com.autoclicker.app.util.CrashHandler.logWarning(
                    "UpdateDownloadService",
                    "Update file does not exist: ${file.absolutePath}"
                )
                return
            }
            
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            startActivity(intent)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun cancelDownload() {
        if (downloadId != -1L) {
            downloadManager.remove(downloadId)
            downloadId = -1
        }
    }
    
    private fun cleanOldApks() {
        try {
            val downloadsDir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            downloadsDir?.listFiles()?.forEach { file ->
                if (file.name.startsWith("AutoClicker-") && file.name.endsWith(".apk")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
    
    private fun getDownloadErrorMessage(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Невозможно возобновить загрузку"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Нет доступного хранилища"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "Файл уже существует"
            DownloadManager.ERROR_FILE_ERROR -> "Ошибка файловой системы"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "Ошибка HTTP данных"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Недостаточно места"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Слишком много перенаправлений"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Неизвестный HTTP код"
            else -> "Ошибка загрузки (код: $reason)"
        }
    }
    
    private fun sendProgressBroadcast(progress: Int, downloaded: Long, total: Long) {
        val intent = Intent(BROADCAST_DOWNLOAD_PROGRESS).apply {
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_DOWNLOADED, downloaded)
            putExtra(EXTRA_TOTAL, total)
        }
        sendBroadcast(intent)
    }
    
    private fun sendCompletedBroadcast(filePath: String) {
        val intent = Intent(BROADCAST_DOWNLOAD_COMPLETED).apply {
            putExtra(EXTRA_FILE_PATH, filePath)
        }
        sendBroadcast(intent)
    }
    
    private fun sendFailedBroadcast(error: String) {
        val intent = Intent(BROADCAST_DOWNLOAD_FAILED).apply {
            putExtra(EXTRA_ERROR, error)
        }
        sendBroadcast(intent)
    }
    
    private fun stopSelfAndCleanup() {
        progressJob?.cancel()
        downloadReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        stopForeground(true)
        stopSelf()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopSelfAndCleanup()
    }
}
