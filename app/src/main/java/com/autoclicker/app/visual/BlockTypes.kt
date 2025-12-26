package com.autoclicker.app.visual

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Типы блоков для визуального редактора
 */
enum class BlockCategory(val title: String, val icon: String, val color: String) {
    ACTIONS("Действия", "🖱️", "#8B5CF6"),
    WAIT("Ожидание", "⏱️", "#06B6D4"),
    CONDITIONS("Условия", "🔀", "#F59E0B"),
    LOOPS("Циклы", "🔄", "#10B981"),
    DATA("Данные", "📊", "#EC4899"),
    OUTPUT("Вывод", "📤", "#3B82F6"),
    SYSTEM("Система", "⚙️", "#6B7280")
}

enum class BlockType(
    val category: BlockCategory,
    val title: String,
    val icon: String,
    val description: String,
    val params: List<BlockParam> = emptyList(),
    val hasChildren: Boolean = false
) {
    // === ДЕЙСТВИЯ ===
    CLICK(
        BlockCategory.ACTIONS, "Клик", "👆", "Нажать на точку экрана",
        listOf(BlockParam("x", "X", ParamType.NUMBER), BlockParam("y", "Y", ParamType.NUMBER))
    ),
    LONG_CLICK(
        BlockCategory.ACTIONS, "Долгий клик", "👇", "Долгое нажатие",
        listOf(
            BlockParam("x", "X", ParamType.NUMBER),
            BlockParam("y", "Y", ParamType.NUMBER),
            BlockParam("duration", "Длительность (мс)", ParamType.NUMBER, "500")
        )
    ),
    SWIPE(
        BlockCategory.ACTIONS, "Свайп", "👉", "Провести по экрану",
        listOf(
            BlockParam("x1", "X1", ParamType.NUMBER),
            BlockParam("y1", "Y1", ParamType.NUMBER),
            BlockParam("x2", "X2", ParamType.NUMBER),
            BlockParam("y2", "Y2", ParamType.NUMBER),
            BlockParam("duration", "Длительность (мс)", ParamType.NUMBER, "300")
        )
    ),
    TAP(
        BlockCategory.ACTIONS, "Множественный тап", "👆👆", "Несколько быстрых нажатий",
        listOf(
            BlockParam("x", "X", ParamType.NUMBER),
            BlockParam("y", "Y", ParamType.NUMBER),
            BlockParam("count", "Количество", ParamType.NUMBER, "2")
        )
    ),
    
    // === ОЖИДАНИЕ ===
    SLEEP(
        BlockCategory.WAIT, "Ждать", "⏳", "Пауза в миллисекундах",
        listOf(BlockParam("ms", "Миллисекунды", ParamType.NUMBER, "1000"))
    ),
    WAIT_COLOR(
        BlockCategory.WAIT, "Ждать цвет", "🎨", "Ждать появления цвета",
        listOf(
            BlockParam("x", "X", ParamType.NUMBER),
            BlockParam("y", "Y", ParamType.NUMBER),
            BlockParam("color", "Цвет", ParamType.COLOR),
            BlockParam("timeout", "Таймаут (мс)", ParamType.NUMBER, "10000")
        )
    ),
    WAIT_TEXT(
        BlockCategory.WAIT, "Ждать текст", "📝", "Ждать появления текста на экране",
        listOf(
            BlockParam("x1", "X1", ParamType.NUMBER),
            BlockParam("y1", "Y1", ParamType.NUMBER),
            BlockParam("x2", "X2", ParamType.NUMBER),
            BlockParam("y2", "Y2", ParamType.NUMBER),
            BlockParam("text", "Текст", ParamType.TEXT),
            BlockParam("timeout", "Таймаут (мс)", ParamType.NUMBER, "10000")
        )
    ),
    
    // === УСЛОВИЯ ===
    IF_COLOR(
        BlockCategory.CONDITIONS, "Если цвет", "🎨❓", "Выполнить если цвет совпадает",
        listOf(
            BlockParam("x", "X", ParamType.NUMBER),
            BlockParam("y", "Y", ParamType.NUMBER),
            BlockParam("color", "Цвет", ParamType.COLOR),
            BlockParam("tolerance", "Погрешность", ParamType.NUMBER, "10")
        ),
        hasChildren = true
    ),
    IF_TEXT(
        BlockCategory.CONDITIONS, "Если текст", "📝❓", "Выполнить если текст найден",
        listOf(
            BlockParam("x1", "X1", ParamType.NUMBER),
            BlockParam("y1", "Y1", ParamType.NUMBER),
            BlockParam("x2", "X2", ParamType.NUMBER),
            BlockParam("y2", "Y2", ParamType.NUMBER),
            BlockParam("text", "Текст", ParamType.TEXT)
        ),
        hasChildren = true
    ),
    
    // === ЦИКЛЫ ===
    REPEAT(
        BlockCategory.LOOPS, "Повторить", "🔁", "Повторить N раз",
        listOf(BlockParam("count", "Количество", ParamType.NUMBER, "5")),
        hasChildren = true
    ),
    LOOP_FOREVER(
        BlockCategory.LOOPS, "Бесконечный цикл", "♾️", "Повторять пока не остановят",
        hasChildren = true
    ),
    LOOP_WHILE_COLOR(
        BlockCategory.LOOPS, "Пока цвет", "🔄🎨", "Повторять пока цвет совпадает",
        listOf(
            BlockParam("x", "X", ParamType.NUMBER),
            BlockParam("y", "Y", ParamType.NUMBER),
            BlockParam("color", "Цвет", ParamType.COLOR)
        ),
        hasChildren = true
    ),
    BREAK(
        BlockCategory.LOOPS, "Прервать цикл", "⏹️", "Выйти из цикла"
    ),
    
    // === ДАННЫЕ ===
    GET_COLOR(
        BlockCategory.DATA, "Получить цвет", "🎨", "Сохранить цвет пикселя в переменную",
        listOf(
            BlockParam("x", "X", ParamType.NUMBER),
            BlockParam("y", "Y", ParamType.NUMBER),
            BlockParam("variable", "Переменная", ParamType.VARIABLE)
        )
    ),
    GET_TEXT(
        BlockCategory.DATA, "Распознать текст", "👁️", "OCR - распознать текст с экрана",
        listOf(
            BlockParam("x1", "X1", ParamType.NUMBER),
            BlockParam("y1", "Y1", ParamType.NUMBER),
            BlockParam("x2", "X2", ParamType.NUMBER),
            BlockParam("y2", "Y2", ParamType.NUMBER),
            BlockParam("variable", "Переменная", ParamType.VARIABLE)
        )
    ),
    RANDOM(
        BlockCategory.DATA, "Случайное число", "🎲", "Генерировать случайное число",
        listOf(
            BlockParam("min", "Минимум", ParamType.NUMBER, "0"),
            BlockParam("max", "Максимум", ParamType.NUMBER, "100"),
            BlockParam("variable", "Переменная", ParamType.VARIABLE)
        )
    ),
    SET_VARIABLE(
        BlockCategory.DATA, "Установить переменную", "📝", "Сохранить значение",
        listOf(
            BlockParam("name", "Имя", ParamType.VARIABLE),
            BlockParam("value", "Значение", ParamType.TEXT)
        )
    ),
    
    // === ВЫВОД ===
    LOG(
        BlockCategory.OUTPUT, "Лог", "📋", "Записать в лог",
        listOf(BlockParam("text", "Текст", ParamType.TEXT))
    ),
    TOAST(
        BlockCategory.OUTPUT, "Уведомление", "💬", "Показать всплывающее сообщение",
        listOf(BlockParam("text", "Текст", ParamType.TEXT))
    ),
    TELEGRAM(
        BlockCategory.OUTPUT, "Telegram", "📱", "Отправить в Telegram",
        listOf(BlockParam("text", "Текст", ParamType.TEXT))
    ),
    VIBRATE(
        BlockCategory.OUTPUT, "Вибрация", "📳", "Вибрировать",
        listOf(BlockParam("duration", "Длительность (мс)", ParamType.NUMBER, "200"))
    ),
    
    // === СИСТЕМА ===
    BACK(BlockCategory.SYSTEM, "Назад", "◀️", "Нажать кнопку Назад"),
    HOME(BlockCategory.SYSTEM, "Домой", "🏠", "Нажать кнопку Домой"),
    RECENTS(BlockCategory.SYSTEM, "Недавние", "📱", "Открыть недавние приложения"),
    STOP(BlockCategory.SYSTEM, "Остановить", "⏹️", "Остановить скрипт")
}

