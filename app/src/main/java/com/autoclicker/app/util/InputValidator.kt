package com.autoclicker.app.util

import android.graphics.Color
import android.widget.EditText

/**
 * Утилита для валидации пользовательского ввода
 * Предотвращает ошибки и улучшает UX
 */
object InputValidator {
    
    /**
     * Проверить, что строка не пустая
     */
    fun isNotEmpty(text: String?): Boolean {
        return !text.isNullOrBlank()
    }
    
    /**
     * Проверить, что число находится в диапазоне
     */
    fun isInRange(value: Int, min: Int, max: Int): Boolean {
        return value in min..max
    }
    
    /**
     * Проверить, что число находится в диапазоне (Long)
     */
    fun isInRange(value: Long, min: Long, max: Long): Boolean {
        return value in min..max
    }
    
    /**
     * Проверить, что число находится в диапазоне (Float)
     */
    fun isInRange(value: Float, min: Float, max: Float): Boolean {
        return value in min..max
    }
    
    /**
     * Проверить координаты на валидность
     */
    fun isValidCoordinate(x: Float, y: Float, screenWidth: Int, screenHeight: Int): Boolean {
        return x >= 0 && x <= screenWidth && y >= 0 && y <= screenHeight
    }
    
    /**
     * Проверить координаты на валидность (Int version)
     */
    fun isValidCoordinate(x: Int, y: Int, screenWidth: Int, screenHeight: Int): Boolean {
        return x >= 0 && x < screenWidth && y >= 0 && y < screenHeight
    }
    
    /**
     * Проверить, что задержка валидна (не отрицательная и не слишком большая)
     */
    fun isValidDelay(delay: Long): Boolean {
        return delay >= 0 && delay <= 3600000 // Максимум 1 час
    }
    
    /**
     * Проверить, что длительность валидна
     */
    fun isValidDuration(duration: Long): Boolean {
        return duration > 0 && duration <= 10000 // Максимум 10 секунд
    }
    
    /**
     * Проверить, что количество повторений валидно
     */
    fun isValidRepeatCount(count: Int): Boolean {
        return count >= 1 && count <= 1000000 // Максимум 1 миллион
    }
    
    /**
     * Проверить имя файла на валидность
     */
    fun isValidFileName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        
        // Запрещенные символы в имени файла
        val forbiddenChars = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        return !name.any { it in forbiddenChars }
    }
    
    /**
     * Проверить URL на валидность (базовая проверка)
     */
    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.matches(Regex("^(http|https)://.*"))
    }
    
    /**
     * Проверить email на валидность (базовая проверка)
     */
    fun isValidEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }
    
    /**
     * Очистить и валидировать ввод из EditText
     */
    fun validateAndGetText(editText: EditText, errorMessage: String? = null): String? {
        val text = editText.text.toString().trim()
        
        if (text.isBlank()) {
            editText.error = errorMessage ?: "Поле не может быть пустым"
            return null
        }
        
        return text
    }
    
    /**
     * Валидировать и получить целое число из EditText
     */
    fun validateAndGetInt(
        editText: EditText,
        min: Int? = null,
        max: Int? = null,
        errorMessage: String? = null
    ): Int? {
        val text = validateAndGetText(editText) ?: return null
        
        val value = text.toIntOrNull()
        if (value == null) {
            editText.error = errorMessage ?: "Введите корректное число"
            return null
        }
        
        if (min != null && value < min) {
            editText.error = errorMessage ?: "Минимальное значение: $min"
            return null
        }
        
        if (max != null && value > max) {
            editText.error = errorMessage ?: "Максимальное значение: $max"
            return null
        }
        
        return value
    }
    
    /**
     * Валидировать и получить число с плавающей точкой из EditText
     */
    fun validateAndGetFloat(
        editText: EditText,
        min: Float? = null,
        max: Float? = null,
        errorMessage: String? = null
    ): Float? {
        val text = validateAndGetText(editText) ?: return null
        
        val value = text.toFloatOrNull()
        if (value == null) {
            editText.error = errorMessage ?: "Введите корректное число"
            return null
        }
        
        if (min != null && value < min) {
            editText.error = errorMessage ?: "Минимальное значение: $min"
            return null
        }
        
        if (max != null && value > max) {
            editText.error = errorMessage ?: "Максимальное значение: $max"
            return null
        }
        
        return value
    }
    
    /**
     * Результат валидации с подробной информацией
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    ) {
        companion object {
            fun success() = ValidationResult(true)
            fun error(message: String) = ValidationResult(false, message)
        }
    }
    
    /**
     * Комплексная валидация с результатом
     */
    fun validate(block: () -> Boolean, errorMessage: String): ValidationResult {
        return if (block()) {
            ValidationResult.success()
        } else {
            ValidationResult.error(errorMessage)
        }
    }
}

/**
 * Extension functions for coordinate and parameter validation
 */
object ValidationUtils {
    
    /**
     * Check if coordinates are valid within screen bounds
     */
    fun isValidCoordinate(x: Int, y: Int, screenWidth: Int, screenHeight: Int): Boolean {
        return x >= 0 && x < screenWidth && y >= 0 && y < screenHeight
    }
    
    /**
     * Check if sleep duration is valid (non-negative)
     */
    fun isValidSleepDuration(duration: Long): Boolean {
        return duration >= 0
    }
    
    /**
     * Clamp coordinate value to valid range
     */
    fun clampCoordinate(value: Int, min: Int, max: Int): Int {
        return value.coerceIn(min, max)
    }
    
    /**
     * Parse color from string format (#RRGGBB, RRGGBB, or named colors)
     */
    fun parseColor(colorString: String): Int? {
        return try {
            when {
                colorString.startsWith("#") -> Color.parseColor(colorString)
                colorString.length == 6 -> Color.parseColor("#$colorString")
                else -> parseNamedColor(colorString.lowercase())
            }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    
    private fun parseNamedColor(name: String): Int? {
        return when (name) {
            "red" -> Color.RED
            "green" -> Color.GREEN
            "blue" -> Color.BLUE
            "black" -> Color.BLACK
            "white" -> Color.WHITE
            "yellow" -> Color.YELLOW
            "cyan" -> Color.CYAN
            "magenta" -> Color.MAGENTA
            "gray", "grey" -> Color.GRAY
            "dkgray", "darkgray" -> Color.DKGRAY
            "ltgray", "lightgray" -> Color.LTGRAY
            "transparent" -> Color.TRANSPARENT
            else -> null
        }
    }
}


