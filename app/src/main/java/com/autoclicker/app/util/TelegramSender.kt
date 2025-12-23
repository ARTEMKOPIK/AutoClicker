package com.autoclicker.app.util

import android.graphics.Bitmap
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Telegram sender for bot notifications and error reporting.
 * 
 * HTTP timeouts configured to Constants.NETWORK_TIMEOUT_SECONDS (15 seconds)
 * for reliable operation without excessive waiting.
 * 
 * JSON escaping uses JsonEscaper utility for consistent and correct escaping order.
 * 
 * Thread-safety: This class is stateless except for OkHttpClient which is thread-safe.
 */
class TelegramSender(
    private val token: String,
    private val chatId: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(Constants.NETWORK_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        .writeTimeout(Constants.NETWORK_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        .readTimeout(Constants.NETWORK_TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val baseUrl = "https://api.telegram.org/bot"

    fun sendMessage(text: String, onError: ((String) -> Unit)? = null) {
        if (token.isEmpty() || chatId.isEmpty()) {
            onError?.invoke(Constants.ERROR_TELEGRAM_CREDENTIALS_EMPTY)
            return
        }

        val url = "$baseUrl$token/sendMessage"
        val escapedText = JsonEscaper.escape(text)
        val json = """{"chat_id":"$chatId","text":"$escapedText"}"""
        
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val errorMsg = when {
                    e is java.net.SocketTimeoutException -> "Timeout: сервер не отвечает"
                    e is java.net.UnknownHostException -> "Нет подключения к интернету"
                    else -> e.message ?: "Unknown error"
                }
                CrashHandler.logError("TelegramSender", "Failed to send message", e)
                onError?.invoke(errorMsg)
            }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorMsg = when (response.code) {
                        401 -> "Неверный токен бота"
                        400 -> "Неверный chat_id или формат сообщения"
                        429 -> "Слишком много запросов, подождите"
                        else -> "HTTP ${response.code}"
                    }
                    android.util.Log.e("TelegramSender", "Error response: ${response.code}")
                    onError?.invoke(errorMsg)
                }
                response.close()
            }
        })
    }
    
    /**
     * Синхронная отправка сообщения с возвратом результата
     */
    fun sendMessageSync(text: String): Boolean {
        if (token.isEmpty() || chatId.isEmpty()) return false

        val url = "$baseUrl$token/sendMessage"
        val escapedText = JsonEscaper.escape(text)
        val json = """{"chat_id":"$chatId","text":"$escapedText"}"""
        
        val request = Request.Builder()
            .url(url)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            response.close()
            success
        } catch (e: java.net.SocketTimeoutException) {
            CrashHandler.logError("TelegramSender", "Timeout sending message", e)
            false
        } catch (e: java.net.UnknownHostException) {
            CrashHandler.logError("TelegramSender", "No internet connection", e)
            false
        } catch (e: IOException) {
            CrashHandler.logError("TelegramSender", "Error sending message", e)
            false
        }
    }

    fun sendPhoto(caption: String, bitmap: Bitmap, onError: ((String) -> Unit)? = null) {
        if (token.isEmpty() || chatId.isEmpty()) {
            onError?.invoke("Token or ChatId is empty")
            return
        }
        
        if (bitmap.isRecycled) {
            onError?.invoke("Bitmap is recycled")
            return
        }

        val url = "$baseUrl$token/sendPhoto"
        
        val byteArray: ByteArray
        try {
            ByteArrayOutputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                byteArray = stream.toByteArray()
            }
        } catch (e: Exception) {
            android.util.Log.e("TelegramSender", "Error compressing bitmap", e)
            onError?.invoke(e.message ?: "Compression error")
            return
        }
        
        if (byteArray.isEmpty()) {
            onError?.invoke("Empty image data")
            return
        }

        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)
                .addFormDataPart("caption", caption)
                .addFormDataPart(
                    "photo", "screenshot.png",
                    byteArray.toRequestBody("image/png".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val errorMsg = when {
                        e is java.net.SocketTimeoutException -> "Timeout: сервер не отвечает"
                        e is java.net.UnknownHostException -> "Нет подключения к интернету"
                        else -> e.message ?: "Unknown error"
                    }
                    CrashHandler.logError("TelegramSender", "Failed to send photo", e)
                    onError?.invoke(errorMsg)
                }
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        val errorMsg = when (response.code) {
                            401 -> "Неверный токен бота"
                            400 -> "Неверный chat_id или формат"
                            413 -> "Фото слишком большое"
                            else -> "HTTP ${response.code}"
                        }
                        CrashHandler.logError("TelegramSender", "Error response: ${response.code}")
                        onError?.invoke(errorMsg)
                    }
                    response.close()
                }
            })
        } catch (e: Exception) {
            CrashHandler.logError("TelegramSender", "Error preparing photo request", e)
            onError?.invoke(e.message ?: "Unknown error")
        }
    }
}
