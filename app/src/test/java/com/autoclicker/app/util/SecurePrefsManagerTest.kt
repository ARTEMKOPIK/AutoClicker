package com.autoclicker.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for SecurePrefsManager - encrypted preferences wrapper.
 */
class SecurePrefsManagerTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var securePrefsManager: SecurePrefsManager

    @Before
    fun setup() {
        context = mock()
        sharedPreferences = mock()
        
        // Mock EncryptedSharedPreferences creation
        whenever(context.applicationContext).thenReturn(context)
        
        securePrefsManager = SecurePrefsManager(context)
    }

    @Test
    fun `manager initializes without exceptions`() {
        // Should not throw exception during initialization
        assertThat(securePrefsManager).isNotNull()
    }

    @Test
    fun `saveAndRetrieve sensitive data`() {
        // Save sensitive token
        securePrefsManager.saveToken("test_token_123")
        
        // Retrieve should return same value
        val retrieved = securePrefsManager.getToken()
        assertThat(retrieved).isEqualTo("test_token_123")
    }

    @Test
    fun `clear removes all data`() {
        // Save some data
        securePrefsManager.saveToken("test_token")
        securePrefsManager.saveChatId("test_chat_id")
        
        // Clear all
        securePrefsManager.clear()
        
        // Data should be removed
        assertThat(securePrefsManager.getToken()).isNull()
        assertThat(securePrefsManager.getChatId()).isNull()
    }

    @Test
    fun `handles null values gracefully`() {
        // Initially should return null
        assertThat(securePrefsManager.getToken()).isNull()
        assertThat(securePrefsManager.getChatId()).isNull()
    }
}

/**
 * Secure preferences manager using EncryptedSharedPreferences.
 * Provides secure storage for sensitive data like tokens and chat IDs.
 */
class SecurePrefsManager(private val context: Context) {
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    companion object {
        private const val KEY_TOKEN = "encrypted_token"
        private const val KEY_CHAT_ID = "encrypted_chat_id"
    }
    
    fun saveToken(token: String) {
        encryptedPrefs.edit().putString(KEY_TOKEN, token).apply()
    }
    
    fun getToken(): String? {
        return encryptedPrefs.getString(KEY_TOKEN, null)
    }
    
    fun saveChatId(chatId: String) {
        encryptedPrefs.edit().putString(KEY_CHAT_ID, chatId).apply()
    }
    
    fun getChatId(): String? {
        return encryptedPrefs.getString(KEY_CHAT_ID, null)
    }
    
    fun clear() {
        encryptedPrefs.edit().clear().apply()
    }
}
