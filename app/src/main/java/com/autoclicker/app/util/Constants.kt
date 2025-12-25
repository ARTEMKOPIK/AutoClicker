package com.autoclicker.app.util

/**
 * Centralized constants for AutoClicker application.
 * This file contains all timeout values, dimensions, notification IDs,
 * and other magic numbers used throughout the app.
 * 
 * Thread-safety: All constants are immutable (val) and therefore thread-safe.
 */
object Constants {

    // ========================================
    // NETWORK TIMEOUT VALUES
    // ========================================
    
    /** Default timeout for network operations (in seconds) */
    const val NETWORK_TIMEOUT_SECONDS = 15
    
    /** Timeout for Telegram API calls (in seconds) */
    const val TELEGRAM_TIMEOUT_SECONDS = 15
    
    /** Timeout for crash reporting (in seconds) */
    const val CRASH_REPORT_TIMEOUT_SECONDS = 10
    
    /** Delay before crash reporting terminates (in milliseconds) */
    const val CRASH_REPORT_DELAY_MS = 2000L
    
    // ========================================
    // SCREEN & DISPLAY VALUES
    // ========================================
    
    /** Fallback screen width if unable to detect actual dimensions */
    const val FALLBACK_SCREEN_WIDTH = 1080
    
    /** Fallback screen height if unable to detect actual dimensions */
    const val FALLBACK_SCREEN_HEIGHT = 1920
    
    /** Fallback screen density if unable to detect actual value */
    const val FALLBACK_SCREEN_DENSITY = 420
    
    /** Default ImageReader buffer size (number of images to keep in buffer) */
    const val IMAGEREADER_BUFFER_SIZE = 2
    
    /** Bitmap sample size for memory optimization */
    const val BITMAP_SAMPLE_SIZE = 1
    
    // ========================================
    // NOTIFICATION IDs
    // ========================================
    
    /** Notification ID for Screen Capture service */
    const val NOTIFICATION_ID_SCREEN_CAPTURE = 1001
    
    /** Notification ID for Floating Window service */
    const val NOTIFICATION_ID_FLOATING_WINDOW = 1002
    
    /** Notification ID for Color Picker service */
    const val NOTIFICATION_ID_COLOR_PICKER = 1003
    
    /** Notification ID for Macro Recorder service */
    const val NOTIFICATION_ID_MACRO_RECORDER = 1005
    
    /** Notification ID for Coordinate Overlay service */
    const val NOTIFICATION_ID_COORDINATE_OVERLAY = 1006
    
    /** Notification ID for Backup/Restore operations */
    const val NOTIFICATION_ID_BACKUP_RESTORE = 1007
    
    // ========================================
    // LOGGING & DEBUGGING
    // ========================================
    
    /** Maximum log message length before truncation */
    const val MAX_LOG_LENGTH = 1000
    
    /** Maximum stack trace length before truncation */
    const val MAX_STACK_TRACE_LENGTH = 2500
    
    /** Maximum floating window log buffer size */
    const val MAX_FLOATING_WINDOW_LOG_LENGTH = 5000
    
    /** Floating window log trim size (how much to keep after trimming) */
    const val FLOATING_WINDOW_LOG_TRIM_SIZE = 4000
    
    /** Time format pattern for logs */
    const val LOG_TIME_FORMAT_PATTERN = "HH:mm:ss"
    
    /** Time format pattern for scripts */
    const val SCRIPT_DATE_FORMAT_PATTERN = "dd.MM.yyyy HH:mm"
    
    /** Time format pattern for crash reports */
    const val CRASH_REPORT_DATE_PATTERN = "dd.MM.yyyy HH:mm:ss"

    /** Log retention period (in days) */
    const val LOG_RETENTION_DAYS = 7

    /** Maximum log file size (in bytes) before rotation */
    const val MAX_LOG_FILE_SIZE = 10 * 1024 * 1024L // 10MB
    
    // ========================================
    // UI & USER EXPERIENCE
    // ========================================
    
    /** Touch click threshold (in pixels) - maximum distance for a click vs drag */
    const val TOUCH_CLICK_THRESHOLD_PX = 10
    
    /** Default floating window X position */
    const val DEFAULT_FLOATING_WINDOW_X = 50
    
    /** Default floating window Y position */
    const val DEFAULT_FLOATING_WINDOW_Y = 200
    
    /** Default color picker X position */
    const val DEFAULT_PICKER_X = 20
    
    /** Default color picker Y position */
    const val DEFAULT_PICKER_Y = 100
    
    /** Default accent color (Material Orange) */
    val DEFAULT_ACCENT_COLOR = 0xFFFF5722.toInt()

    /** Maximum color history size */
    const val MAX_HISTORY_COLOR = 6
    
    // ========================================
    // SCRIPT EDITOR & HIGHLIGHTING
    // ========================================

    /** Maximum text length for syntax highlighting (chars) */
    const val MAX_TEXT_FOR_HIGHLIGHT_CHARS = 15000

    /** Delay for syntax highlighting (milliseconds) */
    const val HIGHLIGHT_DELAY_MS = 150L

    /** Maximum quick actions allowed */
    const val MAX_QUICK_ACTIONS = 50

    /** Maximum macro actions allowed in a single recording */
    const val MAX_MACRO_ACTIONS = 1000

