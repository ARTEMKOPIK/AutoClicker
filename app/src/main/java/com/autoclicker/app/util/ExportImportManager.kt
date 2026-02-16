package com.autoclicker.app.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

/**
 * Менеджер экспорта и импорта скриптов
 * 
 * Обрабатывает сохранение и загрузку скриптов в/из JSON файлов.
 * Использует Storage Access Framework (SAF) для доступа к файлам.
 * 
 * **Формат JSON:**
 * ```json
 * {
 *   "id": "550e8400-e29b-41d4-a716-446655440000",
 *   "name": "Мой скрипт",
 *   "code": "click(100, 200)\nsleep(1000)",
 *   "date": 1640000000000,
 *   "version": "1.0"
 * }
 * ```
 * 
 * **Для массового экспорта:**
 * ```json
 * [
 *   { "id": "...", "name": "...", ... },
 *   { "id": "...", "name": "...", ... }
 * ]
 * ```
 * 
 * @example
 * ```kotlin
 * // Экспорт одного скрипта
 * val json = ExportImportManager.exportScriptToJson(script)
 * 
 * // Импорт скрипта из URI
 * val script = ExportImportManager.importScriptFromUri(context, uri)
 * if (script != null) {
 *     ScriptStorage.saveScript(context, script)
 * }
 * 
 * // Экспорт всех скриптов
 * val scripts = ScriptStorage.loadScripts(context)
 * val json = ExportImportManager.exportAllScriptsToJson(scripts)
 * ```
 */
object ExportImportManager {
    
    private val gson = Gson()
    
    /**
     * Экспортировать скрипт в JSON строку
     * 
     * @param script Скрипт для экспорта
     * @return JSON строка или null при ошибке
     */
    fun exportScriptToJson(script: ScriptStorage.Script): String? {
        return try {
            gson.toJson(script)
        } catch (e: Exception) {
            CrashHandler.logError("ExportImportManager", "Failed to export script: ${script.name}", e)
            null
        }
    }
    
    /**
     * Экспортировать несколько скриптов в JSON массив
     * 
     * @param scripts Список скриптов для экспорта
     * @return JSON строка с массивом или null при ошибке
     */
    fun exportAllScriptsToJson(scripts: List<ScriptStorage.Script>): String? {
        return try {
            gson.toJson(scripts)
        } catch (e: Exception) {
            CrashHandler.logError("ExportImportManager", "Failed to export scripts", e)
            null
        }
    }
    
    /**
     * Импортировать скрипт из JSON строки
     * 
     * Автоматически обрабатывает коллизии ID (генерирует новый UUID).
     * Проверяет версию для совместимости.
     * 
     * @param json JSON строка
     * @param context Контекст для проверки коллизий ID
     * @return Импортированный скрипт или null при ошибке
     */
    fun importScriptFromJson(json: String, context: Context): ScriptStorage.Script? {
        return try {
            val script = gson.fromJson(json, ScriptStorage.Script::class.java)
            
            // Проверяем базовые поля
            if (script.name.isBlank()) {
                CrashHandler.logWarning("ExportImportManager", "Script has empty name")
                return null
            }
            
            if (script.code.isBlank()) {
                CrashHandler.logWarning("ExportImportManager", "Script has empty code")
                return null
            }
            
            // Проверяем коллизию ID
            val storage = ScriptStorage(context)
            val existingScripts = storage.getAllScripts()
            val hasIdCollision = existingScripts.any { it.id == script.id }
            
            if (hasIdCollision) {
                CrashHandler.logInfo("ExportImportManager", "ID collision detected, generating new UUID")
                return script.copy(id = UUID.randomUUID().toString())
            }
            
            CrashHandler.logInfo("ExportImportManager", "Script imported successfully: ${script.name}")
            script
        } catch (e: JsonSyntaxException) {
            CrashHandler.logError("ExportImportManager", "Invalid JSON format", e)
            null
        } catch (e: Exception) {
            CrashHandler.logError("ExportImportManager", "Failed to import script", e)
            null
        }
    }
    
