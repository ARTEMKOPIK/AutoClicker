package com.autoclicker.app.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.autoclicker.app.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Менеджер автообновления приложения через GitHub Releases
 */
class UpdateManager(private val context: Context) {
    
    companion object {
        private const val GITHUB_OWNER = "ARTEMKOPIK"
        private const val GITHUB_REPO = "AutoClicker"
        
        private const val API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        private const val PREFS_NAME = "update_prefs"
        private const val KEY_LAST_CHECK = "last_check_time"
        private const val KEY_SKIPPED_VERSION = "skipped_version"
        private const val KEY_DOWNLOAD_ID = "download_id"
        private const val KEY_APP_LAUNCHES = "app_launches"
        
        // Проверять обновления не чаще раза в 6 часов
        private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
        // Первые 3 запуска всегда проверяем обновления
        private const val ALWAYS_CHECK_FIRST_LAUNCHES = 3
        
        @Volatile
        private var instance: UpdateManager? = null
        
        fun getInstance(context: Context): UpdateManager {
            return instance ?: synchronized(this) {
                instance ?: UpdateManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    
    private var downloadStateListener: ((UpdateDownloadState) -> Unit)? = null
    private var downloadReceiver: BroadcastReceiver? = null
    private var progressJob: Job? = null
    
    /**
     * Проверяет наличие обновлений
     * @param force - игнорировать интервал проверки
     * @return UpdateInfo если есть новая версия, null если нет
     */
    suspend fun checkForUpdate(force: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            // Увеличиваем счётчик запусков
            val launches = prefs.getInt(KEY_APP_LAUNCHES, 0) + 1
            prefs.edit().putInt(KEY_APP_LAUNCHES, launches).apply()
            
            // Проверяем интервал (но первые N запусков всегда проверяем)
            if (!force && launches > ALWAYS_CHECK_FIRST_LAUNCHES) {
                val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
                if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) {
                    return@withContext null
                }
            }
            
            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("User-Agent", "AutoClicker-App/${BuildConfig.VERSION_NAME}")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext null
            }
            
            val body = response.body?.string() ?: return@withContext null
            val release = gson.fromJson(body, GitHubRelease::class.java)
            
            // Сохраняем время проверки
            prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()
            
            // Сравниваем версии
            val latestVersion = release.tagName.removePrefix("v")
            val currentVersion = BuildConfig.VERSION_NAME
            
            if (!isNewerVersion(latestVersion, currentVersion)) {
                return@withContext null
            }
            
            // Проверяем, не пропустил ли пользователь эту версию
            val skippedVersion = prefs.getString(KEY_SKIPPED_VERSION, null)
            if (skippedVersion == latestVersion && !force) {
                return@withContext null
            }
            
            // Ищем APK в assets
            val apkAsset = release.assets.find { 
                it.name.endsWith(".apk") && it.contentType == "application/vnd.android.package-archive"
            } ?: release.assets.find { it.name.endsWith(".apk") }
            
            if (apkAsset == null) {
                return@withContext null
            }
            
            return@withContext UpdateInfo(
                versionName = latestVersion,
                versionCode = parseVersionCode(latestVersion),
                downloadUrl = apkAsset.downloadUrl,
                fileSize = apkAsset.size,
                changelog = release.body ?: "",
                publishedAt = release.publishedAt,
                releaseUrl = release.htmlUrl
            )
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
    
    /**
     * Сравнивает версии (поддерживает формат X.Y.Z)
     */
    private fun isNewerVersion(latest: String, current: String): Boolean {
        try {
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            
            val maxLength = maxOf(latestParts.size, currentParts.size)
            
            for (i in 0 until maxLength) {
                val latestPart = latestParts.getOrElse(i) { 0 }
                val currentPart = currentParts.getOrElse(i) { 0 }
                
                if (latestPart > currentPart) return true
                if (latestPart < currentPart) return false
            }
            
            return false
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun parseVersionCode(version: String): Int {
        return try {
            version.replace(".", "").toInt()
        } catch (e: Exception) {
            1
        }
    }
    
    /**
     * Скачивает обновление через Foreground Service для надёжности
     */
    fun downloadUpdate(updateInfo: UpdateInfo, listener: (UpdateDownloadState) -> Unit) {
        downloadStateListener = listener
        
        try {
            // Регистрируем receiver для получения обновлений от Service
            registerServiceReceiver()
            
            // Запускаем Foreground Service для загрузки
            UpdateDownloadService.startDownload(context, updateInfo)
            
            // Сообщаем, что загрузка начата
            listener(UpdateDownloadState.Idle)
            
        } catch (e: Exception) {
            listener(UpdateDownloadState.Error("Ошибка запуска загрузки: ${e.message}"))
        }
    }
    
    /**
     * Регистрирует BroadcastReceiver для получения обновлений от Service
     */
    private fun registerServiceReceiver() {
        downloadReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
        }
        
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    UpdateDownloadService.BROADCAST_DOWNLOAD_PROGRESS -> {
                        val progress = intent.getIntExtra(UpdateDownloadService.EXTRA_PROGRESS, 0)
                        val downloaded = intent.getLongExtra(UpdateDownloadService.EXTRA_DOWNLOADED, 0)
                        val total = intent.getLongExtra(UpdateDownloadService.EXTRA_TOTAL, 0)
                        downloadStateListener?.invoke(
                            UpdateDownloadState.Downloading(progress, downloaded, total)
                        )
                    }
                    
                    UpdateDownloadService.BROADCAST_DOWNLOAD_COMPLETED -> {
                        val filePath = intent.getStringExtra(UpdateDownloadService.EXTRA_FILE_PATH) ?: ""
                        downloadStateListener?.invoke(UpdateDownloadState.Downloaded(filePath))
                        // Можно автоматически не отменять receiver, если нужно отслеживать повторные загрузки
                    }
                    
                    UpdateDownloadService.BROADCAST_DOWNLOAD_FAILED -> {
                        val error = intent.getStringExtra(UpdateDownloadService.EXTRA_ERROR) ?: "Неизвестная ошибка"
                        downloadStateListener?.invoke(UpdateDownloadState.Error(error))
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(UpdateDownloadService.BROADCAST_DOWNLOAD_PROGRESS)
            addAction(UpdateDownloadService.BROADCAST_DOWNLOAD_COMPLETED)
            addAction(UpdateDownloadService.BROADCAST_DOWNLOAD_FAILED)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(downloadReceiver, filter)
        }
    }
    
    /**
     * Отменяет загрузку
     */
    fun cancelDownload() {
        UpdateDownloadService.cancelDownload(context)
    }
    
    /**
     * Устанавливает скачанное обновление
     */
    fun installUpdate(filePath: String) {
        try {
            val file = if (filePath.startsWith("file://")) {
                val uri = Uri.parse(filePath)
                val path = uri.path
                if (path == null) {
                    com.autoclicker.app.util.CrashHandler.logError(
                        "UpdateManager",
                        "Failed to parse file path from URI: $filePath",
                        null
                    )
                    downloadStateListener?.invoke(UpdateDownloadState.Error("Не удалось получить путь к файлу обновления"))
                    return
                }
                File(path)
            } else {
                File(filePath)
            }
            
            if (!file.exists()) {
                com.autoclicker.app.util.CrashHandler.logWarning(
                    "UpdateManager",
                    "Update file does not exist: ${file.absolutePath}"
                )
                downloadStateListener?.invoke(UpdateDownloadState.Error("Файл не найден"))
                return
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            context.startActivity(intent)
            
        } catch (e: Exception) {
            downloadStateListener?.invoke(UpdateDownloadState.Error("Ошибка установки: ${e.message}"))
        }
    }
    
    /**
     * Пропустить эту версию
     */
    fun skipVersion(version: String) {
        prefs.edit().putString(KEY_SKIPPED_VERSION, version).apply()
    }
    
    /**
     * Сбросить пропущенную версию
     */
    fun resetSkippedVersion() {
        prefs.edit().remove(KEY_SKIPPED_VERSION).apply()
    }
    
    /**
     * Удаляет старые скачанные APK
     */
    private fun cleanOldApks() {
        try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir?.listFiles()?.forEach { file ->
                if (file.name.startsWith("AutoClicker-") && file.name.endsWith(".apk")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
    
    /**
     * Освобождает ресурсы
     */
    fun cleanup() {
        progressJob?.cancel()
        downloadReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
        }
        downloadStateListener = null
    }
}
