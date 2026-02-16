package com.autoclicker.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonEscaperTest {

    @Test
    fun `escapeAndTruncate handles quote close to limit without broken escape`() {
        val input = "1234\"6789"
        val result = JsonEscaper.escapeAndTruncate(input, 9)

        assertEquals("1234...", result)
        assertFalse("Truncated string should not end with dangling backslash", result.dropLast(3).endsWith("\\"))
    }

    @Test
    fun `escapeAndTruncate keeps complete escaped backslash sequence`() {
        val input = "abc\\def"
        val result = JsonEscaper.escapeAndTruncate(input, 8)

        assertEquals("abc\\...", result)
        assertFalse("Base should not end with odd count of backslashes", hasOddTrailingBackslashes(result.dropLast(3)))
    }

    @Test
    fun `escapeAndTruncate handles newline close to limit`() {
        val input = "abcd\nefgh"
        val result = JsonEscaper.escapeAndTruncate(input, 8)

        assertEquals("abcd...", result)
        assertFalse("Newline escape should not be cut after backslash", result.dropLast(3).endsWith("\\"))
    }

    @Test
    fun `escapeAndTruncate supports tiny limits predictably`() {
        val input = "abcdef"

        assertEquals("", JsonEscaper.escapeAndTruncate(input, 0))
        assertEquals(".", JsonEscaper.escapeAndTruncate(input, 1))
        assertEquals("..", JsonEscaper.escapeAndTruncate(input, 2))
        assertEquals("...", JsonEscaper.escapeAndTruncate(input, 3))
    }

    @Test
    fun `escapeAndTruncate returns full escaped string when it fits`() {
        val input = "ok\n\"yes\""
        val escaped = JsonEscaper.escape(input)
        val result = JsonEscaper.escapeAndTruncate(input, escaped.length)

        assertEquals(escaped, result)
        assertTrue(result.contains("\\n"))
        assertTrue(result.contains("\\\"yes\\\""))
    }


    @Test
    fun `escapeAndTruncate handles emoji near truncation boundary`() {
        val input = "abcd😀xyz"
        val result = JsonEscaper.escapeAndTruncate(input, 8)

        assertEquals("abcd...", result)
        assertFalse("Result base should not end with dangling high surrogate", result.dropLast(3).lastOrNull()?.isHighSurrogate() == true)
    }
    private fun hasOddTrailingBackslashes(value: String): Boolean {
        var count = 0
        for (i in value.length - 1 downTo 0) {
            if (value[i] == '\\') count++ else break
        }
        return count % 2 == 1
    }
}
