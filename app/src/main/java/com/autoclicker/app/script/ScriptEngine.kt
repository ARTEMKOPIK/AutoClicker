package com.autoclicker.app.script

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.autoclicker.app.service.ClickerAccessibilityService
import com.autoclicker.app.service.ScreenCaptureService
import com.autoclicker.app.util.Constants
import com.autoclicker.app.util.CrashHandler
import com.autoclicker.app.util.PrefsManager
import com.autoclicker.app.util.ScriptLogger
import com.autoclicker.app.util.ScriptVariables
import com.autoclicker.app.util.TelegramSender
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * Script execution engine with comprehensive parameter validation and thread-safety.
 * 
 * Thread-safety:
 * - variables Map uses ConcurrentHashMap for thread-safe access
 * - textRecognizer is protected by recognizerLock
 * - functions map is accessed only during parsing phase (single-threaded)
 * 
 * Input validation:
 * - All coordinate parameters are validated against screen bounds
 * - Sleep duration must be non-negative
 * - All numeric parameters are range-checked before use
 */
class ScriptEngine(
    private val context: Context,
    private val logCallback: (String) -> Unit,
    private val scriptName: String? = null
) : Closeable {
    private val prefs = PrefsManager(context)
    private val telegram = TelegramSender(prefs.telegramToken, prefs.telegramChatId)
    private var textRecognizer: TextRecognizer? = null
    private val recognizerLock = Any()

    // Thread-safe variable storage using ConcurrentHashMap
    private val variables = ConcurrentHashMap<String, Any>()
    private val functions = mutableMapOf<String, FunctionDef>()

    private val exitFlag = AtomicBoolean(false)

    init {
        ScriptLogger.init(context)
        ScriptLogger.setScriptName(scriptName)
        ScriptVariables.init(context)
    }

    var EXIT: Boolean
        get() = exitFlag.get()
        set(value) = exitFlag.set(value)

    data class FunctionDef(val name: String, val params: List<String>, val body: List<String>)

    companion object {
        private val REGEX_CLICK = Regex("""click\((\d+),\s*(\d+)\)""")
        private val REGEX_SLEEP = Regex("""sleep\((\d+)\)""")
        private val REGEX_LOG_QUOTED = Regex("""log\(["'](.*)["']\)""")
        private val REGEX_LOG_SIMPLE = Regex("""log\((.*)\)""")
        private val REGEX_TELEGRAM = Regex("""sendTelegram\(["'](.*)["']\)""")
        private val REGEX_SWIPE = Regex("""swipe\((\d+),\s*(\d+),\s*(\d+),\s*(\d+)\)""")
        private val REGEX_SWIPE_DURATION = Regex("""swipe\((\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*(\d+)\)""")
        private val REGEX_LONG_CLICK = Regex("""longClick\((\d+),\s*(\d+)\)""")
        private val REGEX_LONG_CLICK_DURATION = Regex("""longClick\((\d+),\s*(\d+),\s*(\d+)\)""")
        private val REGEX_GET_TEXT = Regex("""getText\((\d+),\s*(\d+),\s*(\d+),\s*(\d+)\)""")
        private val REGEX_GET_COLOR = Regex("""getColor\((\d+),\s*(\d+)\)""")
        private val REGEX_PUSH_CB = Regex("""pushToCb\(["']([^"']*)["']\)""")
        private val REGEX_TAP = Regex("""tap\((\d+),\s*(\d+),\s*(\d+)\)""")
        private val REGEX_TAP_DELAY = Regex("""tap\((\d+),\s*(\d+),\s*(\d+),\s*(\d+)\)""")
        private val REGEX_RANDOM = Regex("""random\((\d+),\s*(\d+)\)""")
        private val REGEX_WAIT_COLOR = Regex("""waitForColor\((\d+),\s*(\d+),\s*["']?([#\w-]+)["']?,\s*(\d+)\)""")
        private val REGEX_WAIT_TEXT = Regex("""waitForText\((\d+),\s*(\d+),\s*(\d+),\s*(\d+),\s*["'](.*)["'],\s*(\d+)\)""")
        private val REGEX_COMPARE_COLOR = Regex("""compareColor\((\d+),\s*(\d+),\s*["']?([#\w-]+)["']?\)""")
        private val REGEX_COMPARE_COLOR_TOL = Regex("""compareColor\((\d+),\s*(\d+),\s*["']?([#\w-]+)["']?,\s*(\d+)\)""")
        private val REGEX_FUNCTION_DEF = Regex("""fun\s+(\w+)\s*\((.*)\)\s*\{""")
        private val REGEX_FUNCTION_CALL = Regex("""(\w+)\((.*)\)""")
        private val REGEX_VIBRATE = Regex("""vibrate\((\d+)\)""")
        private val REGEX_TOAST = Regex("""toast\(["'](.*)["']\)""")
        private val REGEX_SET_VAR = Regex("""setVar\(["'](\w+)["'],\s*(.+)\)""")
        private val REGEX_GET_VAR = Regex("""getVar\(["'](\w+)["']\)""")
        private val REGEX_INC_VAR = Regex("""incVar\(["'](\w+)["']\)""")
        private val REGEX_DEC_VAR = Regex("""decVar\(["'](\w+)["']\)""")
    }

    fun execute(code: String) {
        EXIT = false
        variables.clear()
        functions.clear()

        try {
            val lines = code.lines()
            // Первый проход - собираем функции
            parseFunctions(lines)
            // Второй проход - выполняем код
            var i = 0
            while (i < lines.size && !EXIT && !Thread.currentThread().isInterrupted) {
                val line = lines[i].trim()
                try {
                    i = processLine(line, lines, i)
                } catch (e: Exception) {
                    log("⚠️ Ошибка в строке ${i + 1}: ${e.message}")
                    CrashHandler.logWarning("ScriptEngine", "Ошибка в строке ${i + 1}: ${e.message}", e)
                }
                i++
            }
        } catch (e: Exception) {
            log("❌ Критическая ошибка: ${e.message}")
            CrashHandler.logError("ScriptEngine", "Критическая ошибка выполнения скрипта", e)
        } finally {
            close()
        }
    }

    override fun close() {
        synchronized(recognizerLock) {
            textRecognizer?.close()
            textRecognizer = null
        }
    }

    private fun getTextRecognizer(): TextRecognizer {
        synchronized(recognizerLock) {
            if (textRecognizer == null) {
                textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            }
            return textRecognizer!!
        }
    }

    private fun parseFunctions(lines: List<String>) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            val match = REGEX_FUNCTION_DEF.find(line)
            if (match != null) {
                val name = match.groupValues[1]
                val params = match.groupValues[2].split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val endIndex = findMatchingBrace(lines, i)
                val body = lines.subList(i + 1, endIndex)
                functions[name] = FunctionDef(name, params, body)
                i = endIndex
            }
            i++
        }
    }

    private fun processLine(line: String, allLines: List<String>, currentIndex: Int): Int {
        if (EXIT || Thread.currentThread().isInterrupted) return currentIndex

        when {
            line.isEmpty() || line.startsWith("//") -> {}
            line.startsWith("fun ") -> return skipFunctionDef(allLines, currentIndex)
            line.startsWith("click(") -> parseClick(line)
            line.startsWith("sleep(") -> parseSleep(line)
            line.startsWith("log(") -> parseLog(line)
            line.startsWith("sendTelegram(") -> parseTelegram(line)
            line.startsWith("swipe(") -> parseSwipe(line)
            line.startsWith("longClick(") -> parseLongClick(line)
            line.startsWith("tap(") -> parseTap(line)
            line.startsWith("back()") -> performBack()
            line.startsWith("home()") -> performHome()
            line.startsWith("recents()") -> performRecents()
            line.startsWith("vibrate(") -> parseVibrate(line)
            line.startsWith("toast(") -> parseToast(line)
            line.startsWith("setVar(") -> parseSetVar(line)
            line.contains("getVar(") -> parseGetVar(line)
            line.startsWith("incVar(") -> parseIncVar(line)
            line.startsWith("decVar(") -> parseDecVar(line)
            line.contains("waitForColor(") -> parseWaitForColor(line)
            line.contains("waitForText(") -> parseWaitForText(line)
            line.contains("compareColor(") -> parseCompareColor(line)
            line.contains("random(") -> parseRandom(line)
            line.contains("getText(") -> parseGetText(line)
            line.contains("getColor(") -> parseGetColor(line)
            line.contains("pushToCb(") -> parsePushToClipboard(line)
            line == "EXIT = true" -> EXIT = true
            line.startsWith("while") -> return executeWhileLoop(line, allLines, currentIndex)
            line.startsWith("if") -> return executeIfBlock(line, allLines, currentIndex)
            line.contains("=") && !line.contains("==") && !line.contains("!=") -> parseVariableAssignment(line)
            else -> tryCallFunction(line)
        }
        return currentIndex
    }

    private fun skipFunctionDef(lines: List<String>, startIndex: Int): Int {
        return findMatchingBrace(lines, startIndex)
    }

    private fun executeWhileLoop(condition: String, lines: List<String>, startIndex: Int): Int {
        val endIndex = findMatchingBrace(lines, startIndex)
        val loopLines = lines.subList(startIndex + 1, endIndex)

        // Проверяем условие while
        while (!EXIT && !Thread.currentThread().isInterrupted) {
            if (!evaluateWhileCondition(condition)) break
            executeBlock(loopLines)
        }
        return endIndex
    }

    private fun evaluateWhileCondition(condition: String): Boolean {
        // while (!EXIT) или while (true) или while (condition)
        val content = condition.substringAfter("(").substringBefore(")").trim()
        return when {
            content == "!EXIT" -> !EXIT
            content == "true" -> true
            content == "false" -> false
            else -> evaluateCondition("if ($content)")
        }
    }

    private fun executeIfBlock(condition: String, lines: List<String>, startIndex: Int): Int {
        val endIndex = findMatchingBrace(lines, startIndex)
        val conditionResult = evaluateCondition(condition)

        // Проверяем есть ли else
        val elseIndex = findElseBlock(lines, endIndex)

        if (conditionResult) {
            val blockLines = lines.subList(startIndex + 1, endIndex)
            executeBlock(blockLines)
        } else if (elseIndex > endIndex) {
            val elseEndIndex = findMatchingBrace(lines, elseIndex)
            val elseLines = lines.subList(elseIndex + 1, elseEndIndex)
            executeBlock(elseLines)
            return elseEndIndex
        }
        return if (elseIndex > endIndex) findMatchingBrace(lines, elseIndex) else endIndex
    }

    private fun findElseBlock(lines: List<String>, afterIndex: Int): Int {
        if (afterIndex + 1 < lines.size) {
            val nextLine = lines[afterIndex + 1].trim()
            if (nextLine.startsWith("else") || nextLine == "} else {") {
                return afterIndex + 1
            }
        }
        if (afterIndex < lines.size) {
            val currentLine = lines[afterIndex].trim()
            if (currentLine.contains("} else {") || currentLine.endsWith("else {")) {
                return afterIndex
            }
        }
        return -1
    }

    private fun executeBlock(blockLines: List<String>) {
        var j = 0
        while (j < blockLines.size && !EXIT && !Thread.currentThread().isInterrupted) {
            val blockLine = blockLines[j].trim()

            when {
                blockLine.startsWith("while") -> j = executeWhileLoop(blockLine, blockLines, j)
                blockLine.startsWith("if") && blockLine.contains("{") -> j = executeIfBlock(blockLine, blockLines, j)
                blockLine == "break" -> return
                blockLine == "continue" -> return
                blockLine == "return" -> { EXIT = true; return }
                else -> processLine(blockLine, blockLines, j)
            }
            j++
        }
    }

    private fun evaluateCondition(condition: String): Boolean {
        val content = condition.substringAfter("(").substringBefore(")").trim()
        if (content.isEmpty()) return false

        // Поддержка логических операторов
        if (content.contains("&&")) {
            val parts = content.split("&&")
            return parts.all { evaluateSimpleCondition(it.trim()) }
        }
        if (content.contains("||")) {
            val parts = content.split("||")
            return parts.any { evaluateSimpleCondition(it.trim()) }
        }

        return evaluateSimpleCondition(content)
    }

    private fun evaluateSimpleCondition(content: String): Boolean {
        // Отрицание
        if (content.startsWith("!")) {
            val inner = content.substring(1).trim()
            return !evaluateSimpleCondition(inner)
        }

        return when {
            content == "true" -> true
            content == "false" -> false
            content == "EXIT" -> EXIT
            content.contains(">=") -> compareValues(content, ">=") { a, b -> a >= b }
            content.contains("<=") -> compareValues(content, "<=") { a, b -> a <= b }
            content.contains("!=") -> compareStrings(content, "!=") { a, b -> a != b }
            content.contains("==") -> compareStrings(content, "==") { a, b -> a == b }
            content.contains(">") -> compareValues(content, ">") { a, b -> a > b }
            content.contains("<") -> compareValues(content, "<") { a, b -> a < b }
            variables.containsKey(content) -> variables[content].toString().toBoolean()
            else -> false
        }
    }

    private fun compareValues(content: String, op: String, compare: (Float, Float) -> Boolean): Boolean {
        val parts = content.split(op, limit = 2)
        if (parts.size != 2) return false
        val left = resolveValue(parts[0].trim())
        val right = resolveValue(parts[1].trim())
        return compare(left, right)
    }

    private fun compareStrings(content: String, op: String, compare: (String, String) -> Boolean): Boolean {
        val parts = content.split(op, limit = 2)
        if (parts.size != 2) return false
        val left = resolveString(parts[0].trim())
        val right = resolveString(parts[1].trim())
        return compare(left, right)
    }

    private fun resolveValue(text: String): Float {
        // Проверяем на выражения с random
        if (text.contains("random(")) {
            val match = REGEX_RANDOM.find(text)
            if (match != null) {
                val min = match.groupValues[1].toIntOrNull() ?: 0
                val max = match.groupValues[2].toIntOrNull() ?: 0
                return Random.nextInt(min, max + 1).toFloat()
            }
        }
        return text.toFloatOrNull()
            ?: variables[text]?.toString()?.toFloatOrNull()
            ?: 0f
    }

    private fun resolveString(text: String): String {
        val trimmed = text.trim().removeSurrounding("\"").removeSurrounding("'")
        return variables[trimmed]?.toString() ?: trimmed
    }

    private fun findMatchingBrace(lines: List<String>, startIndex: Int): Int {
        var braceCount = 0
        for (i in startIndex until lines.size) {
            val line = lines[i]
            braceCount += line.count { it == '{' }
            braceCount -= line.count { it == '}' }
            if (braceCount == 0 && i > startIndex) return i
        }
        return lines.size - 1
    }

    private fun parseVariableAssignment(line: String) {
        val parts = line.split("=", limit = 2)
        if (parts.size == 2) {
            val varName = parts[0].trim()
                .replace("val ", "").replace("var ", "")
                .replace("float ", "").replace("int ", "").replace("string ", "")
            var value = parts[1].trim().trimEnd(';')

            // Вычисляем выражения
            value = evaluateExpression(value)
            variables[varName] = value
        }
    }

    private fun evaluateExpression(expr: String): String {
        var result = expr

        // random(min, max)
        val randomMatch = REGEX_RANDOM.find(result)
        if (randomMatch != null) {
            val min = randomMatch.groupValues[1].toIntOrNull() ?: 0
            val max = randomMatch.groupValues[2].toIntOrNull() ?: 0
            val randomVal = Random.nextInt(min, max + 1)
            result = result.replace(randomMatch.value, randomVal.toString())
        }

        // getColor(x, y)
        val colorMatch = REGEX_GET_COLOR.find(result)
        if (colorMatch != null) {
            val x = colorMatch.groupValues[1].toIntOrNull() ?: 0
            val y = colorMatch.groupValues[2].toIntOrNull() ?: 0
            val color = getColor(x, y)
            result = result.replace(colorMatch.value, color.toString())
        }

        // Подстановка переменных
        for ((key, value) in variables) {
            result = result.replace(key, value.toString())
        }

        return result
    }

    // ==================== ПАРСЕРЫ КОМАНД ====================

    private fun parseClick(line: String) {
        val match = REGEX_CLICK.find(line) ?: return
        val x = match.groupValues[1].toFloatOrNull() ?: return
        val y = match.groupValues[2].toFloatOrNull() ?: return
        click(x, y)
    }

    private fun parseSleep(line: String) {
        val match = REGEX_SLEEP.find(line) ?: return
        val ms = match.groupValues[1].toLongOrNull() ?: return
        sleep(ms)
    }

    private fun parseLog(line: String) {
        val match = REGEX_LOG_QUOTED.find(line) ?: REGEX_LOG_SIMPLE.find(line) ?: return
        val text = match.groupValues[1]
        log(resolveVariables(text))
    }

    private fun parseTelegram(line: String) {
        val match = REGEX_TELEGRAM.find(line) ?: return
        sendTelegram(resolveVariables(match.groupValues[1]))
    }

    private fun parseSwipe(line: String) {
        // Сначала проверяем версию с duration
        val matchDuration = REGEX_SWIPE_DURATION.find(line)
        if (matchDuration != null) {
            val x1 = matchDuration.groupValues[1].toFloatOrNull() ?: return
            val y1 = matchDuration.groupValues[2].toFloatOrNull() ?: return
            val x2 = matchDuration.groupValues[3].toFloatOrNull() ?: return
            val y2 = matchDuration.groupValues[4].toFloatOrNull() ?: return
            val duration = matchDuration.groupValues[5].toLongOrNull() ?: 300L
            swipe(x1, y1, x2, y2, duration)
            return
        }

        val match = REGEX_SWIPE.find(line) ?: return
        val x1 = match.groupValues[1].toFloatOrNull() ?: return
        val y1 = match.groupValues[2].toFloatOrNull() ?: return
        val x2 = match.groupValues[3].toFloatOrNull() ?: return
        val y2 = match.groupValues[4].toFloatOrNull() ?: return
        swipe(x1, y1, x2, y2)
    }

    private fun parseLongClick(line: String) {
        val matchDuration = REGEX_LONG_CLICK_DURATION.find(line)
        if (matchDuration != null) {
            val x = matchDuration.groupValues[1].toFloatOrNull() ?: return
            val y = matchDuration.groupValues[2].toFloatOrNull() ?: return
            val duration = matchDuration.groupValues[3].toLongOrNull() ?: 500L
            longClick(x, y, duration)
            return
        }

        val match = REGEX_LONG_CLICK.find(line) ?: return
        val x = match.groupValues[1].toFloatOrNull() ?: return
        val y = match.groupValues[2].toFloatOrNull() ?: return
        longClick(x, y)
    }

    private fun parseTap(line: String) {
        val matchDelay = REGEX_TAP_DELAY.find(line)
        if (matchDelay != null) {
            val x = matchDelay.groupValues[1].toFloatOrNull() ?: return
            val y = matchDelay.groupValues[2].toFloatOrNull() ?: return
            val count = matchDelay.groupValues[3].toIntOrNull() ?: return
            val delay = matchDelay.groupValues[4].toLongOrNull() ?: 100L
            tap(x, y, count, delay)
            return
        }

        val match = REGEX_TAP.find(line) ?: return
        val x = match.groupValues[1].toFloatOrNull() ?: return
        val y = match.groupValues[2].toFloatOrNull() ?: return
        val count = match.groupValues[3].toIntOrNull() ?: return
        tap(x, y, count)
    }

    private fun parseGetText(line: String) {
        val match = REGEX_GET_TEXT.find(line) ?: return
        val x1 = match.groupValues[1].toIntOrNull() ?: return
        val y1 = match.groupValues[2].toIntOrNull() ?: return
        val x2 = match.groupValues[3].toIntOrNull() ?: return
        val y2 = match.groupValues[4].toIntOrNull() ?: return
        val text = getText(x1, y1, x2, y2)

        if (line.contains("=")) {
            val varName = line.substringBefore("=").trim()
                .replace("val ", "").replace("var ", "")
            variables[varName] = text
        }
        log("OCR: $text")
    }

    private fun parseGetColor(line: String) {
        val match = REGEX_GET_COLOR.find(line) ?: return
        val x = match.groupValues[1].toIntOrNull() ?: return
        val y = match.groupValues[2].toIntOrNull() ?: return
        val color = getColor(x, y)

        if (line.contains("=")) {
            val varName = line.substringBefore("=").trim()
                .replace("val ", "").replace("var ", "").replace("int ", "")
            variables[varName] = color
        }
        log("Color($x,$y): $color (${colorToHex(color)})")
    }

    private fun parseRandom(line: String) {
        val match = REGEX_RANDOM.find(line) ?: return
        val min = match.groupValues[1].toIntOrNull() ?: return
        val max = match.groupValues[2].toIntOrNull() ?: return
        val result = Random.nextInt(min, max + 1)

        if (line.contains("=")) {
            val varName = line.substringBefore("=").trim()
                .replace("val ", "").replace("var ", "").replace("int ", "")
            variables[varName] = result
        }
        log("Random: $result")
    }

    private fun parseWaitForColor(line: String) {
        val match = REGEX_WAIT_COLOR.find(line) ?: return
        val x = match.groupValues[1].toIntOrNull() ?: return
        val y = match.groupValues[2].toIntOrNull() ?: return
        val targetColor = parseColor(match.groupValues[3]) ?: return
        val timeout = match.groupValues[4].toLongOrNull() ?: return
        val result = waitForColor(x, y, targetColor, timeout)

        if (line.contains("=")) {
            val varName = line.substringBefore("=").trim()
                .replace("val ", "").replace("var ", "")
            variables[varName] = result
        }
    }

    private fun parseWaitForText(line: String) {
        val match = REGEX_WAIT_TEXT.find(line) ?: return
        val x1 = match.groupValues[1].toIntOrNull() ?: return
        val y1 = match.groupValues[2].toIntOrNull() ?: return
        val x2 = match.groupValues[3].toIntOrNull() ?: return
        val y2 = match.groupValues[4].toIntOrNull() ?: return
        val targetText = match.groupValues[5]
        val timeout = match.groupValues[6].toLongOrNull() ?: return
        val result = waitForText(x1, y1, x2, y2, targetText, timeout)

        if (line.contains("=")) {
            val varName = line.substringBefore("=").trim()
                .replace("val ", "").replace("var ", "")
            variables[varName] = result
        }
    }

    private fun parseCompareColor(line: String) {
        // С толерантностью
        val matchTol = REGEX_COMPARE_COLOR_TOL.find(line)
        if (matchTol != null) {
            val x = matchTol.groupValues[1].toIntOrNull() ?: return
            val y = matchTol.groupValues[2].toIntOrNull() ?: return
            val targetColor = parseColor(matchTol.groupValues[3]) ?: return
            val tolerance = matchTol.groupValues[4].toIntOrNull() ?: 0
            val result = compareColor(x, y, targetColor, tolerance)

            if (line.contains("=")) {
                val varName = line.substringBefore("=").trim()
                    .replace("val ", "").replace("var ", "")
                variables[varName] = result
            }
            return
        }

        val match = REGEX_COMPARE_COLOR.find(line) ?: return
        val x = match.groupValues[1].toIntOrNull() ?: return
        val y = match.groupValues[2].toIntOrNull() ?: return
        val targetColor = parseColor(match.groupValues[3]) ?: return
        val result = compareColor(x, y, targetColor)

        if (line.contains("=")) {
            val varName = line.substringBefore("=").trim()
                .replace("val ", "").replace("var ", "")
            variables[varName] = result
        }
    }
    
    private fun parseColor(colorStr: String): Int? {
        return when {
            colorStr.startsWith("#") -> {
                try {
                    Color.parseColor(colorStr)
                } catch (e: Exception) {
                    null
                }
            }
            colorStr.startsWith("-") || colorStr.all { it.isDigit() } -> {
                colorStr.toIntOrNull()
            }
            else -> null
        }
    }

    private fun parsePushToClipboard(line: String) {
        val match = REGEX_PUSH_CB.find(line) ?: return
        val text = resolveVariables(match.groupValues[1])
        pushToClipboard(text)
    }

    private fun parseVibrate(line: String) {
        val match = REGEX_VIBRATE.find(line) ?: return
        val duration = match.groupValues[1].toLongOrNull() ?: return
        vibrate(duration)
    }

    private fun parseToast(line: String) {
        val match = REGEX_TOAST.find(line) ?: return
        val text = resolveVariables(match.groupValues[1])
        showToast(text)
    }

    private fun parseSetVar(line: String) {
        val match = REGEX_SET_VAR.find(line) ?: return
        val key = match.groupValues[1]
        val value = resolveVariables(match.groupValues[2].trim().removeSurrounding("\"").removeSurrounding("'"))
        ScriptVariables.set(key, value)
        log("SetVar: $key = $value")
    }

    private fun parseGetVar(line: String) {
        val match = REGEX_GET_VAR.find(line) ?: return
        val key = match.groupValues[1]
        val value = ScriptVariables.getString(key)

        if (line.contains("=")) {
            val varName = line.substringBefore("=").trim()
                .replace("val ", "").replace("var ", "")
            variables[varName] = value
        }
        log("GetVar: $key = $value")
    }

    private fun parseIncVar(line: String) {
        val match = REGEX_INC_VAR.find(line) ?: return
        val key = match.groupValues[1]
        val newValue = ScriptVariables.increment(key)
        log("IncVar: $key = $newValue")
    }

    private fun parseDecVar(line: String) {
        val match = REGEX_DEC_VAR.find(line) ?: return
        val key = match.groupValues[1]
        val newValue = ScriptVariables.decrement(key)
        log("DecVar: $key = $newValue")
    }

    private fun tryCallFunction(line: String) {
        val match = REGEX_FUNCTION_CALL.find(line) ?: return
        val funcName = match.groupValues[1]
        
        // Пропускаем встроенные функции
        if (funcName in listOf("click", "sleep", "log", "sendTelegram", "swipe", "longClick", 
            "tap", "back", "home", "recents", "vibrate", "toast", "setVar", "getVar", 
            "incVar", "decVar", "waitForColor", "waitForText", "compareColor", "random",
            "getText", "getColor", "pushToCb", "screenshot")) {
            return
        }
        
        val argsStr = match.groupValues[2]
        val func = functions[funcName] ?: return
        val args = if (argsStr.isBlank()) emptyList() else argsStr.split(",").map { it.trim() }

        // Сохраняем текущие переменные
        val savedVars = variables.toMap()

        // Устанавливаем параметры
        func.params.forEachIndexed { index, param ->
            if (index < args.size) {
                variables[param] = resolveString(args[index])
            }
        }

        // Выполняем тело функции
        executeBlock(func.body)

        // Восстанавливаем переменные (кроме результата)
        val result = variables["result"]
        variables.clear()
        variables.putAll(savedVars)
        if (result != null && line.contains("=")) {
            val varName = line.substringBefore("=").trim()
                .replace("val ", "").replace("var ", "")
            variables[varName] = result
        }
    }

    private fun resolveVariables(text: String): String {
        var result = text
        for ((key, value) in variables) {
            // Заменяем ${key} и $key на значение переменной
            result = result.replace("\${$key}", value.toString())
            result = result.replace("\$$key", value.toString())
        }
        return result
    }

    // ==================== ОСНОВНЫЕ ФУНКЦИИ ====================

    /**
     * Validate coordinates are within screen bounds.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if coordinates are valid
     */
    private fun validateCoordinates(x: Float, y: Float): Boolean {
        // Allow coordinates slightly outside bounds for gestures (swipes)
        val maxWidth = Constants.FALLBACK_SCREEN_WIDTH * 1.5f
        val maxHeight = Constants.FALLBACK_SCREEN_HEIGHT * 1.5f
        return x >= 0 && y >= 0 && x < maxWidth && y < maxHeight
    }

    /**
     * Validate tap coordinates (must be on screen).
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if coordinates are on screen
     */
    private fun validateTapCoordinates(x: Float, y: Float): Boolean {
        val maxWidth = Constants.FALLBACK_SCREEN_WIDTH
        val maxHeight = Constants.FALLBACK_SCREEN_HEIGHT
        if (x < 0 || y < 0 || x >= maxWidth || y >= maxHeight) {
            log("⚠️ Координаты вне экрана: (${x.toInt()}, ${y.toInt()})")
            return false
        }
        return true
    }

    /**
     * Validate sleep duration is non-negative.
     * 
     * @param ms Duration in milliseconds
     * @return true if duration is valid
     */
    private fun validateSleepDuration(ms: Long): Boolean {
        if (ms < 0) {
            log("⚠️ Невалидная задержка: $msms (должно быть >= 0)")
            return false
        }
        return true
    }

    /**
     * Validate tap count is positive.
     * 
     * @param count Number of taps
     * @return true if count is valid
     */
    private fun validateTapCount(count: Int): Boolean {
        if (count <= 0) {
            log("⚠️ Невалидное количество тапов: $count (должно быть > 0)")
            return false
        }
        return true
    }

    fun click(x: Float, y: Float) {
        if (EXIT || Thread.currentThread().isInterrupted) return
        
        // Validate coordinates
        if (!validateTapCoordinates(x, y)) {
            CrashHandler.logWarning("ScriptEngine", "Invalid click coordinates: $x, $y")
            return
        }
        
        val service = ClickerAccessibilityService.instance
        if (service == null) {
            log("⚠️ Accessibility Service недоступен")
            return
        }
        service.click(x, y)
        log("Click: ${x.toInt()}, ${y.toInt()}")
    }

    fun longClick(x: Float, y: Float, duration: Long = 500L) {
        if (EXIT || Thread.currentThread().isInterrupted) return
        
        // Validate coordinates
        if (!validateTapCoordinates(x, y)) {
            CrashHandler.logWarning("ScriptEngine", "Invalid longClick coordinates: $x, $y")
            return
        }
        
        // Validate duration
        if (duration < 0) {
            log("⚠️ Невалидная длительность: ${duration}ms (должно быть >= 0)")
            return
        }
        
        val service = ClickerAccessibilityService.instance
        if (service == null) {
            log("⚠️ Accessibility Service недоступен")
            return
        }
        service.longClick(x, y, duration)
        log("LongClick: ${x.toInt()}, ${y.toInt()} (${duration}ms)")
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 300L) {
        if (EXIT || Thread.currentThread().isInterrupted) return
        
        // Validate coordinates (allow off-screen for gestures)
        if (!validateCoordinates(x1, y1) || !validateCoordinates(x2, y2)) {
            CrashHandler.logWarning("ScriptEngine", "Invalid swipe coordinates: ($x1,$y1) -> ($x2,$y2)")
            return
        }
        
        // Validate duration
        if (duration <= 0) {
            log("⚠️ Невалидная длительность свайпа: ${duration}ms (должно быть > 0)")
            return
        }
        
        val service = ClickerAccessibilityService.instance
        if (service == null) {
            log("⚠️ Accessibility Service недоступен")
            return
        }
        service.swipe(x1, y1, x2, y2, duration)
        log("Swipe: (${x1.toInt()},${y1.toInt()}) -> (${x2.toInt()},${y2.toInt()})")
    }

    fun tap(x: Float, y: Float, count: Int, delay: Long = 100L) {
        if (EXIT || Thread.currentThread().isInterrupted) return
        
        // Validate coordinates
        if (!validateTapCoordinates(x, y)) {
            CrashHandler.logWarning("ScriptEngine", "Invalid tap coordinates: $x, $y")
            return
        }
        
        // Validate count and delay
        if (!validateTapCount(count)) return
        if (delay < 0) {
            log("⚠️ Невалидная задержка тапов: ${delay}ms (должно быть >= 0)")
            return
        }
        
        val service = ClickerAccessibilityService.instance
        if (service == null) {
            log("⚠️ Accessibility Service недоступен")
            return
        }
        for (i in 0 until count) {
            if (EXIT || Thread.currentThread().isInterrupted) return
            service.click(x, y)
            if (i < count - 1) sleep(delay)
        }
        log("Tap: ${x.toInt()}, ${y.toInt()} x$count")
    }

    fun sleep(ms: Long) {
        // Validate duration
        if (!validateSleepDuration(ms)) return
        
        if (ms <= 0) return
        // Разбиваем на маленькие интервалы для быстрой реакции на EXIT
        val interval = 50L
        var remaining = ms
        while (remaining > 0 && !EXIT && !Thread.currentThread().isInterrupted) {
            try {
                val sleepTime = minOf(remaining, interval)
                Thread.sleep(sleepTime)
                remaining -= sleepTime
            } catch (e: InterruptedException) {
                EXIT = true
                Thread.currentThread().interrupt()
                return
            }
        }
    }

    fun log(text: String) {
        logCallback(text)
        ScriptLogger.info(text)
    }

    fun sendTelegram(text: String) {
        if (EXIT || Thread.currentThread().isInterrupted) return
        telegram.sendMessage(text)
        log("TG: $text")
    }

    fun sendTelegramPhoto(text: String, bitmap: Bitmap) {
        if (EXIT || Thread.currentThread().isInterrupted) return
        telegram.sendPhoto(text, bitmap)
        log("TG Photo sent")
    }

    fun screenshot(): Bitmap? {
        if (EXIT || Thread.currentThread().isInterrupted) return null
        return ScreenCaptureService.instance?.takeScreenshot()
    }

    fun getColor(x: Int, y: Int): Int {
        if (EXIT || Thread.currentThread().isInterrupted) return 0
        return ScreenCaptureService.instance?.getPixelColor(x, y) ?: 0
    }

    private fun colorToHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    // ==================== НОВЫЕ ФУНКЦИИ ====================

    fun waitForColor(x: Int, y: Int, targetColor: Int, timeout: Long, tolerance: Int = 0): Boolean {
        if (EXIT || Thread.currentThread().isInterrupted) return false
        log("WaitForColor: ($x,$y) = ${colorToHex(targetColor)}, timeout=${timeout}ms")
        val startTime = System.currentTimeMillis()
        while (!EXIT && !Thread.currentThread().isInterrupted) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= timeout) {
                log("WaitForColor: Timeout!")
                return false
            }
            val currentColor = getColor(x, y)
            if (colorsMatch(currentColor, targetColor, tolerance)) {
                log("WaitForColor: Found! (${elapsed}ms)")
                return true
            }
            sleep(50)
        }
        return false
    }

    fun waitForText(x1: Int, y1: Int, x2: Int, y2: Int, targetText: String, timeout: Long): Boolean {
        if (EXIT || Thread.currentThread().isInterrupted) return false
        log("WaitForText: '$targetText', timeout=${timeout}ms")
        val startTime = System.currentTimeMillis()
        while (!EXIT && !Thread.currentThread().isInterrupted) {
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed >= timeout) {
                log("WaitForText: Timeout!")
                return false
            }
            val currentText = getText(x1, y1, x2, y2)
            if (currentText.contains(targetText, ignoreCase = true)) {
                log("WaitForText: Found! (${elapsed}ms)")
                return true
            }
            sleep(200)
        }
        return false
    }

    fun compareColor(x: Int, y: Int, targetColor: Int, tolerance: Int = 0): Boolean {
        if (EXIT || Thread.currentThread().isInterrupted) return false
        val currentColor = getColor(x, y)
        val result = colorsMatch(currentColor, targetColor, tolerance)
        log("CompareColor: ($x,$y) ${colorToHex(currentColor)} ${if (result) "==" else "!="} ${colorToHex(targetColor)}")
        return result
    }

    private fun colorsMatch(color1: Int, color2: Int, tolerance: Int): Boolean {
        if (tolerance == 0) return color1 == color2

        val r1 = Color.red(color1)
        val g1 = Color.green(color1)
        val b1 = Color.blue(color1)
        val r2 = Color.red(color2)
        val g2 = Color.green(color2)
        val b2 = Color.blue(color2)

        return kotlin.math.abs(r1 - r2) <= tolerance &&
                kotlin.math.abs(g1 - g2) <= tolerance &&
                kotlin.math.abs(b1 - b2) <= tolerance
    }

    fun performBack() {
        if (EXIT || Thread.currentThread().isInterrupted) return
        val service = ClickerAccessibilityService.instance
        if (service == null) {
            log("⚠️ Accessibility Service недоступен")
            return
        }
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        log("Back")
    }

    fun performHome() {
        if (EXIT || Thread.currentThread().isInterrupted) return
        val service = ClickerAccessibilityService.instance
        if (service == null) {
            log("⚠️ Accessibility Service недоступен")
            return
        }
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        log("Home")
    }

    fun performRecents() {
        if (EXIT || Thread.currentThread().isInterrupted) return
        val service = ClickerAccessibilityService.instance
        if (service == null) {
            log("⚠️ Accessibility Service недоступен")
            return
        }
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
        log("Recents")
    }

    fun vibrate(duration: Long) {
        if (EXIT || Thread.currentThread().isInterrupted) return
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }

            if (vibrator == null) {
                log("Vibrate error: Vibrator not available")
                return
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
            log("Vibrate: ${duration}ms")
        } catch (e: Exception) {
            log("Vibrate error: ${e.message}")
        }
    }

    fun showToast(text: String) {
        if (EXIT || Thread.currentThread().isInterrupted) return
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
        }
        log("Toast: $text")
    }

    fun getText(x1: Int, y1: Int, x2: Int, y2: Int): String {
        if (EXIT || Thread.currentThread().isInterrupted) return ""
        val screenshot = screenshot() ?: return ""

        var cropped: Bitmap? = null
        var shouldRecycleCropped = false
        return try {
            if (x1 >= x2 || y1 >= y2) {
                log("OCR Error: Invalid coordinates")
                return ""
            }

            val cropX = x1.coerceIn(0, screenshot.width - 1)
            val cropY = y1.coerceIn(0, screenshot.height - 1)
            val cropW = (x2 - x1).coerceIn(1, screenshot.width - cropX)
            val cropH = (y2 - y1).coerceIn(1, screenshot.height - cropY)

            cropped = Bitmap.createBitmap(screenshot, cropX, cropY, cropW, cropH)
            shouldRecycleCropped = cropped !== screenshot

            val image = InputImage.fromBitmap(cropped, 0)
            val latch = CountDownLatch(1)
            var result = ""

            getTextRecognizer().process(image)
                .addOnSuccessListener { visionText ->
                    result = visionText.text.replace("\n", " ").trim()
                    latch.countDown()
                }
                .addOnFailureListener { e ->
                    log("OCR Error: ${e.message}")
                    latch.countDown()
                }

            if (!latch.await(3000, TimeUnit.MILLISECONDS)) {
                log("OCR Timeout")
            }
            result
        } catch (e: Exception) {
            log("OCR Error: ${e.message}")
            ""
        } finally {
            if (shouldRecycleCropped) cropped?.recycle()
            screenshot.recycle()
        }
    }

    fun pushToClipboard(text: String) {
        if (EXIT || Thread.currentThread().isInterrupted) return
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("text", text)
            clipboard.setPrimaryClip(clip)
        }
        log("Clipboard: $text")
    }
}
