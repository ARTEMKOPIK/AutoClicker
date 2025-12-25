package com.autoclicker.app.util

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.autoclicker.app.R

/**
 * Помощник автодополнения для редактора скриптов
 */
class AutoCompleteHelper(
    private val context: Context,
    private val editText: EditText
) {
    private var popupWindow: PopupWindow? = null
    private var currentWord = ""
    private var wordStart = 0

    private val suggestions = listOf(
        Suggestion("click", "click(x, y)", "Клик по координатам"),
        Suggestion("longClick", "longClick(x, y)", "Долгий клик"),
        Suggestion("longClick", "longClick(x, y, duration)", "Долгий клик с длительностью"),
        Suggestion("tap", "tap(x, y, count)", "Множественный тап"),
        Suggestion("tap", "tap(x, y, count, delay)", "Множественный тап с задержкой"),
        Suggestion("swipe", "swipe(x1, y1, x2, y2)", "Свайп между точками"),
        Suggestion("swipe", "swipe(x1, y1, x2, y2, duration)", "Свайп с длительностью"),
        Suggestion("back", "back()", "Кнопка Назад"),
        Suggestion("home", "home()", "Кнопка Домой"),
        Suggestion("recents", "recents()", "Недавние приложения"),
        Suggestion("sleep", "sleep(ms)", "Задержка в миллисекундах"),
        Suggestion("waitForColor", "waitForColor(x, y, color, timeout)", "Ждать появления цвета"),
        Suggestion("waitForText", "waitForText(x1, y1, x2, y2, \"text\", timeout)", "Ждать появления текста"),
        Suggestion("getText", "getText(x1, y1, x2, y2)", "OCR распознавание текста"),
        Suggestion("getColor", "getColor(x, y)", "Получить цвет пикселя"),
        Suggestion("compareColor", "compareColor(x, y, color)", "Сравнить цвет"),
        Suggestion("compareColor", "compareColor(x, y, color, tolerance)", "Сравнить цвет с допуском"),
        Suggestion("random", "random(min, max)", "Случайное число"),
        Suggestion("log", "log(\"text\")", "Вывод в лог"),
        Suggestion("toast", "toast(\"text\")", "Показать уведомление"),
        Suggestion("vibrate", "vibrate(ms)", "Вибрация"),
        Suggestion("sendTelegram", "sendTelegram(\"text\")", "Отправка в Telegram"),
        Suggestion("pushToCb", "pushToCb(\"text\")", "Копировать в буфер обмена"),
        Suggestion("screenshot", "screenshot()", "Сделать скриншот"),
        Suggestion("while", "while (!EXIT) {\n    \n}", "Цикл while"),
        Suggestion("if", "if (condition) {\n    \n}", "Условие if"),
        Suggestion("fun", "fun name() {\n    \n}", "Объявление функции"),
        Suggestion("EXIT", "EXIT = true", "Остановить скрипт"),
        Suggestion("setVar", "setVar(\"key\", value)", "Сохранить глобальную переменную"),
        Suggestion("getVar", "getVar(\"key\")", "Получить глобальную переменную"),
        Suggestion("incVar", "incVar(\"key\")", "Увеличить переменную на 1"),
        Suggestion("decVar", "decVar(\"key\")", "Уменьшить переменную на 1")
    )

    data class Suggestion(
        val keyword: String,
        val template: String,
        val description: String
    )

    fun showSuggestions(cursorPosition: Int) {
        val text = editText.text?.toString() ?: return
        
        wordStart = findWordStart(text, cursorPosition)
        currentWord = text.substring(wordStart, cursorPosition)
        
        if (currentWord.length < 2) {
            dismiss()
            return
        }

        val filtered = suggestions.filter { 
            it.keyword.startsWith(currentWord, ignoreCase = true) && 
            it.keyword != currentWord 
        }

        if (filtered.isEmpty()) {
            dismiss()
            return
        }

        showPopup(filtered)
    }

    private fun findWordStart(text: String, position: Int): Int {
        if (position <= 0) return 0
        var start = position - 1
        while (start >= 0 && isWordChar(text[start])) {
            start--
        }
        return (start + 1).coerceIn(0, text.length)
    }

    private fun isWordChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '_'
    }

    private fun showPopup(filtered: List<Suggestion>) {
        dismiss()

        val layout = editText.layout ?: return
        val selectionStart = editText.selectionStart
        val textLength = editText.text?.length ?: 0
        if (selectionStart < 0 || selectionStart > textLength) return
        if (layout.lineCount == 0) return
        if (!editText.isAttachedToWindow) return

        val backgroundColor = ContextCompat.getColor(context, R.color.background_card)
        val dividerColor = ContextCompat.getColor(context, R.color.background_dark)

        val listView = ListView(context).apply {
            adapter = SuggestionAdapter(context, filtered)
            divider = ColorDrawable(dividerColor)
            dividerHeight = 1
            setBackgroundColor(backgroundColor)
            setOnItemClickListener { _, _, position, _ ->
                insertSuggestion(filtered[position])
                dismiss()
            }
        }

        popupWindow = PopupWindow(
            listView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 8f
        }

        try {
            val lineCount = layout.lineCount
            if (lineCount == 0) {
                dismiss()
                return
            }
            
            val line = layout.getLineForOffset(selectionStart).coerceIn(0, lineCount - 1)
            val cursorX = layout.getPrimaryHorizontal(selectionStart).toInt()
            val lineBottom = layout.getLineBottom(line)

            val location = IntArray(2)
            editText.getLocationOnScreen(location)
            
            val screenWidth = context.resources.displayMetrics.widthPixels
            val screenHeight = context.resources.displayMetrics.heightPixels
            
            // Динамический расчёт ширины popup
            val popupWidth = (screenWidth * 0.6).toInt().coerceIn(200, 350)
            val popupHeight = (context.resources.displayMetrics.density * 200).toInt() // Оценочная высота
            
            var popupX = cursorX + editText.paddingLeft
            var popupY = lineBottom - editText.scrollY + editText.paddingTop
            
            // Проверяем выход за правый край экрана
            if (location[0] + popupX + popupWidth > screenWidth) {
                popupX = screenWidth - location[0] - popupWidth - 16
            }
            // Проверяем выход за левый край
            if (location[0] + popupX < 0) {
                popupX = -location[0] + 16
            }
            
            // Проверяем наличие места внизу
            val absoluteY = location[1] + popupY
            if (absoluteY + popupHeight > screenHeight) {
                // Показываем сверху если снизу нет места
                popupY -= popupHeight + layout.getLineBottom(line) - layout.getLineTop(line) + 16
            }
            
            if (editText.isAttachedToWindow && editText.windowToken != null) {
                popupWindow?.width = popupWidth
                popupWindow?.height = WindowManager.LayoutParams.WRAP_CONTENT
                popupWindow?.showAsDropDown(editText, popupX, popupY - editText.height)
            }
        } catch (e: Exception) {
            dismiss()
        }
    }

    private fun insertSuggestion(suggestion: Suggestion) {
        val text = editText.text ?: return
        val cursorPos = editText.selectionStart
        if (cursorPos < 0) return
        
        try {
            text.replace(wordStart, cursorPos, suggestion.template)
            
            val parenIndex = suggestion.template.indexOf('(')
            if (parenIndex >= 0) {
                val newCursorPos = wordStart + parenIndex + 1
                if (newCursorPos <= text.length) {
                    editText.setSelection(newCursorPos)
                }
            }
        } catch (e: Exception) {
            com.autoclicker.app.util.CrashHandler.logError("AutoComplete", "Failed to insert suggestion", e)
        }
    }

    fun cleanup() {
        dismiss()
    }

    fun dismiss() {
        try {
            popupWindow?.dismiss()
        } catch (e: Exception) {
            // ignore
        }
        popupWindow = null
    }

    private class SuggestionAdapter(
        context: Context,
        private val items: List<Suggestion>
    ) : ArrayAdapter<Suggestion>(context, 0, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_autocomplete, parent, false)

            val item = items[position]
            view.findViewById<TextView>(R.id.tvKeyword).text = item.keyword
            view.findViewById<TextView>(R.id.tvDescription).text = item.description

            return view
        }
    }
}
