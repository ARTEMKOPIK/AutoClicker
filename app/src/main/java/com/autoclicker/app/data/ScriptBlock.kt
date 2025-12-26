package com.autoclicker.app.data

import java.util.UUID

/**
 * Типы блоков для визуального редактора
 */
enum class BlockType {
    // Действия
    CLICK,          // click(x, y)
    LONG_CLICK,     // longClick(x, y)
    SWIPE,          // swipe(x1, y1, x2, y2, duration)
    TAP,            // tap(x, y, count)
    
    // Ожидание
    SLEEP,          // sleep(ms)
    WAIT_COLOR,     // waitForColor(x, y, color, timeout)
    WAIT_TEXT,      // waitForText(text, timeout)
    
    // Проверки
    IF_COLOR,       // if (getColor(x, y) == color)
    IF_TEXT,        // if (findText(text) != null)
    IF_IMAGE,       // if (findImage(template) != null)
    
    // Циклы
    LOOP,           // while (true)
    LOOP_COUNT,     // for (i = 0; i < count; i++)
    
    // Переменные
    SET_VAR,        // setVar(name, value)
    INC_VAR,        // incVar(name)
    DEC_VAR,        // decVar(name)
    
    // Вывод
    LOG,            // log(message)
    TOAST,          // toast(message)
    TELEGRAM,       // sendTelegram(message)
    
    // OCR
    GET_TEXT,       // getText(x, y, w, h)
    FIND_TEXT,      // findText(text)
    
    // Системные
    BACK,           // back()
    HOME,           // home()
    RECENTS,        // recents()
    
    // Функции
    FUNCTION,       // function myFunc()
    CALL_FUNC,      // myFunc()
    RETURN,         // return value
    
    // Специальные
    COMMENT,        // // комментарий
    BREAK           // break (выход из цикла)
}

/**
 * Категории блоков
 */
enum class BlockCategory {
    ACTIONS,        // Действия (клики, свайпы)
    WAIT,           // Ожидание
    CONDITIONS,     // Условия
    LOOPS,          // Циклы
    VARIABLES,      // Переменные
    OUTPUT,         // Вывод
    OCR,            // OCR
    SYSTEM,         // Системные действия
    FUNCTIONS,      // Функции
    SPECIAL         // Специальные
}

/**
 * Параметр блока
 */
data class BlockParameter(
    val name: String,           // Имя параметра (x, y, duration и т.д.)
    val type: ParameterType,    // Тип параметра
    val value: String = "",     // Значение
    val label: String = ""      // Отображаемое имя
)

/**
 * Типы параметров
 */
enum class ParameterType {
    NUMBER,         // Число
    TEXT,           // Текст
    COLOR,          // Цвет (hex)
    COORDINATE,     // Координата (x или y)
    BOOLEAN,        // true/false
    VARIABLE        // Имя переменной
}

/**
 * Блок визуального редактора
 */
