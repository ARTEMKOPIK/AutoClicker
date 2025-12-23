package com.autoclicker.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.autoclicker.app.service.FloatingWindowService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.Date

/**
 * Планировщик запуска скриптов по расписанию
 * 
 * Поддерживает как однократные, так и повторяющиеся задачи.
 * Для повторяющихся задач использует setExactAndAllowWhileIdle и перепланирует
 * после каждого выполнения, что более надёжно на Android 6+.
 * 
 * Thread-safety: Все операции с SharedPreferences и AlarmManager синхронизированы.
 * 
 * @property context Application context for accessing system services
 */
class ScriptScheduler(private val context: Context) {

    private val prefs = context.getSharedPreferences("scheduler", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val lock = Any()

    data class ScheduledTask(
        val id: String = java.util.UUID.randomUUID().toString(),
        val scriptId: String,
        val scriptName: String,
        val hour: Int,
        val minute: Int,
        // Повторяющиеся дни: используем константы Calendar (1=Вс, 2=Пн, 3=Вт, 4=Ср, 5=Чт, 6=Пт, 7=Сб)
        // Пустой список = однократная задача
        val repeatDays: List<Int> = emptyList(),
        val enabled: Boolean = true,
        val createdAt: Long = System.currentTimeMillis()
    )

    /**
     * Schedule a task (create or update).
     * 
     * @param task The task to schedule
     * @throws IllegalArgumentException if task has invalid parameters
     */
    fun scheduleTask(task: ScheduledTask) {
        // Validate task parameters
        validateTask(task)
        
        synchronized(lock) {
            try {
                // Сохраняем задачу
                val tasks = getAllTasksInternal().toMutableList()
                val existingIndex = tasks.indexOfFirst { it.id == task.id }
                if (existingIndex >= 0) {
                    tasks[existingIndex] = task
                } else {
                    tasks.add(task)
                }
                prefs.edit().putString("tasks_list", gson.toJson(tasks)).apply()

                // Устанавливаем alarm
                if (task.enabled) {
                    setAlarm(task)
                }
                
                Log.i("ScriptScheduler", "Task ${task.id} scheduled for ${task.hour}:${task.minute}")
            } catch (e: Exception) {
                Log.e("ScriptScheduler", "Error scheduling task", e)
                CrashHandler.logError("ScriptScheduler", "Ошибка планирования задачи '${task.scriptName}'", e)
                throw e
            }
        }
    }
    
    /**
     * Validate task parameters to prevent scheduling errors.
     * 
     * @param task The task to validate
     * @throws IllegalArgumentException if validation fails
     */
    private fun validateTask(task: ScheduledTask) {
        when {
            task.hour < 0 || task.hour > 23 -> throw IllegalArgumentException("Invalid hour: ${task.hour}")
            task.minute < 0 || task.minute > 59 -> throw IllegalArgumentException("Invalid minute: ${task.minute}")
            task.scriptId.isEmpty() -> throw IllegalArgumentException("Empty script ID")
            task.scriptName.isEmpty() -> throw IllegalArgumentException("Empty script name")
            task.repeatDays.any { it < 1 || it > 7 } -> throw IllegalArgumentException("Invalid day of week in repeatDays")
        }
    }

    fun cancelTask(taskId: String) {
        synchronized(lock) {
            val task = getTaskInternal(taskId) ?: return
            cancelAlarm(task)

            val tasks = getAllTasksInternal().filter { it.id != taskId }
            prefs.edit().putString("tasks_list", gson.toJson(tasks)).apply()
        }
    }

    fun enableTask(taskId: String, enabled: Boolean) {
        synchronized(lock) {
            val task = getTaskInternal(taskId) ?: return
            val updated = task.copy(enabled = enabled)
            
            val tasks = getAllTasksInternal().toMutableList()
            val index = tasks.indexOfFirst { it.id == taskId }
            if (index >= 0) {
                tasks[index] = updated
                prefs.edit().putString("tasks_list", gson.toJson(tasks)).apply()
            }

            if (enabled) {
                setAlarm(updated)
            } else {
                cancelAlarm(task)
            }
        }
    }

    fun getTask(id: String): ScheduledTask? {
        synchronized(lock) {
            return getTaskInternal(id)
        }
    }
    
    private fun getTaskInternal(id: String): ScheduledTask? {
        return getAllTasksInternal().find { it.id == id }
    }

    fun getAllTasks(): List<ScheduledTask> {
        synchronized(lock) {
            return getAllTasksInternal()
        }
    }

    /**
     * Get all scheduled tasks from storage (internal method).
     * 
     * Thread-safety: Must be called from within synchronized block.
     * Error handling: Logs corruption with context, returns empty list on parse error.
     * 
     * @return List of all scheduled tasks, empty list if corrupted or not found
     */
    private fun getAllTasksInternal(): List<ScheduledTask> {
        val json = prefs.getString("tasks_list", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ScheduledTask>>() {}.type
            gson.fromJson<List<ScheduledTask>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            // Log corrupted data with context - CRITICAL for debugging data loss
            val errorMessage = """
                ScriptScheduler data corruption detected!
                
                JSON Length: ${json.length}
                JSON Preview: ${json.take(200)}
                Error: ${e.javaClass.simpleName}: ${e.message}
            """.trimIndent()
            
            Log.e("ScriptScheduler", errorMessage, e)
            CrashHandler.logError("ScriptScheduler", errorMessage, e)
            
            // Return empty list to prevent app crash, but data is lost
            // User should be notified about this issue
            emptyList()
        }
    }

    /**
     * Calculate the next execution time for a task.
     * 
     * @param task The scheduled task
     * @return Unix timestamp of next execution
     */
    private fun calculateNextExecutionTime(task: ScheduledTask): Long {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, task.hour)
            set(Calendar.MINUTE, task.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return when {
            // Если время уже прошло сегодня и задача не повторяется по дням недели
            calendar.timeInMillis <= now && task.repeatDays.isEmpty() -> {
                // Следующий день
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                calendar.timeInMillis
            }
            // Если задача повторяется по дням недели
            task.repeatDays.isNotEmpty() -> {
                findNextDayInPattern(now, calendar, task.repeatDays)
            }
            // Если время ещё не прошло сегодня
            else -> {
                calendar.timeInMillis
            }
        }
    }
    
    /**
     * Find the next day that matches the weekly pattern.
     * 
     * @param now Current time in milliseconds
     * @param calendar Calendar set to today's execution time
     * @param repeatDays List of days of week (1=Sunday, 2=Monday, ..., 7=Saturday)
     * @return Unix timestamp of next execution
     */
    private fun findNextDayInPattern(now: Long, calendar: Calendar, repeatDays: List<Int>): Long {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val sortedDays = repeatDays.sorted()
        
        // Find next matching day
        val nextDay = sortedDays.firstOrNull { it > currentDay } 
            ?: sortedDays.first() // Wrap around to first day of week
            
        val daysToAdd = when {
            nextDay > currentDay -> nextDay - currentDay
            else -> 7 - currentDay + nextDay // Next week
        }
        
        val targetTime = calendar.timeInMillis + (daysToAdd * 24 * 60 * 60 * 1000)
        
        // If the target time is in the past (shouldn't happen with correct calculation),
        // add another week
        return if (targetTime <= now) {
            targetTime + (7 * 24 * 60 * 60 * 1000)
        } else {
            targetTime
        }
    }
    
    /**
     * Get the next occurrence of a weekday.
     * 
     * @param dayOfWeek Day of week (Calendar.SUNDAY = 1, Calendar.MONDAY = 2, etc.)
     * @return Unix timestamp of next occurrence
     */
    private fun getNextOccurrence(dayOfWeek: Int): Long {
        val calendar = Calendar.getInstance()
        val daysUntilTarget = (dayOfWeek - calendar.get(Calendar.DAY_OF_WEEK) + 7) % 7
        calendar.add(Calendar.DAY_OF_MONTH, if (daysUntilTarget == 0) 7 else daysUntilTarget)
        return calendar.timeInMillis
    }
    
    /**
     * Set alarm for task execution.
     * For repeating tasks, schedules the next occurrence after current execution.
     * 
     * @param task The task to schedule
     */
    private fun setAlarm(task: ScheduledTask) {
        val nextExecutionTime = calculateNextExecutionTime(task)
        
        val intent = Intent(context, SchedulerReceiver::class.java).apply {
            action = ACTION_RUN_SCRIPT
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_SCRIPT_ID, task.scriptId)
            putExtra(EXTRA_EXECUTE_TIME, nextExecutionTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Используем setExactAndAllowWhileIdle для всех случаев (более надёжно)
        // setRepeating не гарантирует точное время на Android 6+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextExecutionTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                nextExecutionTime,
                pendingIntent
            )
        }
        
        Log.i("ScriptScheduler", "Alarm set for task ${task.id} at ${Date(nextExecutionTime)}")
    }

