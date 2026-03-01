package com.autoclicker.app.data

/**
 * Категории сниппетов
 */
enum class SnippetCategory {
    BASIC,          // Базовые примеры
    GAMES,          // Игровые скрипты
    AUTOMATION,     // Автоматизация задач
    TESTING,        // Тестирование UI
    ADVANCED        // Продвинутые техники
}

/**
 * Модель сниппета кода
 */
data class CodeSnippet(
    val id: String,
    val nameResId: Int,              // Ресурс названия
    val descriptionResId: Int,       // Ресурс описания
    val category: SnippetCategory,
    val code: String,
    val tags: List<String> = emptyList(),
    var isFavorite: Boolean = false
) {
    companion object {
        /**
         * Получить все предустановленные сниппеты
         */
        fun getDefaultSnippets(): List<CodeSnippet> {
            return listOf(
                // BASIC сниппеты
                CodeSnippet(
                    id = "basic_click",
                    nameResId = R.string.snippet_basic_click_name,
                    descriptionResId = R.string.snippet_basic_click_desc,
                    category = SnippetCategory.BASIC,
                    code = """
                        // Простой клик по координатам
                        click(500, 1000)
                        sleep(1000)
                    """.trimIndent(),
                    tags = listOf("click", "basic", "simple")
                ),
                
                CodeSnippet(
                    id = "basic_loop",
                    nameResId = R.string.snippet_basic_loop_name,
                    descriptionResId = R.string.snippet_basic_loop_desc,
                    category = SnippetCategory.BASIC,
                    code = """
                        // Цикл с кликами и задержкой
                        for (i = 0; i < 10; i++) {
                            click(500, 1000)
                            sleep(1000)
                        }
                    """.trimIndent(),
                    tags = listOf("loop", "basic", "repeat")
                ),
                
                CodeSnippet(
                    id = "basic_swipe",
                    nameResId = R.string.snippet_basic_swipe_name,
                    descriptionResId = R.string.snippet_basic_swipe_desc,
                    category = SnippetCategory.BASIC,
                    code = """
                        // Свайп вниз для обновления
                        swipe(500, 800, 500, 1500, 500)
                        sleep(2000)
                    """.trimIndent(),
                    tags = listOf("swipe", "basic", "gesture")
                ),
                
                CodeSnippet(
                    id = "basic_conditional",
                    nameResId = R.string.snippet_basic_conditional_name,
                    descriptionResId = R.string.snippet_basic_conditional_desc,
                    category = SnippetCategory.BASIC,
                    code = """
                        // Условный клик по цвету
                        if (getColor(100, 200) == #FF0000) {
                            click(300, 400)
                            log("Красный цвет найден!")
                        }
                    """.trimIndent(),
                    tags = listOf("condition", "color", "if")
                ),
                
                // GAMES сниппеты
                CodeSnippet(
                    id = "game_auto_clicker",
                    nameResId = R.string.snippet_game_auto_clicker_name,
                    descriptionResId = R.string.snippet_game_auto_clicker_desc,
                    category = SnippetCategory.GAMES,
                    code = """
                        // Авто-кликер для игр
                        setVar("clicks", 0)
                        
                        while (true) {
                            click(540, 960)
                            incVar("clicks")
                            
                            if (getVar("clicks") % 100 == 0) {
                                toast("Кликов: " + getVar("clicks"))
                            }
                            
                            sleep(50)
                        }
                    """.trimIndent(),
                    tags = listOf("game", "clicker", "auto")
                ),
                
                CodeSnippet(
                    id = "game_farm_resources",
                    nameResId = R.string.snippet_game_farm_resources_name,
                    descriptionResId = R.string.snippet_game_farm_resources_desc,
                    category = SnippetCategory.GAMES,
                    code = """
                        // Фарм ресурсов с проверкой
                        function collectResource() {
                            // Проверяем наличие кнопки сбора (зелёный цвет)
                            if (getColor(540, 1200) == #00FF00) {
                                click(540, 1200)
                                sleep(500)
                                toast("Ресурс собран!")
                                return true
                            }
                            return false
                        }
                        
                        // Основной цикл
                        for (i = 0; i < 50; i++) {
                            if (collectResource()) {
                                sleep(2000)
                            } else {
                                log("Ресурс недоступен, ждём...")
                                sleep(5000)
                            }
                        }
                    """.trimIndent(),
                    tags = listOf("game", "farm", "resources")
                ),
                
                CodeSnippet(
                    id = "game_quest_complete",
                    nameResId = R.string.snippet_game_quest_complete_name,
                    descriptionResId = R.string.snippet_game_quest_complete_desc,
                    category = SnippetCategory.GAMES,
                    code = """
                        // Автоматическое выполнение квестов
                        setVar("quests", 0)
                        
                        while (getVar("quests") < 10) {
                            // Открываем меню квестов
                            click(100, 100)
                            sleep(1000)
                            
                            // Ищем кнопку "Получить награду"
                            if (findText("Получить") != null) {
                                click(540, 1400)
                                sleep(1000)
                                incVar("quests")
                                toast("Квест " + getVar("quests") + " завершён!")
                            } else {
                                log("Нет доступных квестов")
                                break
                            }
                            
                            // Закрываем меню
                            back()
                            sleep(1000)
                        }
                        
                        toast("Выполнено квестов: " + getVar("quests"))
                    """.trimIndent(),
                    tags = listOf("game", "quest", "auto")
                ),
                
                // AUTOMATION сниппеты
                CodeSnippet(
                    id = "auto_form_fill",
                    nameResId = R.string.snippet_auto_form_fill_name,
                    descriptionResId = R.string.snippet_auto_form_fill_desc,
                    category = SnippetCategory.AUTOMATION,
                    code = """
                        // Автозаполнение формы
                        function fillField(x, y, text) {
                            click(x, y)
                            sleep(500)
                            input(text)
                            sleep(300)
                        }
                        
                        fillField(300, 400, "Иван Иванов")
                        fillField(300, 600, "ivan@example.com")
                        fillField(300, 800, "+7 999 123 45 67")
                        
                        // Отправка формы
                        click(540, 1400)
                        sleep(2000)
                        toast("Форма отправлена!")
                    """.trimIndent(),
                    tags = listOf("automation", "form", "input")
                ),
                
                CodeSnippet(
                    id = "auto_scroll_content",
                    nameResId = R.string.snippet_auto_scroll_content_name,
                    descriptionResId = R.string.snippet_auto_scroll_content_desc,
                    category = SnippetCategory.AUTOMATION,
                    code = """
                        // Прокрутка контента с паузами
                        setVar("scrolls", 0)
                        
                        while (getVar("scrolls") < 20) {
                            // Свайп вверх для прокрутки вниз
                            swipe(540, 1500, 540, 500, 500)
                            sleep(2000)
                            
                            // Проверяем достижение конца
                            if (getColor(540, 1800) == #FFFFFF) {
                                log("Достигнут конец контента")
                                break
                            }
                            
                            incVar("scrolls")
                        }
                        
                        toast("Прокручено " + getVar("scrolls") + " раз")
                    """.trimIndent(),
                    tags = listOf("automation", "scroll", "swipe")
                ),
                
                CodeSnippet(
                    id = "auto_screenshot_monitor",
                    nameResId = R.string.snippet_auto_screenshot_monitor_name,
                    descriptionResId = R.string.snippet_auto_screenshot_monitor_desc,
                    category = SnippetCategory.AUTOMATION,
                    code = """
                        // Мониторинг экрана с скриншотами
                        setVar("checks", 0)
                        
                        while (getVar("checks") < 100) {
                            // Проверяем определённый пиксель
                            color = getColor(540, 960)
                            
                            if (color == #FF0000) {
                                // Обнаружено изменение - делаем скриншот
                                screenshot("alert_" + getVar("checks") + ".png")
                                toast("⚠️ Обнаружено изменение!")
                                sendTelegram("Внимание! Изменение на экране!")
                            }
                            
                            incVar("checks")
                            sleep(5000)
                        }
                    """.trimIndent(),
                    tags = listOf("automation", "monitor", "screenshot")
                ),
                
                // TESTING сниппеты
                CodeSnippet(
                    id = "test_ui_elements",
                    nameResId = R.string.snippet_test_ui_elements_name,
                    descriptionResId = R.string.snippet_test_ui_elements_desc,
                    category = SnippetCategory.TESTING,
                    code = """
                        // Тестирование UI элементов
                        function testButton(x, y, expectedColor, name) {
                            color = getColor(x, y)
                            if (color == expectedColor) {
                                log("✅ " + name + " OK")
                                return true
                            } else {
                                log("❌ " + name + " FAIL")
                                return false
                            }
                        }
                        
                        setVar("passed", 0)
                        setVar("failed", 0)
                        
                        // Тесты
                        if (testButton(100, 200, #0000FF, "Синяя кнопка")) {
                            incVar("passed")
                        } else {
                            incVar("failed")
                        }
                        
                        if (testButton(300, 200, #00FF00, "Зелёная кнопка")) {
                            incVar("passed")
                        } else {
                            incVar("failed")
                        }
                        
                        toast("Пройдено: " + getVar("passed") + " | Провалено: " + getVar("failed"))
                    """.trimIndent(),
                    tags = listOf("testing", "ui", "validation")
                ),
                
                CodeSnippet(
                    id = "test_navigation",
                    nameResId = R.string.snippet_test_navigation_name,
                    descriptionResId = R.string.snippet_test_navigation_desc,
                    category = SnippetCategory.TESTING,
                    code = """
                        // Тестирование навигации
                        function navigateAndVerify(x, y, verifyX, verifyY, expectedColor, screenName) {
                            click(x, y)
                            sleep(1000)
                            
                            if (getColor(verifyX, verifyY) == expectedColor) {
                                log("✅ Переход на " + screenName + " успешен")
                                return true
                            } else {
                                log("❌ Не удалось перейти на " + screenName)
                                return false
                            }
                        }
                        
                        // Тест навигации по экранам
                        navigateAndVerify(100, 1800, 540, 100, #FF0000, "Главная")
                        sleep(1000)
                        back()
                        sleep(1000)
                        
                        navigateAndVerify(300, 1800, 540, 100, #00FF00, "Профиль")
                        sleep(1000)
                        back()
                        sleep(1000)
                        
                        toast("Тест навигации завершён")
                    """.trimIndent(),
                    tags = listOf("testing", "navigation", "screens")
                ),
                
                // ADVANCED сниппеты
                CodeSnippet(
                    id = "advanced_ocr_data",
                    nameResId = R.string.snippet_advanced_ocr_data_name,
                    descriptionResId = R.string.snippet_advanced_ocr_data_desc,
                    category = SnippetCategory.ADVANCED,
                    code = """
                        // Извлечение данных с экрана через OCR
                        function extractData(x, y, w, h, label) {
                            text = getText(x, y, w, h)
                            log(label + ": " + text)
                            return text
                        }
                        
                        // Извлекаем различные данные
                        username = extractData(100, 200, 400, 50, "Имя пользователя")
                        balance = extractData(100, 300, 400, 50, "Баланс")
                        level = extractData(100, 400, 400, 50, "Уровень")
                        
                        // Отправляем отчёт
                        report = "Данные:\n"
                        report = report + "User: " + username + "\n"
                        report = report + "Balance: " + balance + "\n"
                        report = report + "Level: " + level
                        
                        sendTelegram(report)
                        toast("Данные извлечены и отправлены!")
                    """.trimIndent(),
                    tags = listOf("advanced", "ocr", "data")
                ),
                
                CodeSnippet(
                    id = "advanced_image_search",
                    nameResId = R.string.snippet_advanced_image_search_name,
                    descriptionResId = R.string.snippet_advanced_image_search_desc,
                    category = SnippetCategory.ADVANCED,
                    code = """
                        // Поиск и клик по изображению
                        function findAndClick(templateName, maxAttempts) {
                            for (attempt = 0; attempt < maxAttempts; attempt++) {
                                pos = findImage(templateName)
                                
                                if (pos != null) {
                                    click(pos.x, pos.y)
                                    log("✅ Найдено: " + templateName)
                                    return true
                                }
                                
                                log("⏳ Попытка " + (attempt + 1) + "/" + maxAttempts)
                                sleep(1000)
                            }
                            
                            log("❌ Не найдено: " + templateName)
                            return false
                        }
                        
                        // Поиск кнопок
                        if (findAndClick("start_button.png", 10)) {
                            sleep(2000)
                            findAndClick("confirm_button.png", 5)
                        }
                    """.trimIndent(),
                    tags = listOf("advanced", "image", "search")
                ),
                
                CodeSnippet(
                    id = "advanced_multi_account",
                    nameResId = R.string.snippet_advanced_multi_account_name,
                    descriptionResId = R.string.snippet_advanced_multi_account_desc,
                    category = SnippetCategory.ADVANCED,
                    code = """
                        // Управление несколькими аккаунтами
                        accounts = ["user1@mail.com", "user2@mail.com", "user3@mail.com"]
                        
                        function switchAccount(email) {
                            // Открываем меню
                            click(100, 100)
                            sleep(1000)
                            
                            // Выход из аккаунта
                            click(540, 1500)
                            sleep(2000)
                            
                            // Вход в новый аккаунт
                            click(540, 800)
                            sleep(500)
                            input(email)
                            click(540, 1000)
                            sleep(3000)
                            
                            log("Переключено на: " + email)
                        }
                        
                        function collectDailyReward() {
                            click(540, 960)
                            sleep(2000)
                            toast("Награда собрана!")
                        }
                        
                        // Обрабатываем все аккаунты
                        for (i = 0; i < accounts.length; i++) {
                            switchAccount(accounts[i])
                            collectDailyReward()
                            sleep(2000)
                        }
                        
                        toast("Обработано аккаунтов: " + accounts.length)
                    """.trimIndent(),
                    tags = listOf("advanced", "multi-account", "automation")
                )
            )
        }
    }
}