data class ScriptBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: BlockType,
    val category: BlockCategory,
    val parameters: MutableList<BlockParameter> = mutableListOf(),
    val children: MutableList<ScriptBlock> = mutableListOf(),  // Дочерние блоки (для циклов, условий)
    var order: Int = 0                                          // Порядок в списке
) {
    /**
     * Генерировать код для этого блока
     */
    fun generateCode(indent: String = ""): String {
        return when (type) {
            BlockType.CLICK -> {
                val x = getParam("x")
                val y = getParam("y")
                "${indent}click($x, $y)"
            }
            BlockType.LONG_CLICK -> {
                val x = getParam("x")
                val y = getParam("y")
                "${indent}longClick($x, $y)"
            }
            BlockType.SWIPE -> {
                val x1 = getParam("x1")
                val y1 = getParam("y1")
                val x2 = getParam("x2")
                val y2 = getParam("y2")
                val duration = getParam("duration", "500")
                "${indent}swipe($x1, $y1, $x2, $y2, $duration)"
            }
            BlockType.TAP -> {
                val x = getParam("x")
                val y = getParam("y")
                val count = getParam("count", "2")
                "${indent}tap($x, $y, $count)"
            }
            BlockType.SLEEP -> {
                val ms = getParam("ms", "1000")
                "${indent}sleep($ms)"
            }
            BlockType.WAIT_COLOR -> {
                val x = getParam("x")
                val y = getParam("y")
                val color = getParam("color")
                val timeout = getParam("timeout", "5000")
                "${indent}waitForColor($x, $y, $color, $timeout)"
            }
            BlockType.WAIT_TEXT -> {
                val text = getParam("text")
                val timeout = getParam("timeout", "5000")
                "${indent}waitForText(\"$text\", $timeout)"
            }
            BlockType.IF_COLOR -> {
                val x = getParam("x")
                val y = getParam("y")
                val color = getParam("color")
                val condition = "${indent}if (getColor($x, $y) == $color) {\n"
                val body = children.joinToString("\n") { it.generateCode("$indent    ") }
                val end = "\n${indent}}"
                condition + body + end
            }
            BlockType.IF_TEXT -> {
                val text = getParam("text")
                val condition = "${indent}if (findText(\"$text\") != null) {\n"
                val body = children.joinToString("\n") { it.generateCode("$indent    ") }
                val end = "\n${indent}}"
                condition + body + end
            }
            BlockType.IF_IMAGE -> {
                val template = getParam("template")
                val condition = "${indent}if (findImage(\"$template\") != null) {\n"
                val body = children.joinToString("\n") { it.generateCode("$indent    ") }
                val end = "\n${indent}}"
                condition + body + end
            }
            BlockType.LOOP -> {
                val loopStart = "${indent}while (true) {\n"
                val body = children.joinToString("\n") { it.generateCode("$indent    ") }
                val end = "\n${indent}}"
                loopStart + body + end
            }
            BlockType.LOOP_COUNT -> {
                val count = getParam("count", "10")
                val varName = getParam("var", "i")
                val loopStart = "${indent}for ($varName = 0; $varName < $count; $varName++) {\n"
                val body = children.joinToString("\n") { it.generateCode("$indent    ") }
                val end = "\n${indent}}"
                loopStart + body + end
            }
            BlockType.SET_VAR -> {
                val name = getParam("name")
                val value = getParam("value")
                "${indent}setVar(\"$name\", $value)"
            }
            BlockType.INC_VAR -> {
                val name = getParam("name")
                "${indent}incVar(\"$name\")"
            }
            BlockType.DEC_VAR -> {
                val name = getParam("name")
                "${indent}decVar(\"$name\")"
            }
            BlockType.LOG -> {
                val message = getParam("message")
                "${indent}log(\"$message\")"
            }
            BlockType.TOAST -> {
                val message = getParam("message")
                "${indent}toast(\"$message\")"
            }
            BlockType.TELEGRAM -> {
                val message = getParam("message")
                "${indent}sendTelegram(\"$message\")"
            }
            BlockType.GET_TEXT -> {
                val x = getParam("x")
                val y = getParam("y")
                val w = getParam("w")
                val h = getParam("h")
                val varName = getParam("var", "text")
                "${indent}$varName = getText($x, $y, $w, $h)"
            }
            BlockType.FIND_TEXT -> {
                val text = getParam("text")
                val varName = getParam("var", "pos")
                "${indent}$varName = findText(\"$text\")"
            }
            BlockType.BACK -> "${indent}back()"
            BlockType.HOME -> "${indent}home()"
            BlockType.RECENTS -> "${indent}recents()"
            BlockType.FUNCTION -> {
                val name = getParam("name", "myFunction")
                val funcStart = "${indent}function $name() {\n"
                val body = children.joinToString("\n") { it.generateCode("$indent    ") }
                val end = "\n${indent}}"
                funcStart + body + end
            }
            BlockType.CALL_FUNC -> {
                val name = getParam("name")
                "${indent}$name()"
            }
            BlockType.RETURN -> {
                val value = getParam("value", "")
                if (value.isEmpty()) "${indent}return" else "${indent}return $value"
            }
            BlockType.COMMENT -> {
                val text = getParam("text")
                "${indent}// $text"
            }
            BlockType.BREAK -> "${indent}break"
        }
    }
    
    /**
     * Получить значение параметра
     */
    private fun getParam(name: String, default: String = ""): String {
        return parameters.find { it.name == name }?.value ?: default
    }
    
    /**
     * Получить категорию блока
     */
    companion object {
        fun getCategoryForType(type: BlockType): BlockCategory {
            return when (type) {
                BlockType.CLICK, BlockType.LONG_CLICK, BlockType.SWIPE, BlockType.TAP -> BlockCategory.ACTIONS
                BlockType.SLEEP, BlockType.WAIT_COLOR, BlockType.WAIT_TEXT -> BlockCategory.WAIT
                BlockType.IF_COLOR, BlockType.IF_TEXT, BlockType.IF_IMAGE -> BlockCategory.CONDITIONS
                BlockType.LOOP, BlockType.LOOP_COUNT -> BlockCategory.LOOPS
                BlockType.SET_VAR, BlockType.INC_VAR, BlockType.DEC_VAR -> BlockCategory.VARIABLES
                BlockType.LOG, BlockType.TOAST, BlockType.TELEGRAM -> BlockCategory.OUTPUT
                BlockType.GET_TEXT, BlockType.FIND_TEXT -> BlockCategory.OCR
                BlockType.BACK, BlockType.HOME, BlockType.RECENTS -> BlockCategory.SYSTEM
                BlockType.FUNCTION, BlockType.CALL_FUNC, BlockType.RETURN -> BlockCategory.FUNCTIONS
                BlockType.COMMENT, BlockType.BREAK -> BlockCategory.SPECIAL
            }
        }
        
        /**
         * Создать блок с параметрами по умолчанию
         */
        fun createBlock(type: BlockType): ScriptBlock {
            val category = getCategoryForType(type)
            val parameters = getDefaultParameters(type)
            return ScriptBlock(
                type = type,
                category = category,
                parameters = parameters
            )
        }
        
        /**
         * Получить параметры по умолчанию для типа блока
         */
        private fun getDefaultParameters(type: BlockType): MutableList<BlockParameter> {
            return when (type) {
                BlockType.CLICK, BlockType.LONG_CLICK -> mutableListOf(
                    BlockParameter("x", ParameterType.COORDINATE, "0", "X"),
                    BlockParameter("y", ParameterType.COORDINATE, "0", "Y")
                )
                BlockType.SWIPE -> mutableListOf(
                    BlockParameter("x1", ParameterType.COORDINATE, "0", "X1"),
                    BlockParameter("y1", ParameterType.COORDINATE, "0", "Y1"),
                    BlockParameter("x2", ParameterType.COORDINATE, "0", "X2"),
                    BlockParameter("y2", ParameterType.COORDINATE, "0", "Y2"),
                    BlockParameter("duration", ParameterType.NUMBER, "500", "Duration (ms)")
                )
                BlockType.TAP -> mutableListOf(
                    BlockParameter("x", ParameterType.COORDINATE, "0", "X"),
                    BlockParameter("y", ParameterType.COORDINATE, "0", "Y"),
                    BlockParameter("count", ParameterType.NUMBER, "2", "Count")
                )
                BlockType.SLEEP -> mutableListOf(
                    BlockParameter("ms", ParameterType.NUMBER, "1000", "Duration (ms)")
                )
                BlockType.WAIT_COLOR -> mutableListOf(
                    BlockParameter("x", ParameterType.COORDINATE, "0", "X"),
                    BlockParameter("y", ParameterType.COORDINATE, "0", "Y"),
                    BlockParameter("color", ParameterType.COLOR, "#FFFFFF", "Color"),
                    BlockParameter("timeout", ParameterType.NUMBER, "5000", "Timeout (ms)")
                )
                BlockType.WAIT_TEXT -> mutableListOf(
                    BlockParameter("text", ParameterType.TEXT, "", "Text"),
                    BlockParameter("timeout", ParameterType.NUMBER, "5000", "Timeout (ms)")
                )
                BlockType.IF_COLOR -> mutableListOf(
                    BlockParameter("x", ParameterType.COORDINATE, "0", "X"),
                    BlockParameter("y", ParameterType.COORDINATE, "0", "Y"),
                    BlockParameter("color", ParameterType.COLOR, "#FFFFFF", "Color")
                )
                BlockType.IF_TEXT -> mutableListOf(
                    BlockParameter("text", ParameterType.TEXT, "", "Text")
                )
                BlockType.IF_IMAGE -> mutableListOf(
                    BlockParameter("template", ParameterType.TEXT, "", "Template file")
                )
                BlockType.LOOP -> mutableListOf() // Бесконечный цикл без параметров
                BlockType.LOOP_COUNT -> mutableListOf(
                    BlockParameter("count", ParameterType.NUMBER, "10", "Count"),
                    BlockParameter("var", ParameterType.VARIABLE, "i", "Variable")
                )
                BlockType.SET_VAR -> mutableListOf(
                    BlockParameter("name", ParameterType.VARIABLE, "", "Variable"),
                    BlockParameter("value", ParameterType.TEXT, "", "Value")
                )
                BlockType.INC_VAR, BlockType.DEC_VAR -> mutableListOf(
                    BlockParameter("name", ParameterType.VARIABLE, "", "Variable")
                )
                BlockType.LOG, BlockType.TOAST, BlockType.TELEGRAM -> mutableListOf(
                    BlockParameter("message", ParameterType.TEXT, "", "Message")
                )
                BlockType.GET_TEXT -> mutableListOf(
                    BlockParameter("x", ParameterType.COORDINATE, "0", "X"),
                    BlockParameter("y", ParameterType.COORDINATE, "0", "Y"),
                    BlockParameter("w", ParameterType.NUMBER, "100", "Width"),
                    BlockParameter("h", ParameterType.NUMBER, "50", "Height"),
                    BlockParameter("var", ParameterType.VARIABLE, "text", "Variable")
                )
                BlockType.FIND_TEXT -> mutableListOf(
                    BlockParameter("text", ParameterType.TEXT, "", "Text"),
                    BlockParameter("var", ParameterType.VARIABLE, "pos", "Variable")
                )
                BlockType.FUNCTION -> mutableListOf(
                    BlockParameter("name", ParameterType.TEXT, "myFunction", "Function name")
                )
                BlockType.CALL_FUNC -> mutableListOf(
                    BlockParameter("name", ParameterType.TEXT, "", "Function name")
                )
                BlockType.RETURN -> mutableListOf(
                    BlockParameter("value", ParameterType.TEXT, "", "Return value")
                )
                BlockType.COMMENT -> mutableListOf(
                    BlockParameter("text", ParameterType.TEXT, "", "Comment")
                )
                else -> mutableListOf()
            }
        }
    }
}