enum class ParamType {
    NUMBER,
    TEXT,
    COLOR,
    VARIABLE,
    COORDINATE
}

data class BlockParam(
    val id: String,
    val label: String,
    val type: ParamType,
    val defaultValue: String = ""
)

/**
 * Экземпляр блока в скрипте
 * 
 * ВАЖНО: Реализует Parcelable для сохранения состояния Activity при configuration changes
 */
@Parcelize
data class ScriptBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType,
    val params: MutableMap<String, String> = mutableMapOf(),
    val children: MutableList<ScriptBlock> = mutableListOf()
) : Parcelable {
    init {
        // Заполняем дефолтные значения
        type.params.forEach { param ->
            if (!params.containsKey(param.id) && param.defaultValue.isNotEmpty()) {
                params[param.id] = param.defaultValue
            }
        }
    }
    
    fun toCode(): String {
        return when (type) {
            BlockType.CLICK -> "click(${params["x"]}, ${params["y"]})"
            BlockType.LONG_CLICK -> "longClick(${params["x"]}, ${params["y"]}, ${params["duration"]})"
            BlockType.SWIPE -> "swipe(${params["x1"]}, ${params["y1"]}, ${params["x2"]}, ${params["y2"]}, ${params["duration"]})"
            BlockType.TAP -> "tap(${params["x"]}, ${params["y"]}, ${params["count"]})"
            
            BlockType.SLEEP -> "sleep(${params["ms"]})"
            BlockType.WAIT_COLOR -> "waitForColor(${params["x"]}, ${params["y"]}, \"${params["color"]}\", ${params["timeout"]})"
            BlockType.WAIT_TEXT -> "waitForText(${params["x1"]}, ${params["y1"]}, ${params["x2"]}, ${params["y2"]}, \"${params["text"]}\", ${params["timeout"]})"
            
            BlockType.IF_COLOR -> buildIfColorCode()
            BlockType.IF_TEXT -> buildIfTextCode()
            
            BlockType.REPEAT -> buildRepeatCode()
            BlockType.LOOP_FOREVER -> buildLoopForeverCode()
            BlockType.LOOP_WHILE_COLOR -> buildLoopWhileColorCode()
            BlockType.BREAK -> "break"
            
            BlockType.GET_COLOR -> "${params["variable"]} = getColor(${params["x"]}, ${params["y"]})"
            BlockType.GET_TEXT -> "${params["variable"]} = getText(${params["x1"]}, ${params["y1"]}, ${params["x2"]}, ${params["y2"]})"
            BlockType.RANDOM -> "${params["variable"]} = random(${params["min"]}, ${params["max"]})"
            BlockType.SET_VARIABLE -> "setVar(\"${params["name"]}\", \"${params["value"]}\")"
            
            BlockType.LOG -> "log(\"${params["text"]}\")"
            BlockType.TOAST -> "toast(\"${params["text"]}\")"
            BlockType.TELEGRAM -> "sendTelegram(\"${params["text"]}\")"
            BlockType.VIBRATE -> "vibrate(${params["duration"]})"
            
            BlockType.BACK -> "back()"
            BlockType.HOME -> "home()"
            BlockType.RECENTS -> "recents()"
            BlockType.STOP -> "EXIT = true"
        }
    }
    
    private fun buildIfColorCode(): String {
        val childrenCode = children.joinToString("\n    ") { it.toCode() }
        return """if (compareColor(${params["x"]}, ${params["y"]}, "${params["color"]}", ${params["tolerance"]})) {
    $childrenCode
}"""
    }
    
    private fun buildIfTextCode(): String {
        val childrenCode = children.joinToString("\n    ") { it.toCode() }
        return """val text = getText(${params["x1"]}, ${params["y1"]}, ${params["x2"]}, ${params["y2"]})
if (text.contains("${params["text"]}")) {
    $childrenCode
}"""
    }
    
    private fun buildRepeatCode(): String {
        val childrenCode = children.joinToString("\n    ") { it.toCode() }
        return """for (i in 1..${params["count"]}) {
    $childrenCode
}"""
    }
    
    private fun buildLoopForeverCode(): String {
        val childrenCode = children.joinToString("\n    ") { it.toCode() }
        return """while (!EXIT) {
    $childrenCode
}"""
    }
    
    private fun buildLoopWhileColorCode(): String {
        val childrenCode = children.joinToString("\n    ") { it.toCode() }
        return """while (!EXIT && compareColor(${params["x"]}, ${params["y"]}, "${params["color"]}")) {
    $childrenCode
}"""
    }
}

/**
 * Визуальный скрипт - список блоков
 */
data class VisualScript(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val blocks: MutableList<ScriptBlock> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toCode(): String {
        val header = """// Скрипт: $name
// Создан визуальным редактором
// ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(createdAt))}

"""
        val body = blocks.joinToString("\n\n") { it.toCode() }
        return header + body
    }
}
