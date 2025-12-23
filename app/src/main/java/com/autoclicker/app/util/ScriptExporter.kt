package com.autoclicker.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.util.zip.Deflater
import java.util.zip.Inflater
import android.util.Base64

/**
 * Импорт и экспорт скриптов
 */
class ScriptExporter(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val gsonCompact: Gson = Gson()
    private val exportDir: File
        get() = File(context.filesDir, "exports").also { it.mkdirs() }

    data class ExportedScript(
        val name: String,
        val code: String,
        val version: Int = 1,
        val exportDate: String = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(java.util.Date())
    )

    /**
     * Экспортировать скрипт в файл
     */
    fun exportScript(script: ScriptStorage.Script): File? {
        return try {
            val exported = ExportedScript(
                name = script.name,
                code = script.code
            )
            val json = gson.toJson(exported)
            val fileName = "${sanitizeFileName(script.name)}.txt"
            val file = File(exportDir, fileName)
            file.writeText(json)
            file
        } catch (e: Exception) {
            android.util.Log.e("ScriptExporter", "Export error", e)
            null
        }
    }

    /**
     * Получить Intent для шаринга скрипта
     */
    fun getShareIntent(script: ScriptStorage.Script): Intent? {
        val file = exportScript(script) ?: return null

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "AutoClicker Script: ${script.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Получить Intent для шаринга QR-кода как картинки
     */
    fun getShareQRIntent(script: ScriptStorage.Script): Intent? {
        val qrBitmap = generateQRCode(script) ?: return null
        
        return try {
            // Сохраняем QR-код во временный файл
            val fileName = "${sanitizeFileName(script.name)}_qr.png"
            val file = File(exportDir, fileName)
            file.outputStream().use { out ->
                qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AutoClicker QR: ${script.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            android.util.Log.e("ScriptExporter", "Share QR error", e)
            null
        }
    }

    /**
     * Импортировать скрипт из URI
     */
    fun importScript(uri: Uri): ScriptStorage.Script? {
        return try {
            // Используем try-with-resources (.use) для автоматического закрытия потока
            // Это предотвращает утечку ресурсов и NullPointerException при закрытии
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val json = inputStream.bufferedReader().readText()
                val exported = gson.fromJson(json, ExportedScript::class.java)
                ScriptStorage.Script(
                    id = java.util.UUID.randomUUID().toString(),
                    name = exported.name,
                    code = exported.code
                )
            } ?: return null
        } catch (e: Exception) {
            android.util.Log.e("ScriptExporter", "Import error", e)
            null
        }
    }

    /**
     * Импортировать скрипт из текста (JSON)
     */
    fun importScriptFromJson(json: String): ScriptStorage.Script? {
        return try {
            val exported = gson.fromJson(json, ExportedScript::class.java)
            ScriptStorage.Script(
                id = java.util.UUID.randomUUID().toString(),
                name = exported.name,
                code = exported.code
            )
        } catch (e: Exception) {
            android.util.Log.e("ScriptExporter", "Import from JSON error", e)
            null
        }
    }

    /**
     * Генерация QR-кода для скрипта
     */
    fun generateQRCode(script: ScriptStorage.Script, size: Int = 512): Bitmap? {
        return try {
            val data = compressScript(script)
            if (data.length > 2900) {
                // QR код не может содержать больше ~3000 символов
                android.util.Log.w("ScriptExporter", "Script too large for QR: ${data.length}")
                return null
            }

            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)

            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("ScriptExporter", "QR generation error", e)
            null
        }
    }

    /**
     * Импорт скрипта из QR-кода (строки)
     */
    fun importFromQRData(data: String): ScriptStorage.Script? {
        return try {
            decompressScript(data)
        } catch (e: Exception) {
            android.util.Log.e("ScriptExporter", "QR import error", e)
            null
        }
    }

    /**
     * Сжатие скрипта для QR-кода
     */
    private fun compressScript(script: ScriptStorage.Script): String {
        val json = gsonCompact.toJson(ExportedScript(script.name, script.code))
        val input = json.toByteArray(Charsets.UTF_8)

        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(input)
        deflater.finish()

        val output = ByteArray(input.size)
        val compressedSize = deflater.deflate(output)
        deflater.end()

        // Safety check for compression result
        if (compressedSize == 0) {
            android.util.Log.w("ScriptExporter", "Compression resulted in zero bytes")
            return ""
        }
        
        val compressed = output.copyOf(compressedSize)
        return "AC1:" + Base64.encodeToString(compressed, Base64.NO_WRAP)
    }

    /**
     * Распаковка скрипта из QR-кода
     */
    private fun decompressScript(data: String): ScriptStorage.Script? {
        if (!data.startsWith("AC1:")) return null

        val base64 = data.substring(4)
        val compressed = try {
            Base64.decode(base64, Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("ScriptExporter", "Base64 decode error", e)
            return null
        }

        val inflater = Inflater()
        return try {
            inflater.setInput(compressed)

            // Используем динамический буфер для безопасности
            val outputStream = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (count == 0 && inflater.needsInput()) break
                outputStream.write(buffer, 0, count)
                
                // Защита от слишком больших данных (макс 1MB)
                if (outputStream.size() > 1024 * 1024) {
                    android.util.Log.e("ScriptExporter", "Decompressed data too large")
                    return null
                }
            }

            val json = outputStream.toString(Charsets.UTF_8.name())
            val exported = gsonCompact.fromJson(json, ExportedScript::class.java)

            ScriptStorage.Script(
                id = java.util.UUID.randomUUID().toString(),
                name = exported.name,
                code = exported.code
            )
        } catch (e: Exception) {
            android.util.Log.e("ScriptExporter", "Decompress error", e)
            null
        } finally {
            inflater.end()
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Zа-яА-Я0-9_-]"), "_").take(50)
    }
}
