package com.autoclicker.app.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Утилита для работы с датой и временем
 * Форматирование, парсинг и вычисления
 */
object DateTimeUtils {
    
    private val locale = Locale.getDefault()
    
    /**
     * Форматы дат
     */
    object Formats {
        const val FULL_DATE_TIME = "dd.MM.yyyy HH:mm:ss"
        const val DATE_TIME = "dd.MM.yyyy HH:mm"
        const val DATE = "dd.MM.yyyy"
        const val TIME = "HH:mm:ss"
        const val TIME_SHORT = "HH:mm"
        const val ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        const val FILE_NAME = "yyyyMMdd_HHmmss"
    }
    
    /**
     * Получить текущее время в миллисекундах
     */
    fun now(): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * Форматировать дату
     */
    fun format(timestamp: Long, pattern: String = Formats.FULL_DATE_TIME): String {
        return try {
            val sdf = SimpleDateFormat(pattern, locale)
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Форматировать текущую дату
     */
    fun formatNow(pattern: String = Formats.FULL_DATE_TIME): String {
        return format(now(), pattern)
    }
    
    /**
     * Парсить дату из строки
     */
    fun parse(dateString: String, pattern: String = Formats.FULL_DATE_TIME): Long? {
        return try {
            val sdf = SimpleDateFormat(pattern, locale)
            sdf.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Форматировать длительность в читаемый вид
     */
    fun formatDuration(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        
        return when {
            hours > 0 -> String.format("%dч %dм %dс", hours, minutes, seconds)
            minutes > 0 -> String.format("%dм %dс", minutes, seconds)
            else -> String.format("%dс", seconds)
        }
    }
    
    /**
     * Форматировать длительность в компактный вид
     */
    fun formatDurationCompact(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        
        return when {
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
            minutes > 0 -> String.format("%02d:%02d", minutes, seconds)
            else -> String.format("00:%02d", seconds)
        }
    }
    
    /**
     * Получить относительное время (например: "5 минут назад")
     */
    fun getRelativeTime(timestamp: Long): String {
        val now = now()
        val diff = now - timestamp
        
        return when {
            diff < 0 -> "в будущем"
            diff < TimeUnit.MINUTES.toMillis(1) -> "только что"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "$minutes мин назад"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "$hours ч назад"
            }
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "$days дн назад"
            }
            diff < TimeUnit.DAYS.toMillis(30) -> {
                val weeks = TimeUnit.MILLISECONDS.toDays(diff) / 7
                "$weeks нед назад"
            }
            diff < TimeUnit.DAYS.toMillis(365) -> {
                val months = TimeUnit.MILLISECONDS.toDays(diff) / 30
                "$months мес назад"
            }
            else -> {
                val years = TimeUnit.MILLISECONDS.toDays(diff) / 365
                "$years лет назад"
            }
        }
    }
    
    /**
     * Проверить, сегодняшняя ли дата
     */
    fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Проверить, вчерашняя ли дата
     */
    fun isYesterday(timestamp: Long): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        return yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Добавить дни к дате
     */
    fun addDays(timestamp: Long, days: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            add(Calendar.DAY_OF_YEAR, days)
        }
        return calendar.timeInMillis
    }
    
    /**
     * Добавить часы к дате
     */
    fun addHours(timestamp: Long, hours: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            add(Calendar.HOUR_OF_DAY, hours)
        }
        return calendar.timeInMillis
    }
    
    /**
     * Добавить минуты к дате
     */
    fun addMinutes(timestamp: Long, minutes: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            add(Calendar.MINUTE, minutes)
        }
        return calendar.timeInMillis
    }
    
    /**
     * Получить начало дня
     */
    fun getStartOfDay(timestamp: Long = now()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    /**
     * Получить конец дня
     */
    fun getEndOfDay(timestamp: Long = now()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return calendar.timeInMillis
    }
    
    /**
     * Получить разницу между датами в днях
     */
    fun getDaysDifference(timestamp1: Long, timestamp2: Long): Long {
        val diff = kotlin.math.abs(timestamp1 - timestamp2)
        return TimeUnit.MILLISECONDS.toDays(diff)
    }
    
    /**
     * Получить разницу между датами в часах
     */
    fun getHoursDifference(timestamp1: Long, timestamp2: Long): Long {
        val diff = kotlin.math.abs(timestamp1 - timestamp2)
        return TimeUnit.MILLISECONDS.toHours(diff)
    }
    
    /**
     * Получить разницу между датами в минутах
     */
    fun getMinutesDifference(timestamp1: Long, timestamp2: Long): Long {
        val diff = kotlin.math.abs(timestamp1 - timestamp2)
        return TimeUnit.MILLISECONDS.toMinutes(diff)
    }
    
    /**
     * Получить день недели
     */
    fun getDayOfWeek(timestamp: Long = now()): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Понедельник"
            Calendar.TUESDAY -> "Вторник"
            Calendar.WEDNESDAY -> "Среда"
            Calendar.THURSDAY -> "Четверг"
            Calendar.FRIDAY -> "Пятница"
            Calendar.SATURDAY -> "Суббота"
            Calendar.SUNDAY -> "Воскресенье"
            else -> ""
        }
    }
    
    /**
     * Получить месяц
     */
    fun getMonth(timestamp: Long = now()): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (calendar.get(Calendar.MONTH)) {
            Calendar.JANUARY -> "Январь"
            Calendar.FEBRUARY -> "Февраль"
            Calendar.MARCH -> "Март"
            Calendar.APRIL -> "Апрель"
            Calendar.MAY -> "Май"
            Calendar.JUNE -> "Июнь"
            Calendar.JULY -> "Июль"
            Calendar.AUGUST -> "Август"
            Calendar.SEPTEMBER -> "Сентябрь"
            Calendar.OCTOBER -> "Октябрь"
            Calendar.NOVEMBER -> "Ноябрь"
            Calendar.DECEMBER -> "Декабрь"
            else -> ""
        }
    }
    
    /**
     * Класс для хранения детальной информации о дате
     */
    data class DateInfo(
        val timestamp: Long,
        val formatted: String,
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val minute: Int,
        val second: Int,
        val dayOfWeek: String,
        val monthName: String,
        val isToday: Boolean,
        val isYesterday: Boolean,
        val relativeTime: String
    )
    
    /**
     * Получить детальную информацию о дате
     */
    fun getDateInfo(timestamp: Long = now()): DateInfo {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        return DateInfo(
            timestamp = timestamp,
            formatted = format(timestamp),
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH),
            hour = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE),
            second = calendar.get(Calendar.SECOND),
            dayOfWeek = getDayOfWeek(timestamp),
            monthName = getMonth(timestamp),
            isToday = isToday(timestamp),
            isYesterday = isYesterday(timestamp),
            relativeTime = getRelativeTime(timestamp)
        )
    }
}

