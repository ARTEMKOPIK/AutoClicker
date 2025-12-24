package com.autoclicker.app

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.util.PrefsManager
import com.autoclicker.app.util.TelegramSender
import kotlinx.coroutines.*

class TelegramSettingsActivity : BaseActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var etToken: EditText
    private lateinit var etChatId: EditText
    private lateinit var btnTest: Button
    private lateinit var btnSave: Button
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telegram_settings)

        prefs = PrefsManager(this)

        etToken = findViewById(R.id.etToken)
        etChatId = findViewById(R.id.etChatId)
        btnTest = findViewById(R.id.btnTest)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)

        // Load saved values
        etToken.setText(prefs.telegramToken)
        etChatId.setText(prefs.telegramChatId)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        btnSave.setOnClickListener {
            saveSettings()
        }

        btnTest.setOnClickListener {
            testConnection()
        }
    }

    private fun saveSettings() {
        val token = etToken.text.toString().trim()
        val chatId = etChatId.text.toString().trim()
        
        // Валидация токена
        if (token.isNotEmpty() && !isValidToken(token)) {
            Toast.makeText(this, "Неверный формат токена", Toast.LENGTH_SHORT).show()
            etToken.requestFocus()
            return
        }
        
        // Валидация Chat ID
        if (chatId.isNotEmpty() && !isValidChatId(chatId)) {
            Toast.makeText(this, "Неверный формат Chat ID", Toast.LENGTH_SHORT).show()
            etChatId.requestFocus()
            return
        }
        
        prefs.telegramToken = token
        prefs.telegramChatId = chatId
        Toast.makeText(this, getString(R.string.msg_saved), Toast.LENGTH_SHORT).show()
    }
    
    private fun isValidToken(token: String): Boolean {
        // Формат: ID:секретная_часть (ID может быть до 15 цифр, секретная часть 30-60 символов)
        return token.matches(Regex("^\\d{6,15}:[A-Za-z0-9_-]{25,60}$"))
    }
    
    private fun isValidChatId(chatId: String): Boolean {
        // Может быть положительным (личный чат) или отрицательным (группа/канал)
        return chatId.matches(Regex("^-?\\d+$"))
    }

    private fun testConnection() {
        val token = etToken.text.toString().trim()
        val chatId = etChatId.text.toString().trim()

        if (token.isEmpty() || chatId.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isValidToken(token)) {
            Toast.makeText(this, "Неверный формат токена", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!isValidChatId(chatId)) {
            Toast.makeText(this, "Неверный формат Chat ID", Toast.LENGTH_SHORT).show()
            return
        }

        // Сохраняем перед тестом
        saveSettings()
        
        // Показываем индикатор загрузки
        setLoading(true)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sender = TelegramSender(token, chatId)
                val success = sender.sendMessageSync("✅ AutoClicker подключён!\nТестовое сообщение.")
                
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    if (success) {
                        Toast.makeText(this@TelegramSettingsActivity, getString(R.string.msg_message_sent), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@TelegramSettingsActivity, "Ошибка отправки", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Toast.makeText(this@TelegramSettingsActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun setLoading(loading: Boolean) {
        btnTest.isEnabled = !loading
        btnSave.isEnabled = !loading
        btnTest.text = if (loading) "Отправка..." else "🧪 Тест подключения"
        progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
    }
}
