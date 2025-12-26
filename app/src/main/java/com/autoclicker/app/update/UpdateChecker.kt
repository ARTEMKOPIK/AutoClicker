package com.autoclicker.app.update

import android.app.Activity
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Хелпер для проверки и установки обновлений
 */
class UpdateChecker(private val activity: Activity) {

    private val updateManager = UpdateManager.getInstance(activity)
    private var updateDialog: UpdateDialog? = null
    private var currentUpdateInfo: UpdateInfo? = null
    private var downloadedFilePath: String? = null

    /**
     * Проверяет обновления при запуске приложения
     */
    fun checkOnStartup() {
        // Проверяем, включено ли автообновление
        val prefs = activity.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val isAutoUpdateEnabled = prefs.getBoolean("auto_update_enabled", true)
        
        if (!isAutoUpdateEnabled) {
            return // Автообновление отключено
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            val updateInfo = withContext(Dispatchers.IO) {
                updateManager.checkForUpdate(force = false)
            }
            
            updateInfo?.let { showUpdateDialog(it) }
        }
    }

    /**
     * Принудительная проверка обновлений (из настроек)
     */
    fun checkManually(onNoUpdate: () -> Unit = {}) {
        CoroutineScope(Dispatchers.Main).launch {
            val updateInfo = withContext(Dispatchers.IO) {
                updateManager.checkForUpdate(force = true)
            }
            
            if (updateInfo != null) {
                showUpdateDialog(updateInfo)
            } else {
                onNoUpdate()
            }
        }
    }

    /**
     * Показывает диалог обновления
     */
    private fun showUpdateDialog(updateInfo: UpdateInfo) {
        if (activity.isFinishing || activity.isDestroyed) return
        
        currentUpdateInfo = updateInfo
        downloadedFilePath = null

        updateDialog = UpdateDialog(
            context = activity,
            updateInfo = updateInfo,
            onUpdate = { handleUpdateClick() },
            onLater = { /* просто закрываем */ },
            onSkip = { updateManager.skipVersion(updateInfo.versionName) }
        ).apply {
            show()
        }
    }

    /**
     * Обработка нажатия кнопки обновления
     */
    private fun handleUpdateClick() {
        val info = currentUpdateInfo ?: return
        val dialog = updateDialog ?: return

        // Если уже скачано - устанавливаем
        downloadedFilePath?.let { path ->
            updateManager.installUpdate(path)
            return
        }

        // Иначе начинаем загрузку
        dialog.showDownloadProgress()

        updateManager.downloadUpdate(info) { state ->
            activity.runOnUiThread {
                when (state) {
                    is UpdateDownloadState.Downloading -> {
                        dialog.updateDownloadProgress(
                            state.progress,
                            state.downloadedBytes,
                            state.totalBytes
                        )
                    }
                    is UpdateDownloadState.Downloaded -> {
                        downloadedFilePath = state.filePath
                        dialog.showDownloadComplete()
                    }
                    is UpdateDownloadState.Error -> {
                        dialog.showDownloadError(state.message)
                    }
                    UpdateDownloadState.Idle -> {}
                }
            }
        }
    }

    /**
     * Освобождает ресурсы
     */
    fun cleanup() {
        updateDialog?.dismiss()
        updateDialog = null
        updateManager.cleanup()
    }
}
