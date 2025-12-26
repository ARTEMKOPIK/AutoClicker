package com.autoclicker.app.data

/**
 * Типы блоков для визуального редактора
 */
enum class BlockType {
    // Actions
    CLICK,
    LONG_CLICK,
    SWIPE,
    TAP,
    
    // Wait
    SLEEP,
    WAIT_COLOR,
    WAIT_TEXT,
    
    // Conditions
    IF_COLOR,
    IF_TEXT,
    IF_IMAGE,
    
    // Loops
    LOOP,
    LOOP_COUNT,
    
    // Variables
    SET_VAR,
    INC_VAR,
    DEC_VAR,
    
    // Output
    LOG,
    TOAST,
    TELEGRAM,
    
    // OCR
    GET_TEXT,
    FIND_TEXT,
    
    // System
    BACK,
    HOME,
    RECENTS,
    
    // Functions
    FUNCTION,
    CALL_FUNC,
    RETURN,
    
    // Special
    COMMENT,
    BREAK
}

