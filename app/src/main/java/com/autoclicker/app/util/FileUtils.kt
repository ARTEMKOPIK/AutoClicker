package com.autoclicker.app.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Утилита для работы с файлами
 * Упрощает операции чтения, записи и управления файлами
 */
object FileUtils {
    
    /**
     * Прочитать текст из файла
     */
    fun readText(file: File): String? {
        return try {
            file.readText()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Записать текст в файл
     */
    fun writeText(file: File, text: String): Boolean {
        return try {
            file.writeText(text)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Добавить текст в конец файла
     */
    fun appendText(file: File, text: String): Boolean {
        return try {
            file.appendText(text)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Прочитать строки из файла
     */
    fun readLines(file: File): List<String>? {
        return try {
            file.readLines()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Записать строки в файл
     */
    fun writeLines(file: File, lines: List<String>): Boolean {
        return try {
            file.writeText(lines.joinToString("\n"))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Прочитать байты из файла
     */
    fun readBytes(file: File): ByteArray? {
        return try {
            file.readBytes()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Записать байты в файл
     */
    fun writeBytes(file: File, bytes: ByteArray): Boolean {
        return try {
            file.writeBytes(bytes)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Скопировать файл
     */
    fun copy(source: File, destination: File): Boolean {
        return try {
            source.copyTo(destination, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Переместить файл
     */
    fun move(source: File, destination: File): Boolean {
        return try {
            if (copy(source, destination)) {
                source.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Удалить файл или директорию
     */
    fun delete(file: File): Boolean {
        return try {
            if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Создать директорию
     */
    fun createDirectory(dir: File): Boolean {
        return try {
            if (!dir.exists()) {
                dir.mkdirs()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Получить размер файла в байтах
     */
    fun getSize(file: File): Long {
        return try {
            if (file.isDirectory) {
                file.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
            } else {
                file.length()
            }
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Форматировать размер файла в читаемый вид
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * Получить расширение файла
     */
    fun getExtension(file: File): String {
        return file.extension
    }
    
    /**
     * Получить имя файла без расширения
     */
    fun getNameWithoutExtension(file: File): String {
        return file.nameWithoutExtension
    }
    
    /**
     * Проверить существование файла
     */
    fun exists(file: File): Boolean {
        return file.exists()
    }
    
    /**
     * Получить список файлов в директории
     */
    fun listFiles(dir: File, filter: ((File) -> Boolean)? = null): List<File> {
        return try {
            if (!dir.isDirectory) return emptyList()
            
            val files = dir.listFiles() ?: return emptyList()
            
            if (filter != null) {
                files.filter(filter)
            } else {
                files.toList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Получить список файлов с определенным расширением
     */
    fun listFilesByExtension(dir: File, extension: String): List<File> {
        return listFiles(dir) { it.extension.equals(extension, ignoreCase = true) }
    }
    
    /**
     * Сохранить Bitmap в файл
     */
    fun saveBitmap(bitmap: Bitmap, file: File, format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 100): Boolean {
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(format, quality, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Получить временный файл
     */
    fun getTempFile(context: Context, prefix: String = "temp", suffix: String = ".tmp"): File {
        return File.createTempFile(prefix, suffix, context.cacheDir)
    }
    
    /**
     * Очистить кэш
     */
    fun clearCache(context: Context): Boolean {
        return try {
            context.cacheDir.deleteRecursively()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Получить размер кэша
     */
    fun getCacheSize(context: Context): Long {
        return getSize(context.cacheDir)
    }
    
    /**
     * Генерировать уникальное имя файла с временной меткой
     */
    fun generateFileName(prefix: String = "file", extension: String = "txt"): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${prefix}_${timestamp}.${extension}"
    }
    
    /**
     * Прочитать текст из Uri
     */
    fun readTextFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Записать текст в Uri
     */
    fun writeTextToUri(context: Context, uri: Uri, text: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.bufferedWriter().use { it.write(text) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Информация о файле
     */
    data class FileInfo(
        val name: String,
        val path: String,
        val size: Long,
        val sizeFormatted: String,
        val extension: String,
        val isDirectory: Boolean,
        val lastModified: Long,
        val canRead: Boolean,
        val canWrite: Boolean
    )
    
    /**
     * Получить детальную информацию о файле
     */
    fun getFileInfo(file: File): FileInfo {
        return FileInfo(
            name = file.name,
            path = file.absolutePath,
            size = getSize(file),
            sizeFormatted = formatSize(getSize(file)),
            extension = file.extension,
            isDirectory = file.isDirectory,
            lastModified = file.lastModified(),
            canRead = file.canRead(),
            canWrite = file.canWrite()
        )
    }
}

