package com.autoclicker.app.util

/**
 * Utility class for consistent JSON string escaping.
 * 
 * CRITICAL: The escape order is important. Backslashes MUST be escaped FIRST,
 * otherwise they will be double-escaped when quotes are escaped.
 * 
 * WRONG ORDER:
 *   " -> \" then \ -> \\
 *   Result: \" becomes \\\"
 * 
 * CORRECT ORDER:
 *   \ -> \\\\ then " -> \\\"
 *   Result: Proper escaping without double-escaping
 * 
 * Thread-safety: This object is stateless and thread-safe.
 */
object JsonEscaper {
    
    /**
     * Escapes a string for safe inclusion in JSON.
     * 
     * @param text The input text to escape
     * @return The escaped text safe for JSON
     */
    fun escape(text: String): String {
        if (text.isEmpty()) return ""
        
        return text
            .replace("\\", "\\\\")  // FIRST: Escape backslashes to prevent double-escaping
            .replace("\"", "\\\"")  // SECOND: Escape quotes (backslashes in \" are already escaped)
            .replace("\n", "\\n")     // THIRD: Escape newlines
            .replace("\r", "\\r")     // FOURTH: Escape carriage returns
            .replace("\t", "\\t")     // FIFTH: Escape tabs
            .replace("\b", "\\b")     // SIXTH: Escape backspace
            .replace("\u000C", "\\f")     // SEVENTH: Escape form feed
    }
    
    /**
     * Validates if a string needs JSON escaping.
     * 
     * @param text The input text to check
     * @return true if the text contains characters that need escaping
     */
    fun needsEscaping(text: String): Boolean {
        return text.any { char ->
            char == '\\' || char == '"' || char == '\n' || 
            char == '\r' || char == '\t' || char == '\b' || 
            char == '\u000C'
        }
    }
    
    /**
     * Escapes text and truncates if it exceeds max length.
     * Uses safe truncation that avoids cutting escaped sequences.
     * 
     * @param text The input text to escape
     * @param maxLength Maximum allowed length
     * @return The escaped and possibly truncated text
     */
    fun escapeAndTruncate(text: String, maxLength: Int): String {
        if (maxLength <= 0) return ""

        val escaped = escape(text)
        if (escaped.length <= maxLength) return escaped

        if (maxLength < 4) {
            return "...".take(maxLength)
        }

        // Reserve space for ellipsis and safely trim escaped sequences.
        var base = escaped.substring(0, maxLength - 3)

        // If base ends with odd number of trailing backslashes, we cut inside an escape sequence.
        var trailingBackslashes = 0
        for (i in base.length - 1 downTo 0) {
            if (base[i] == '\\') {
                trailingBackslashes++
            } else {
                break
            }
        }
        if (trailingBackslashes % 2 == 1 && base.isNotEmpty()) {
            base = base.dropLast(1)
        }

        return base + "..."
    }
}