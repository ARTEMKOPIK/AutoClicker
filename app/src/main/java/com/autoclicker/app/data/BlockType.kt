package com.autoclicker.app.data

/**
 * Типы блоков для визуального редактора
 * Использует sealed interface для лучшей типобезопасности и расширяемости
 */
sealed interface BlockType {
    
    // Actions
    object CLICK : BlockType
    object LONG_CLICK : BlockType
    object SWIPE : BlockType
    object TAP : BlockType
    
    // Wait
    object SLEEP : BlockType
    object WAIT_COLOR : BlockType
    object WAIT_TEXT : BlockType
    
    // Conditions
    object IF_COLOR : BlockType
    object IF_TEXT : BlockType
    object IF_IMAGE : BlockType
    
    // Loops
    object LOOP : BlockType
    object LOOP_COUNT : BlockType
    
    // Variables
    object SET_VAR : BlockType
    object INC_VAR : BlockType
    object DEC_VAR : BlockType
    
    // Output
    object LOG : BlockType
    object TOAST : BlockType
    object TELEGRAM : BlockType
    
    // OCR
    object GET_TEXT : BlockType
    object FIND_TEXT : BlockType
    
    // System
    object BACK : BlockType
    object HOME : BlockType
    object RECENTS : BlockType
    
    // Functions
    object FUNCTION : BlockType
    object CALL_FUNC : BlockType
    object RETURN : BlockType
    
    // Special
    object COMMENT : BlockType
    object BREAK : BlockType
    
    companion object {
        fun fromString(name: String): BlockType? {
            return when (name.uppercase()) {
                "CLICK" -> CLICK
                "LONG_CLICK" -> LONG_CLICK
                "SWIPE" -> SWIPE
                "TAP" -> TAP
                "SLEEP" -> SLEEP
                "WAIT_COLOR" -> WAIT_COLOR
                "WAIT_TEXT" -> WAIT_TEXT
                "IF_COLOR" -> IF_COLOR
                "IF_TEXT" -> IF_TEXT
                "IF_IMAGE" -> IF_IMAGE
                "LOOP" -> LOOP
                "LOOP_COUNT" -> LOOP_COUNT
                "SET_VAR" -> SET_VAR
                "INC_VAR" -> INC_VAR
                "DEC_VAR" -> DEC_VAR
                "LOG" -> LOG
                "TOAST" -> TOAST
                "TELEGRAM" -> TELEGRAM
                "GET_TEXT" -> GET_TEXT
                "FIND_TEXT" -> FIND_TEXT
                "BACK" -> BACK
                "HOME" -> HOME
                "RECENTS" -> RECENTS
                "FUNCTION" -> FUNCTION
                "CALL_FUNC" -> CALL_FUNC
                "RETURN" -> RETURN
                "COMMENT" -> COMMENT
                "BREAK" -> BREAK
                else -> null
            }
        }
        
        fun getAllTypes(): List<BlockType> {
            return listOf(
                CLICK, LONG_CLICK, SWIPE, TAP,
                SLEEP, WAIT_COLOR, WAIT_TEXT,
                IF_COLOR, IF_TEXT, IF_IMAGE,
                LOOP, LOOP_COUNT,
                SET_VAR, INC_VAR, DEC_VAR,
                LOG, TOAST, TELEGRAM,
                GET_TEXT, FIND_TEXT,
                BACK, HOME, RECENTS,
                FUNCTION, CALL_FUNC, RETURN,
                COMMENT, BREAK
            )
        }
        
        fun getActionBlocks(): List<BlockType> = listOf(CLICK, LONG_CLICK, SWIPE, TAP)
        fun getWaitBlocks(): List<BlockType> = listOf(SLEEP, WAIT_COLOR, WAIT_TEXT)
        fun getConditionBlocks(): List<BlockType> = listOf(IF_COLOR, IF_TEXT, IF_IMAGE)
        fun getLoopBlocks(): List<BlockType> = listOf(LOOP, LOOP_COUNT)
        fun getVariableBlocks(): List<BlockType> = listOf(SET_VAR, INC_VAR, DEC_VAR)
        fun getOutputBlocks(): List<BlockType> = listOf(LOG, TOAST, TELEGRAM)
        fun getOcrBlocks(): List<BlockType> = listOf(GET_TEXT, FIND_TEXT)
        fun getSystemBlocks(): List<BlockType> = listOf(BACK, HOME, RECENTS)
        fun getFunctionBlocks(): List<BlockType> = listOf(FUNCTION, CALL_FUNC, RETURN)
        fun getSpecialBlocks(): List<BlockType> = listOf(COMMENT, BREAK)
    }
}
