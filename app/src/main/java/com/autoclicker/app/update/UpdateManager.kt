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
        
        // Проверять обновления не чаще раза в час
        private const val CHECK_INTERVAL_MS = 60 * 60 * 1000L
        
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
            // Проверяем интервал
            if (!force) {
                val lastCheck = prefs.getLong(KEY_LAST_CHECK, 0)
                if (System.currentTimeMillis() - lastCheck < CHECK_INTERVAL_MS) {
                    return@withContext null
                }
            }
            
            val request = Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github.v3+json")
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
     * Скачивает обновление
     */
    fun downloadUpdate(updateInfo: UpdateInfo, listener: (UpdateDownloadState) -> Unit) {
        downloadStateListener = listener
        listener(UpdateDownloadState.Idle)
        
        try {
            // Удаляем старые APK
            cleanOldApks()
            
            val fileName = "AutoClicker-${updateInfo.versionName}.apk"
            val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
                .setTitle("Обновление AutoClicker")
                .setDescription("Загрузка версии ${updateInfo.versionName}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            val downloadId = downloadManager.enqueue(request)
            prefs.edit().putLong(KEY_DOWNLOAD_ID, downloadId).apply()
            
            // Регистрируем receiver для отслеживания завершения
            registerDownloadReceiver(downloadId, updateInfo.versionName)
            
            // Запускаем отслеживание прогресса
            startProgressTracking(downloadId, updateInfo.fileSize)
            
        } catch (e: Exception) {
            listener(UpdateDownloadState.Error("Ошибка загрузки: ${e.message}"))
        }
    }
    
    private fun registerDownloadReceiver(downloadId: Long, versionName: String) {
        downloadReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
        }
        
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return
                if (id != downloadId) return
                
                progressJob?.cancel()
                
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            downloadStateListener?.invoke(UpdateDownloadState.Downloaded(uri))
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            downloadStateListener?.invoke(UpdateDownloadState.Error("Ошибка загрузки: $reason"))
                        }
                    }
                }
                cursor.close()
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(downloadReceiver, filter)
        }
    }
    
    private fun startProgressTracking(downloadId: Long, totalSize: Long) {
        progressJob?.cancel()
        progressJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    
                    if (status == DownloadManager.STATUS_RUNNING) {
                        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val actualTotal = if (total > 0) total else totalSize
                        val progress = if (actualTotal > 0) ((downloaded * 100) / actualTotal).toInt() else 0
                        
                        withContext(Dispatchers.Main) {
                            downloadStateListener?.invoke(UpdateDownloadState.Downloading(progress, downloaded, actualTotal))
                        }
                    }
                }
                cursor.close()
                
                delay(200)
            }
        }
    }
    
    /**
     * Устанавливает скачанное обновление
     */
    fun installUpdate(filePath: String) {
        try {
            val file = if (filePath.startsWith("file://")) {
                File(Uri.parse(filePath).path!!)
            } else {
                File(filePath)
            }
            
            if (!file.exists()) {
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