    /** Maximum delay between macro actions (milliseconds) */
    const val MAX_DELAY_MS = 60000L

    /** Threshold for long delay alert in macro recorder (milliseconds) */
    const val MAX_DELAY_ALERT_MS = 30000L
    
    // ========================================
    // SCRIPT EXECUTION
    // ========================================
    
    /** Maximum script execution timeout (in milliseconds) */
    const val SCRIPT_EXECUTION_TIMEOUT_MS = 30000L
    
    /** Script stop monitoring delay (in milliseconds) */
    const val SCRIPT_STOP_MONITOR_DELAY_MS = 3000L
    
    /** Coroutine supervisor timeout (in milliseconds) */
    const val COROUTINE_SUPERVISOR_TIMEOUT_MS = 5000L
    
    // ========================================
    // PREFERENCE KEYS
    // ========================================

    const val PREFS_COLOR_HISTORY_KEY = "color_picker_history"
    const val PREFS_KEY_HISTORY = "history"
    const val QUICK_ACTIONS_KEY = "quick_actions"
    const val PREFS_KEY_ACTIONS = "actions"
    const val PREFS_KEY_QUICK_ACTIONS_BACKUP = "actions_backup"

    // ========================================
    // BACKUP & RECOVERY
    // ========================================
    
    /** Maximum backup file size (in bytes) */
    const val MAX_BACKUP_FILE_SIZE = 10 * 1024 * 1024 // 10MB
    
    /** Backup file extension */
    const val BACKUP_FILE_EXTENSION = ".autoclicker_backup"
    
    /** Number of backup versions to keep */
    const val BACKUP_VERSIONS_TO_KEEP = 5
    
    // ========================================
    // FILE I/O
    // ========================================
    
    /** Default buffer size for file operations */
    const val FILE_BUFFER_SIZE = 8192
    
    /** Crash log file name */
    const val CRASH_LOG_FILENAME = "crash_log.txt"
    
    // ========================================
    // SCHEDULER
    // ========================================
    
    /** Monday constant for scheduler (Calendar.SUNDAY = 1, Calendar.MONDAY = 2, etc.) */
    const val SCHEDULER_MONDAY = 2
    
    /** Sunday constant for scheduler */
    const val SCHEDULER_SUNDAY = 1
    
    /** Minimum scheduler interval (in milliseconds) */
    const val MIN_SCHEDULER_INTERVAL_MS = 60000L // 1 minute
    
    // Threading should use kotlin coroutine dispatchers directly:
    // - kotlinx.coroutines.Dispatchers.IO for I/O operations
    // - kotlinx.coroutines.Dispatchers.Main for UI operations
    // - kotlinx.coroutines.Dispatchers.Default for CPU-intensive operations

    // ========================================
    // ERROR MESSAGES
    // ========================================
    
    const val ERROR_SCRIPT_NOT_FOUND = "Скрипт не найден"
    const val ERROR_SCRIPT_EMPTY = "Скрипт пустой или не найден"
    const val ERROR_ACCESSIBILITY_NOT_ENABLED = "Accessibility не включён!"
    const val ERROR_SCREEN_CAPTURE_NOT_RUNNING = "Захват экрана не активен!"
    const val ERROR_TELEGRAM_CREDENTIALS_EMPTY = "Token или ChatId пустой"
    const val ERROR_BITMAP_RECYCLED = "Bitmap is recycled"
    const val ERROR_IMAGE_COMPRESSION = "Ошибка сжатия изображения"
    const val ERROR_INVALID_SCREEN_DIMENSIONS = "Неверные размеры экрана"
    
    // ========================================
    // STATUS MESSAGES
    // ========================================
    
    const val STATUS_SCRIPT_STARTED = "▶️ Запущен: "
    const val STATUS_SCRIPT_COMPLETED = "✅ Скрипт завершён"
    const val STATUS_SCRIPT_STOPPED = "⏹️ Скрипт остановлен"
    const val STATUS_SCRIPT_ALREADY_RUNNING = "⚠️ Скрипт уже запущен"
    const val STATUS_SCRIPT_SELECTED = "📝 Выбран: "
    
    // ========================================
    // VALIDATION REGEX
    // ========================================
    
    /** Regex for validating Telegram bot token format */
    const val TELEGRAM_BOT_TOKEN_REGEX = "^\\d+:[A-Za-z0-9_-]+$"
    
    /** Regex for validating Telegram chat ID format */
    const val TELEGRAM_CHAT_ID_REGEX = "^-?\\d+$"
    
    // ========================================
    // SYSTEM VALUES
    // ========================================
    
    /** Android API level 23 (Marshmallow) */
    const val API_LEVEL_MARSHMALLOW = 23
    
    /** Android API level 26 (Oreo) */
    const val API_LEVEL_OREO = 26
    
    /** Android API level 28 (Pie) */
    const val API_LEVEL_PIE = 28
    
    /** Android API level 30 (R) */
    const val API_LEVEL_R = 30
    
    /** Android API level 33 (Tiramisu) */
    const val API_LEVEL_TIRAMISU = 33
    
    /** Android API level 34 (UpsideDownCake) */
    const val API_LEVEL_UPSIDE_DOWN_CAKE = 34
}