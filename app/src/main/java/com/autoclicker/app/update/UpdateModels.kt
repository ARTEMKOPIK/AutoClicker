package com.autoclicker.app.update

import com.google.gson.annotations.SerializedName

/**
 * Модель GitHub Release
 */
data class GitHubRelease(
    @SerializedName("tag_name")
    val tagName: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("body")
    val body: String?,
    
    @SerializedName("published_at")
    val publishedAt: String,
    
    @SerializedName("assets")
    val assets: List<GitHubAsset>,
    
    @SerializedName("html_url")
    val htmlUrl: String
)

data class GitHubAsset(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("size")
    val size: Long,
    
    @SerializedName("browser_download_url")
    val downloadUrl: String,
    
    @SerializedName("content_type")
    val contentType: String
)

/**
 * Информация об обновлении для UI
 */
data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val fileSize: Long,
    val changelog: String,
    val publishedAt: String,
    val releaseUrl: String
) {
    val fileSizeFormatted: String
        get() {
            val mb = fileSize / (1024.0 * 1024.0)
            return String.format("%.1f MB", mb)
        }
    
    /**
     * Парсит changelog в список изменений
     */
    fun getChangelogItems(): List<ChangelogItem> {
        if (changelog.isBlank()) return emptyList()
        
        val items = mutableListOf<ChangelogItem>()
        var currentCategory = ""
        
        changelog.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("### ") -> {
                    currentCategory = trimmed.removePrefix("### ").trim()
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val text = trimmed.removePrefix("- ").removePrefix("* ").trim()
                    if (text.isNotBlank()) {
                        items.add(ChangelogItem(
                            category = currentCategory,
                            text = text,
                            icon = getCategoryIcon(currentCategory)
                        ))
                    }
                }
            }
        }
        
        return items
    }
    
    private fun getCategoryIcon(category: String): String {
        return when {
            category.contains("Новое", ignoreCase = true) -> "✨"
            category.contains("New", ignoreCase = true) -> "✨"
            category.contains("Улучшен", ignoreCase = true) -> "🔧"
            category.contains("Improve", ignoreCase = true) -> "🔧"
            category.contains("Исправлен", ignoreCase = true) -> "🐛"
            category.contains("Fix", ignoreCase = true) -> "🐛"
            category.contains("Удален", ignoreCase = true) -> "🗑️"
            category.contains("Remove", ignoreCase = true) -> "🗑️"
            category.contains("Безопасность", ignoreCase = true) -> "🔒"
            category.contains("Security", ignoreCase = true) -> "🔒"
            else -> "📝"
        }
    }
}

data class ChangelogItem(
    val category: String,
    val text: String,
    val icon: String
)

/**
 * Состояние загрузки обновления
 */
sealed class UpdateDownloadState {
    object Idle : UpdateDownloadState()
    data class Downloading(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateDownloadState()
    data class Downloaded(val filePath: String) : UpdateDownloadState()
    data class Error(val message: String) : UpdateDownloadState()
}