    private fun cancelAlarm(task: ScheduledTask) {
        val intent = Intent(context, SchedulerReceiver::class.java).apply {
            action = ACTION_RUN_SCRIPT
            putExtra(EXTRA_TASK_ID, task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAllTasks() {
        synchronized(lock) {
            try {
                getAllTasksInternal().filter { it.enabled }.forEach { setAlarm(it) }
            } catch (e: Exception) {
                android.util.Log.e("ScriptScheduler", "Error rescheduling tasks", e)
            }
        }
    }

    companion object {
        const val ACTION_RUN_SCRIPT = "com.autoclicker.app.ACTION_RUN_SCHEDULED_SCRIPT"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_SCRIPT_ID = "script_id"
        const val EXTRA_EXECUTE_TIME = "execute_time"
    }

    /**
     * Broadcast receiver for scheduled task execution.
     * 
     * Handles both one-time and repeating tasks by rescheduling them
     * after execution if they have repeat patterns.
     */
    class SchedulerReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_RUN_SCRIPT) {
                Log.w("SchedulerReceiver", "Unknown action: ${intent.action}")
                return
            }
            
            val scriptId = intent.getStringExtra(EXTRA_SCRIPT_ID)
            val taskId = intent.getStringExtra(EXTRA_TASK_ID)
            val executeTime = intent.getLongExtra(EXTRA_EXECUTE_TIME, 0)
            
            // Validate execution time to prevent duplicate runs
            val now = System.currentTimeMillis()
            if (executeTime > 0 && now - executeTime > 300000) { // 5 минут - более разумный порог для AlarmManager
                Log.w("SchedulerReceiver", "Discarding stale scheduled execution for task $taskId (delay: ${now - executeTime}ms)")
                return
            }

            if (scriptId != null) {
                Toast.makeText(context, "Запуск запланированного скрипта", Toast.LENGTH_SHORT).show()
                FloatingWindowService.startService(context, scriptId)
                Log.i("SchedulerReceiver", "Started scheduled script $scriptId for task $taskId")
            } else {
                Log.e("SchedulerReceiver", "No script ID in intent")
                CrashHandler.logError("SchedulerReceiver", "Scheduled task $taskId missing script ID", null)
                return
            }

            // Handle task rescheduling if it's a repeating task
            if (taskId != null) {
                val scheduler = ScriptScheduler(context)
                val task = scheduler.getTask(taskId)
                if (task != null) {
                    when {
                        task.repeatDays.isEmpty() -> {
                            // Single-shot task - disable after execution
                            Log.i("SchedulerReceiver", "Disabling one-time task $taskId")
                            scheduler.enableTask(taskId, false)
                        }
                        else -> {
                            // Repeating task - reschedule for next occurrence
                            Log.i("SchedulerReceiver", "Rescheduling repeating task $taskId")
                            try {
                                scheduler.scheduleTask(task)
                            } catch (e: Exception) {
                                Log.e("SchedulerReceiver", "Failed to reschedule task $taskId", e)
                                CrashHandler.logError("SchedulerReceiver", "Failed to reschedule task '${task.scriptName}'", e)
                            }
                        }
                    }
                } else {
                    Log.w("SchedulerReceiver", "Task $taskId not found, cannot reschedule")
                }
            }
        }
    }
    
    class BootReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                // Перепланируем все задачи после перезагрузки
                ScriptScheduler(context).rescheduleAllTasks()
            }
        }
    }
}
