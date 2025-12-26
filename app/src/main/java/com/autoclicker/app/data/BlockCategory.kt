package com.autoclicker.app.data

/**
 * Категории блоков для визуального редактора
 */
enum class BlockCategory {
    ACTIONS,        // Действия (клики, свайпы)
    WAIT,          // Ожидание
    CONDITIONS,    // Условия
    LOOPS,         // Циклы
    VARIABLES,     // Переменные
    OUTPUT,        // Вывод (логи, тосты, telegram)
    OCR,           // Распознавание текста
    SYSTEM,        // Системные действия
    FUNCTIONS,     // Функции
    SPECIAL        // Специальные (комментарии, break)
}

