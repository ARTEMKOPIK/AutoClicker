package com.autoclicker.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatEditText
import java.util.LinkedList
import java.util.regex.Pattern

/**
 * Кастомный редактор кода с номерами строк, Undo/Redo, поиском и подсветкой скобок
 */
class CodeEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private val lineNumberPaint = Paint().apply {
        color = Color.parseColor("#666666")
        textSize = 32f
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT
    }

    private val lineNumberBackgroundPaint = Paint().apply {
        color = Color.parseColor("#1F1F1F")
        style = Paint.Style.FILL
    }

    private val dividerPaint = Paint().apply {
        color = Color.parseColor("#333333")
        strokeWidth = 2f
    }

    private val bracketHighlightPaint = Paint().apply {
        color = Color.parseColor("#44FF9800")
        style = Paint.Style.FILL
    }

    private val currentLinePaint = Paint().apply {
        color = Color.parseColor("#1AFF9800")
        style = Paint.Style.FILL
    }

    private val rect = Rect()
    private val bracketRect = RectF()
    private var lineNumberWidth = 0

    // Undo/Redo стек
    private val undoStack = LinkedList<TextChange>()
    private val redoStack = LinkedList<TextChange>()
    private var isUndoRedo = false

    // Поиск
    private var searchPattern: Pattern? = null
    private var searchMatches = mutableListOf<IntRange>()
    private var currentMatchIndex = -1

    // Подсветка скобок
    private var matchingBracketStart = -1
    private var matchingBracketEnd = -1

    private data class TextChange(
        val oldText: String,
        val newText: String,
        val cursorPosition: Int
    )

    interface OnSearchListener {
        fun onSearchResults(count: Int, current: Int)
    }

    var searchListener: OnSearchListener? = null

    companion object {
        private const val MAX_UNDO_REDO_SIZE = 100
    }

    init {
        gravity = Gravity.TOP or Gravity.START
        typeface = Typeface.MONOSPACE
        setHorizontallyScrolling(true)
        updateLineNumberWidth()

        addTextChangedListener(object : TextWatcher {
            private var beforeText = ""
            private var lastChangeTime = 0L
            private val debounceMs = 300L

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (!isUndoRedo) {
                    val now = System.currentTimeMillis()
                    if (now - lastChangeTime > debounceMs || beforeText.isEmpty()) {
                        beforeText = s?.toString() ?: ""
                    }
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (!isUndoRedo && s != null) {
                    val newText = s.toString()
                    val now = System.currentTimeMillis()

                    if (beforeText != newText && now - lastChangeTime > debounceMs) {
                        undoStack.push(TextChange(beforeText, newText, selectionStart))
                        redoStack.clear()
                        // Limit undo stack size to prevent memory leaks and unbounded growth
                        // Remove oldest entries when we exceed MAX_UNDO_REDO_SIZE
                        while (undoStack.size > MAX_UNDO_REDO_SIZE) {
                            undoStack.removeLast()
                        }
                        lastChangeTime = now
                        beforeText = newText
                    }
                }
                updateLineNumberWidth()
                updateSearchHighlights()
            }
        })

        // Слушатель для подсветки скобок
        setOnClickListener { updateBracketMatching() }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        updateBracketMatching()
    }

    private fun updateLineNumberWidth() {
        val lineCount = maxOf(lineCount, 1)
        val digits = lineCount.toString().length
        lineNumberWidth = (lineNumberPaint.measureText("9".repeat(digits)) + LINE_NUMBER_PADDING * 2).toInt()
        setPadding(lineNumberWidth + CONTENT_PADDING, paddingTop, paddingRight, paddingBottom)
    }

    override fun onDraw(canvas: Canvas) {
        val currentLineHeight = lineHeight
        if (currentLineHeight <= 0) {
            super.onDraw(canvas)
            return
        }

        // Подсветка текущей строки
        val cursorLine = if (selectionStart >= 0) layout?.getLineForOffset(selectionStart) ?: 0 else 0
        val lineTop = layout?.getLineTop(cursorLine) ?: 0
        val lineBottom = layout?.getLineBottom(cursorLine) ?: 0
        canvas.drawRect(
            scrollX.toFloat(),
            lineTop.toFloat(),
            scrollX + width.toFloat(),
            lineBottom.toFloat(),
            currentLinePaint
        )

        // Подсветка парных скобок
        if (matchingBracketStart >= 0 && matchingBracketEnd >= 0) {
            drawBracketHighlight(canvas, matchingBracketStart)
            drawBracketHighlight(canvas, matchingBracketEnd)
        }

        // Фон для номеров строк
        canvas.drawRect(
            scrollX.toFloat(),
            scrollY.toFloat(),
            scrollX + lineNumberWidth.toFloat(),
            scrollY + height.toFloat(),
            lineNumberBackgroundPaint
        )

        // Разделитель
        canvas.drawLine(
            scrollX + lineNumberWidth.toFloat(),
            scrollY.toFloat(),
            scrollX + lineNumberWidth.toFloat(),
            scrollY + height.toFloat(),
            dividerPaint
        )

        // Номера строк
        val firstVisibleLine = scrollY / currentLineHeight
        val lastVisibleLine = (scrollY + height) / currentLineHeight + 1
        val totalLines = lineCount

        for (i in firstVisibleLine..minOf(lastVisibleLine, totalLines - 1)) {
            if (i < 0) continue
            val baseline = getLineBounds(i, rect)
            canvas.drawText(
                (i + 1).toString(),
                scrollX + lineNumberWidth - LINE_NUMBER_PADDING.toFloat(),
                baseline.toFloat(),
                lineNumberPaint
            )
        }

        super.onDraw(canvas)
    }

    private fun drawBracketHighlight(canvas: Canvas, position: Int) {
        val layout = layout ?: return
        if (position < 0 || position >= text?.length ?: 0) return

        val line = layout.getLineForOffset(position)
        val x = layout.getPrimaryHorizontal(position)
        val charWidth = paint.measureText("(")

        bracketRect.set(
            x - 2,
            layout.getLineTop(line).toFloat(),
            x + charWidth + 2,
            layout.getLineBottom(line).toFloat()
        )
        canvas.drawRoundRect(bracketRect, 4f, 4f, bracketHighlightPaint)
    }

    // ==================== ПОИСК И ЗАМЕНА ====================

    fun search(query: String, caseSensitive: Boolean = false, regex: Boolean = false) {
        searchMatches.clear()
        currentMatchIndex = -1
        clearSearchHighlights()

        if (query.isEmpty()) {
            searchListener?.onSearchResults(0, 0)
            return
        }

        try {
            val flags = if (caseSensitive) 0 else Pattern.CASE_INSENSITIVE
            searchPattern = if (regex) {
                Pattern.compile(query, flags)
            } else {
                Pattern.compile(Pattern.quote(query), flags)
            }

            val text = text?.toString() ?: return
            val matcher = searchPattern!!.matcher(text)

            while (matcher.find()) {
                searchMatches.add(matcher.start() until matcher.end())
            }

            if (searchMatches.isNotEmpty()) {
                currentMatchIndex = 0
                highlightSearchMatches()
                goToMatch(0)
            }

            searchListener?.onSearchResults(searchMatches.size, if (searchMatches.isEmpty()) 0 else 1)
        } catch (e: Exception) {
            searchListener?.onSearchResults(0, 0)
        }
    }

    fun findNext(): Boolean {
        if (searchMatches.isEmpty()) return false
        currentMatchIndex = (currentMatchIndex + 1) % searchMatches.size
        goToMatch(currentMatchIndex)
        searchListener?.onSearchResults(searchMatches.size, currentMatchIndex + 1)
        return true
    }

    fun findPrevious(): Boolean {
        if (searchMatches.isEmpty()) return false
        currentMatchIndex = if (currentMatchIndex <= 0) searchMatches.size - 1 else currentMatchIndex - 1
        goToMatch(currentMatchIndex)
        searchListener?.onSearchResults(searchMatches.size, currentMatchIndex + 1)
        return true
    }

    fun replaceCurrent(replacement: String): Boolean {
        if (searchMatches.isEmpty() || currentMatchIndex < 0) return false

        val match = searchMatches[currentMatchIndex]
        val editable = text ?: return false

        isUndoRedo = true
        try {
            editable.replace(match.first, match.last + 1, replacement)
        } finally {
            isUndoRedo = false
        }

        // Обновляем поиск
        val query = searchPattern?.pattern() ?: return true
        search(query.removePrefix("\\Q").removeSuffix("\\E"))
        return true
    }

    fun replaceAll(replacement: String): Int {
        if (searchMatches.isEmpty()) return 0

        val count = searchMatches.size
        val editable = text ?: return 0

        isUndoRedo = true
        try {
            // Заменяем с конца чтобы не сбивать индексы
            for (i in searchMatches.indices.reversed()) {
                val match = searchMatches[i]
                editable.replace(match.first, match.last + 1, replacement)
            }
        } finally {
            isUndoRedo = false
        }

        clearSearch()
        return count
    }

    fun clearSearch() {
        searchMatches.clear()
        currentMatchIndex = -1
        searchPattern = null
        clearSearchHighlights()
        searchListener?.onSearchResults(0, 0)
    }

    private fun goToMatch(index: Int) {
        if (index < 0 || index >= searchMatches.size) return
        val match = searchMatches[index]
        setSelection(match.first, match.last + 1)
        // Прокручиваем к выделению
        post {
            val layout = layout ?: return@post
            val line = layout.getLineForOffset(match.first)
            val y = layout.getLineTop(line)
            if (y < scrollY || y > scrollY + height - lineHeight) {
                scrollTo(scrollX, maxOf(0, y - height / 3))
            }
        }
    }

    private fun highlightSearchMatches() {
        val editable = text as? Spannable ?: return
        clearSearchHighlights()

        for ((index, match) in searchMatches.withIndex()) {
            val color = if (index == currentMatchIndex) {
                Color.parseColor("#FF9800")
            } else {
                Color.parseColor("#555555")
            }
            editable.setSpan(
                BackgroundColorSpan(color),
                match.first,
                match.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun updateSearchHighlights() {
        if (searchPattern != null && searchMatches.isNotEmpty()) {
            val query = searchPattern?.pattern() ?: return
            search(query.removePrefix("\\Q").removeSuffix("\\E"))
        }
    }

    private fun clearSearchHighlights() {
        val editable = text as? Spannable ?: return
        val spans = editable.getSpans(0, editable.length, BackgroundColorSpan::class.java)
        spans.forEach { editable.removeSpan(it) }
    }

    // ==================== ПОДСВЕТКА СКОБОК ====================

    private fun updateBracketMatching() {
        matchingBracketStart = -1
        matchingBracketEnd = -1

        val pos = selectionStart
        val text = text?.toString() ?: return
        if (pos <= 0 || pos > text.length) return

        val charBefore = if (pos > 0) text[pos - 1] else ' '
        val charAfter = if (pos < text.length) text[pos] else ' '

        when {
            charBefore in OPEN_BRACKETS -> {
                matchingBracketStart = pos - 1
                matchingBracketEnd = findMatchingBracket(text, pos - 1, true)
            }
            charBefore in CLOSE_BRACKETS -> {
                matchingBracketEnd = pos - 1
                matchingBracketStart = findMatchingBracket(text, pos - 1, false)
            }
            charAfter in OPEN_BRACKETS -> {
                matchingBracketStart = pos
                matchingBracketEnd = findMatchingBracket(text, pos, true)
            }
            charAfter in CLOSE_BRACKETS -> {
                matchingBracketEnd = pos
                matchingBracketStart = findMatchingBracket(text, pos, false)
            }
        }

        invalidate()
    }

    private fun findMatchingBracket(text: String, pos: Int, forward: Boolean): Int {
        if (pos < 0 || pos >= text.length) return -1
        
        val bracket = text[pos]
        val matchingBracket = when (bracket) {
            '(' -> ')'
            ')' -> '('
            '{' -> '}'
            '}' -> '{'
            '[' -> ']'
            ']' -> '['
            else -> return -1
        }

        var count = 1
        var i = if (forward) pos + 1 else pos - 1

        while (if (forward) i < text.length else i >= 0) {
            when (text[i]) {
                bracket -> count++
                matchingBracket -> {
                    count--
                    if (count == 0) return i
                }
            }
            i += if (forward) 1 else -1
        }

        return -1
    }

    // ==================== UNDO/REDO ====================

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false

        isUndoRedo = true
        try {
            val change = undoStack.pop()
            redoStack.push(change)
            setText(change.oldText)
            val textLength = text?.length ?: 0
            if (textLength > 0) {
                val newPos = change.cursorPosition.coerceIn(0, textLength)
                post { setSelection(newPos) }
            }
        } finally {
            isUndoRedo = false
        }
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false

        isUndoRedo = true
        try {
            val change = redoStack.pop()
            undoStack.push(change)
            setText(change.newText)
            val textLength = text?.length ?: 0
            if (textLength > 0) {
                val newPos = change.cursorPosition.coerceIn(0, textLength)
                post { setSelection(newPos) }
            }
        } finally {
            isUndoRedo = false
        }
        return true
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
    }

    // ==================== ФОРМАТИРОВАНИЕ ====================

    fun formatCode() {
        val code = text?.toString() ?: return
        val formatted = StringBuilder()
        var indent = 0
        val indentStr = "    "

        for (line in code.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                formatted.appendLine()
                continue
            }

            // Уменьшаем отступ для закрывающих скобок
            if (trimmed.startsWith("}") || trimmed.startsWith(")") || trimmed.startsWith("]")) {
                indent = maxOf(0, indent - 1)
            }

            formatted.append(indentStr.repeat(indent))
            formatted.appendLine(trimmed)

            // Увеличиваем отступ после открывающих скобок
            val opens = trimmed.count { it == '{' || it == '(' && trimmed.endsWith("{") }
            val closes = trimmed.count { it == '}' }
            indent = maxOf(0, indent + opens - closes)
        }

        setText(formatted.toString().trimEnd())
    }

    // ==================== ВСТАВКА СНИППЕТОВ ====================

    fun insertSnippet(snippet: String, cursorOffset: Int = 0) {
        val start = selectionStart
        val end = selectionEnd
        text?.replace(start, end, snippet)
        if (cursorOffset != 0) {
            setSelection(start + snippet.length + cursorOffset)
        }
    }

    fun insertAtCursor(textToInsert: String) {
        val start = selectionStart
        text?.insert(start, textToInsert)
        setSelection(start + textToInsert.length)
    }

    companion object {
        private const val LINE_NUMBER_PADDING = 16
        private const val CONTENT_PADDING = 12
        private const val DEFAULT_MAX_UNDO_STACK = 50
        private val OPEN_BRACKETS = charArrayOf('(', '{', '[')
        private val CLOSE_BRACKETS = charArrayOf(')', '}', ']')
    }

    var maxUndoStack: Int = DEFAULT_MAX_UNDO_STACK
}