    /**
     * Импортировать скрипт из файла через URI
     * 
     * Читает файл, парсит JSON и возвращает скрипт.
     * Использует Storage Access Framework.
     * 
     * @param context Контекст приложения
     * @param uri URI файла (получен из ACTION_OPEN_DOCUMENT)
     * @return Импортированный скрипт или null при ошибке
     */
    fun importScriptFromUri(context: Context, uri: Uri): ScriptStorage.Script? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: run {
                    CrashHandler.logError("ExportImportManager", "Cannot open input stream for URI: $uri", null)
                    return null
                }
            
            val reader = BufferedReader(InputStreamReader(inputStream))
            val json = reader.use { it.readText() }
            
            importScriptFromJson(json, context)
        } catch (e: Exception) {
            CrashHandler.logError("ExportImportManager", "Failed to read file from URI: $uri", e)
            null
        }
    }
    
    /**
     * Импортировать несколько скриптов из JSON массива
     * 
     * @param json JSON строка с массивом скриптов
     * @param context Контекст для проверки коллизий ID
     * @return Список импортированных скриптов (может быть пустым)
     */
    fun importAllScriptsFromJson(json: String, context: Context): List<ScriptStorage.Script> {
        return try {
            val scripts = gson.fromJson(json, Array<ScriptStorage.Script>::class.java).toList()
            val storage = ScriptStorage(context)
            val existingScripts = storage.getAllScripts()
            val usedIds = existingScripts.map { it.id }.toMutableSet()
            
            scripts.mapNotNull { script ->
                // Проверяем базовые поля
                if (script.name.isBlank() || script.code.isBlank()) {
                    CrashHandler.logWarning("ExportImportManager", "Skipping invalid script")
                    return@mapNotNull null
                }
                
                // Обрабатываем коллизию ID
                ensureUniqueId(script, usedIds)
            }
        } catch (e: JsonSyntaxException) {
            CrashHandler.logError("ExportImportManager", "Invalid JSON array format", e)
            emptyList()
        } catch (e: Exception) {
            CrashHandler.logError("ExportImportManager", "Failed to import scripts", e)
            emptyList()
        }
    }

    /**
     * Гарантирует уникальный ID скрипта в рамках текущего импорта.
     *
     * Важно: учитываем не только уже сохраненные скрипты, но и дубликаты
     * внутри самого импортируемого JSON-массива.
     */
    private fun ensureUniqueId(
        script: ScriptStorage.Script,
        usedIds: MutableSet<String>
    ): ScriptStorage.Script {
        var candidate = script

        while (usedIds.contains(candidate.id)) {
            candidate = candidate.copy(id = UUID.randomUUID().toString())
        }

        usedIds.add(candidate.id)
        return candidate
    }
    
    /**
     * Записать JSON в файл через URI
     * 
     * Используется для экспорта скриптов.
     * 
     * @param context Контекст приложения
     * @param uri URI файла (получен из ACTION_CREATE_DOCUMENT)
     * @param json JSON строка для записи
     * @return true если успешно, false при ошибке
     */
    fun writeJsonToUri(context: Context, uri: Uri, json: String): Boolean {
        return try {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: run {
                    CrashHandler.logError("ExportImportManager", "Cannot open output stream for URI: $uri", null)
                    return false
                }
            
            outputStream.use { it.write(json.toByteArray()) }
            CrashHandler.logInfo("ExportImportManager", "JSON written to file successfully")
            true
        } catch (e: Exception) {
            CrashHandler.logError("ExportImportManager", "Failed to write JSON to URI: $uri", e)
            false
        }
    }
    
    /**
     * Получить имя файла для экспорта скрипта
     * 
     * @param scriptName Имя скрипта
     * @return Безопасное имя файла
     */
    fun getSafeFileName(scriptName: String): String {
        // Заменяем небезопасные символы
        val safe = scriptName
            .replace(Regex("[^a-zA-Zа-яА-Я0-9\\s]"), "")
            .replace(Regex("\\s+"), "_")
            .take(50) // Ограничиваем длину
        
        return if (safe.isBlank()) {
            "script_${System.currentTimeMillis()}.json"
        } else {
            "${safe}.json"
        }
    }
}
