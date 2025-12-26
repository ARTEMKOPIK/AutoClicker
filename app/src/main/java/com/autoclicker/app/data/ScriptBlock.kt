package com.autoclicker.app.data

/**
 * Параметр блока
 */
data class BlockParameter(
    val name: String,
    val label: String,
    val type: ParameterType,
    val value: String = "",
    val defaultValue: String = ""
)

/**
 * Блок скрипта в визуальном редакторе
 */
data class ScriptBlock(
    val type: BlockType,
    val category: BlockCategory,
    var order: Int = 0,
    val parameters: MutableList<BlockParameter> = mutableListOf()
) {
    
    /**
     * Генерирует код для данного блока
     */
    fun generateCode(): String {
        return when (type) {
            BlockType.CLICK -> {
                val x = parameters.find { it.name == "x" }?.value ?: "0"
                val y = parameters.find { it.name == "y" }?.value ?: "0"
                "click($x, $y)"
            }
            BlockType.LONG_CLICK -> {
                val x = parameters.find { it.name == "x" }?.value ?: "0"
                val y = parameters.find { it.name == "y" }?.value ?: "0"
                val duration = parameters.find { it.name == "duration" }?.value ?: "1000"
                "longClick($x, $y, $duration)"
            }
            BlockType.SWIPE -> {
                val x1 = parameters.find { it.name == "x1" }?.value ?: "0"
                val y1 = parameters.find { it.name == "y1" }?.value ?: "0"
                val x2 = parameters.find { it.name == "x2" }?.value ?: "0"
                val y2 = parameters.find { it.name == "y2" }?.value ?: "0"
                val duration = parameters.find { it.name == "duration" }?.value ?: "500"
                "swipe($x1, $y1, $x2, $y2, $duration)"
            }
            BlockType.TAP -> {
                val x = parameters.find { it.name == "x" }?.value ?: "0"
                val y = parameters.find { it.name == "y" }?.value ?: "0"
                val count = parameters.find { it.name == "count" }?.value ?: "2"
                "tap($x, $y, $count)"
            }
            BlockType.SLEEP -> {
                val ms = parameters.find { it.name == "ms" }?.value ?: "1000"
                "sleep($ms)"
            }
            BlockType.WAIT_COLOR -> {
                val x = parameters.find { it.name == "x" }?.value ?: "0"
                val y = parameters.find { it.name == "y" }?.value ?: "0"
                val color = parameters.find { it.name == "color" }?.value ?: "#FFFFFF"
                "waitColor($x, $y, \"$color\")"
            }
            BlockType.WAIT_TEXT -> {
                val text = parameters.find { it.name == "text" }?.value ?: ""
                "waitText(\"$text\")"
            }
            BlockType.IF_COLOR -> {
                val x = parameters.find { it.name == "x" }?.value ?: "0"
                val y = parameters.find { it.name == "y" }?.value ?: "0"
                val color = parameters.find { it.name == "color" }?.value ?: "#FFFFFF"
                "if (checkColor($x, $y, \"$color\")) {"
            }
            BlockType.IF_TEXT -> {
                val text = parameters.find { it.name == "text" }?.value ?: ""
                "if (findText(\"$text\")) {"
            }
            BlockType.IF_IMAGE -> {
                val image = parameters.find { it.name == "image" }?.value ?: ""
                "if (findImage(\"$image\")) {"
            }
            BlockType.LOOP -> "while (true) {"
            BlockType.LOOP_COUNT -> {
                val count = parameters.find { it.name == "count" }?.value ?: "10"
                "for (i in 0..$count) {"
            }
            BlockType.SET_VAR -> {
                val name = parameters.find { it.name == "name" }?.value ?: "var"
                val value = parameters.find { it.name == "value" }?.value ?: "0"
                "var $name = $value"
            }
            BlockType.INC_VAR -> {
                val name = parameters.find { it.name == "name" }?.value ?: "var"
                "$name++"
            }
            BlockType.DEC_VAR -> {
                val name = parameters.find { it.name == "name" }?.value ?: "var"
                "$name--"
            }
            BlockType.LOG -> {
                val message = parameters.find { it.name == "message" }?.value ?: ""
                "log(\"$message\")"
            }
            BlockType.TOAST -> {
                val message = parameters.find { it.name == "message" }?.value ?: ""
                "toast(\"$message\")"
            }
            BlockType.TELEGRAM -> {
                val message = parameters.find { it.name == "message" }?.value ?: ""
                "sendTelegram(\"$message\")"
            }
            BlockType.GET_TEXT -> {
                val x = parameters.find { it.name == "x" }?.value ?: "0"
                val y = parameters.find { it.name == "y" }?.value ?: "0"
                val width = parameters.find { it.name == "width" }?.value ?: "100"
                val height = parameters.find { it.name == "height" }?.value ?: "100"
                "getText($x, $y, $width, $height)"
            }
            BlockType.FIND_TEXT -> {
                val text = parameters.find { it.name == "text" }?.value ?: ""
                "findText(\"$text\")"
            }
            BlockType.BACK -> "pressBack()"
            BlockType.HOME -> "pressHome()"
            BlockType.RECENTS -> "pressRecents()"
            BlockType.FUNCTION -> {
                val name = parameters.find { it.name == "name" }?.value ?: "func"
                "fun $name() {"
            }
            BlockType.CALL_FUNC -> {
                val name = parameters.find { it.name == "name" }?.value ?: "func"
                "$name()"
            }
            BlockType.RETURN -> "return"
            BlockType.COMMENT -> {
                val text = parameters.find { it.name == "text" }?.value ?: ""
                "// $text"
            }
            BlockType.BREAK -> "break"
        }
    }
    
    companion object {
        /**
         * Создает блок заданного типа с параметрами по умолчанию
         */
        fun createBlock(type: BlockType): ScriptBlock {
            val category = getCategoryForType(type)
            val parameters = getDefaultParametersForType(type)
            return ScriptBlock(type, category, parameters = parameters)
        }
        
        /**
         * Возвращает категорию для типа блока
         */
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
         * Возвращает параметры по умолчанию для типа блока
         */
        private fun getDefaultParametersForType(type: BlockType): MutableList<BlockParameter> {
            return when (type) {
                BlockType.CLICK -> mutableListOf(
                    BlockParameter("x", "X", ParameterType.COORDINATE, "0"),
                    BlockParameter("y", "Y", ParameterType.COORDINATE, "0")
                )
                BlockType.LONG_CLICK -> mutableListOf(
                    BlockParameter("x", "X", ParameterType.COORDINATE, "0"),
                    BlockParameter("y", "Y", ParameterType.COORDINATE, "0"),
                    BlockParameter("duration", "Duration (ms)", ParameterType.NUMBER, "1000")
                )
                BlockType.SWIPE -> mutableListOf(
                    BlockParameter("x1", "Start X", ParameterType.COORDINATE, "0"),
                    BlockParameter("y1", "Start Y", ParameterType.COORDINATE, "0"),
                    BlockParameter("x2", "End X", ParameterType.COORDINATE, "0"),
                    BlockParameter("y2", "End Y", ParameterType.COORDINATE, "0"),
                    BlockParameter("duration", "Duration (ms)", ParameterType.NUMBER, "500")
                )
                BlockType.TAP -> mutableListOf(
                    BlockParameter("x", "X", ParameterType.COORDINATE, "0"),
                    BlockParameter("y", "Y", ParameterType.COORDINATE, "0"),
                    BlockParameter("count", "Count", ParameterType.NUMBER, "2")
                )
                BlockType.SLEEP -> mutableListOf(
                    BlockParameter("ms", "Milliseconds", ParameterType.NUMBER, "1000")
                )
                BlockType.WAIT_COLOR -> mutableListOf(
                    BlockParameter("x", "X", ParameterType.COORDINATE, "0"),
                    BlockParameter("y", "Y", ParameterType.COORDINATE, "0"),
                    BlockParameter("color", "Color", ParameterType.COLOR, "#FFFFFF")
                )
                BlockType.WAIT_TEXT -> mutableListOf(
                    BlockParameter("text", "Text", ParameterType.TEXT, "")
                )
                BlockType.IF_COLOR -> mutableListOf(
                    BlockParameter("x", "X", ParameterType.COORDINATE, "0"),
                    BlockParameter("y", "Y", ParameterType.COORDINATE, "0"),
                    BlockParameter("color", "Color", ParameterType.COLOR, "#FFFFFF")
                )
                BlockType.IF_TEXT -> mutableListOf(
                    BlockParameter("text", "Text", ParameterType.TEXT, "")
                )
                BlockType.IF_IMAGE -> mutableListOf(
                    BlockParameter("image", "Image Path", ParameterType.TEXT, "")
                )
                BlockType.LOOP -> mutableListOf()
                BlockType.LOOP_COUNT -> mutableListOf(
                    BlockParameter("count", "Count", ParameterType.NUMBER, "10")
                )
                BlockType.SET_VAR -> mutableListOf(
                    BlockParameter("name", "Variable Name", ParameterType.TEXT, "var"),
                    BlockParameter("value", "Value", ParameterType.TEXT, "0")
                )
                BlockType.INC_VAR, BlockType.DEC_VAR -> mutableListOf(
                    BlockParameter("name", "Variable Name", ParameterType.TEXT, "var")
                )
                BlockType.LOG, BlockType.TOAST, BlockType.TELEGRAM -> mutableListOf(
                    BlockParameter("message", "Message", ParameterType.TEXT, "")
                )
                BlockType.GET_TEXT -> mutableListOf(
                    BlockParameter("x", "X", ParameterType.COORDINATE, "0"),
                    BlockParameter("y", "Y", ParameterType.COORDINATE, "0"),
                    BlockParameter("width", "Width", ParameterType.NUMBER, "100"),
                    BlockParameter("height", "Height", ParameterType.NUMBER, "100")
                )
                BlockType.FIND_TEXT -> mutableListOf(
                    BlockParameter("text", "Text", ParameterType.TEXT, "")
                )
                BlockType.BACK, BlockType.HOME, BlockType.RECENTS -> mutableListOf()
                BlockType.FUNCTION -> mutableListOf(
                    BlockParameter("name", "Function Name", ParameterType.TEXT, "func")
                )
                BlockType.CALL_FUNC -> mutableListOf(
                    BlockParameter("name", "Function Name", ParameterType.TEXT, "func")
                )
                BlockType.RETURN -> mutableListOf()
                BlockType.COMMENT -> mutableListOf(
                    BlockParameter("text", "Comment", ParameterType.TEXT, "")
                )
                BlockType.BREAK -> mutableListOf()
            }
        }
    }
}

