package com.autoclicker.app.util

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
        return screenWidth > 0 && screenHeight > 0 && x >= 0 && x < screenWidth && y >= 0 && y < screenHeight
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

