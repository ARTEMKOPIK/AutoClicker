package com.autoclicker.app.util

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.widget.EditText
import java.util.regex.Pattern

import androidx.core.content.ContextCompat
import com.autoclicker.app.R

/**
 * Подсветка синтаксиса для редактора скриптов
 */
class SyntaxHighlighter(private val editText: EditText) : TextWatcher {

    private val colorKeyword: Int by lazy { ContextCompat.getColor(editText.context, R.color.syntax_keyword) }
    private val colorFunction: Int by lazy { ContextCompat.getColor(editText.context, R.color.syntax_function) }
    private val colorString: Int by lazy { ContextCompat.getColor(editText.context, R.color.syntax_string) }
    private val colorNumber: Int by lazy { ContextCompat.getColor(editText.context, R.color.syntax_number) }
    private val colorComment: Int by lazy { ContextCompat.getColor(editText.context, R.color.syntax_comment) }

    companion object {
        // Паттерны (компилируются один раз)
        private val PATTERN_KEYWORDS = Pattern.compile(
            "\\b(while|if|else|val|var|true|false|EXIT|return|break|continue|fun)\\b"
        )
        private val PATTERN_FUNCTIONS = Pattern.compile(
            "\\b(click|sleep|log|sendTelegram|swipe|longClick|getText|getColor|pushToCb|screenshot|" +
            "tap|back|home|recents|waitForColor|waitForText|compareColor|random|vibrate|toast|" +
            "setVar|getVar|incVar|decVar)\\s*\\("
        )
        private val PATTERN_STRINGS = Pattern.compile("\"[^\"]*\"|'[^']*'")
        private val PATTERN_NUMBERS = Pattern.compile("\\b\\d+\\.?\\d*\\b")
        private val PATTERN_COMMENTS = Pattern.compile("//.*")
    }

    private var isHighlighting = false
    private val handler = Handler(Looper.getMainLooper())
    private var lastHighlightedText: String? = null
    private val highlightRunnable = Runnable {
        editText.text?.let { editable ->
            val currentText = editable.toString()
            // Пропускаем если текст не изменился
            if (currentText == lastHighlightedText) return@Runnable
            
            isHighlighting = true
            try {
                highlight(editable)
                lastHighlightedText = currentText
            } catch (e: Exception) {
                CrashHandler.logWarning("SyntaxHighlighter", "Error highlighting text", e)
            }
            isHighlighting = false
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(editable: Editable?) {
        if (isHighlighting || editable == null) return
        
        // Debounce - откладываем подсветку чтобы не тормозить при быстром вводе
        handler.removeCallbacks(highlightRunnable)
        handler.postDelayed(highlightRunnable, Constants.HIGHLIGHT_DELAY_MS)
    }

    private fun highlight(editable: Editable) {
        if (editable.isEmpty()) return
        
        val text = editable.toString()
        if (text.length > Constants.MAX_TEXT_FOR_HIGHLIGHT_CHARS) {
            // Пропускаем подсветку для очень больших текстов
            CrashHandler.logDebug("SyntaxHighlighter", "Text too large (${text.length} chars), skipping highlight")
            return
        }
        
        // Удаляем старые спаны
        val spans = editable.getSpans(0, editable.length, ForegroundColorSpan::class.java)
        for (span in spans) {
            editable.removeSpan(span)
        }

        // Применяем подсветку в порядке приоритета (последние перекрывают предыдущие)
        applyPattern(editable, PATTERN_NUMBERS, colorNumber)
        applyPattern(editable, PATTERN_KEYWORDS, colorKeyword)
        applyPattern(editable, PATTERN_FUNCTIONS, colorFunction)
        // Строки и комментарии должны перекрывать всё остальное
        applyPatternExclusive(editable, PATTERN_STRINGS, colorString)
        applyPatternExclusive(editable, PATTERN_COMMENTS, colorComment)
    }

    private fun applyPatternExclusive(editable: Editable, pattern: Pattern, color: Int) {
        try {
            val matcher = pattern.matcher(editable)
            while (matcher.find()) {
                // Удаляем все существующие спаны в этом диапазоне
                val existingSpans = editable.getSpans(matcher.start(), matcher.end(), ForegroundColorSpan::class.java)
                for (span in existingSpans) {
                    editable.removeSpan(span)
                }
                editable.setSpan(
                    ForegroundColorSpan(color),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } catch (e: Exception) {
            // Игнорируем ошибки regex
        }
    }

    private fun applyPattern(editable: Editable, pattern: Pattern, color: Int) {
        try {
            val matcher = pattern.matcher(editable)
            while (matcher.find()) {
                editable.setSpan(
                    ForegroundColorSpan(color),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } catch (e: Exception) {
            // Игнорируем ошибки regex
        }
    }

    fun attach() {
        editText.addTextChangedListener(this)
        // Подсвечиваем существующий текст
        editText.text?.let { 
            isHighlighting = true
            try {
                highlight(it)
            } catch (e: Exception) {
                // Игнорируем
            }
            isHighlighting = false
        }
    }

    fun cleanup() {
        handler.removeCallbacks(highlightRunnable)
        detach()
    }

    fun detach() {
        handler.removeCallbacksAndMessages(null) // Удаляем ВСЕ callbacks
        editText.removeTextChangedListener(this)
    }
}
