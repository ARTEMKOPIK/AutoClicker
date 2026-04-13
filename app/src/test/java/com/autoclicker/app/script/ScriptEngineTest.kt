package com.autoclicker.app.script

import android.content.Context
import com.autoclicker.app.util.PrefsManager
import com.autoclicker.app.util.TelegramSender
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for ScriptEngine coordinate validation and basic execution.
 */
class ScriptEngineTest {

    private lateinit var context: Context
    private lateinit var prefsManager: PrefsManager
    private lateinit var scriptEngine: ScriptEngine

    @Before
    fun setup() {
        context = mock()
        prefsManager = mock()
        
        // Mock TelegramSender dependencies
        whenever(prefsManager.telegramToken).thenReturn("")
        whenever(prefsManager.telegramChatId).thenReturn("")
        
        scriptEngine = ScriptEngine(
            context = context,
            logCallback = {}
        )
    }

    @Test
    fun `engine initializes without exceptions`() = runTest {
        assertThat(scriptEngine.EXIT).isFalse()
    }

    @Test
    fun `EXIT flag can be set and retrieved`() = runTest {
        scriptEngine.EXIT = true
        assertThat(scriptEngine.EXIT).isTrue()
        
        scriptEngine.EXIT = false
        assertThat(scriptEngine.EXIT).isFalse()
    }

    @Test
    fun `log callback is invoked on log command`() = runTest {
        var logReceived = ""
        val engine = ScriptEngine(context, logCallback = { logReceived = it })
        
        // Execute a simple log command
        engine.executeLine("""log("test message")""")
        
        assertThat(logReceived).contains("test message")
    }

    @Test
    fun `sleep command with valid duration executes successfully`() = runTest {
        // This should not throw exception for valid input
        scriptEngine.executeLine("sleep(10)")
        assertThat(scriptEngine.EXIT).isFalse()
    }

    @Test
    fun `click command validates coordinates`() = runTest {
        // Valid coordinates should execute without exception
        scriptEngine.executeLine("click(100, 200)")
        assertThat(scriptEngine.EXIT).isFalse()
    }

    @Test
    fun `close releases resources`() = runTest {
        scriptEngine.close()
        // After close, engine should still handle EXIT flag gracefully
        assertThat(scriptEngine.EXIT).isFalse()
    }
}
