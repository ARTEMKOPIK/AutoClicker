package com.autoclicker.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.autoclicker.app.R
import com.autoclicker.app.service.FloatingWindowService
import com.autoclicker.app.util.ScriptStorage

/**
 * Виджет для быстрого запуска скриптов с рабочего стола
 */
class ScriptWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_START_SCRIPT = "com.autoclicker.app.ACTION_START_SCRIPT"
        const val ACTION_STOP_SCRIPT = "com.autoclicker.app.ACTION_STOP_SCRIPT"
        const val ACTION_NEXT_SCRIPT = "com.autoclicker.app.ACTION_NEXT_SCRIPT"
        const val ACTION_PREV_SCRIPT = "com.autoclicker.app.ACTION_PREV_SCRIPT"
        const val EXTRA_SCRIPT_ID = "script_id"
        const val EXTRA_WIDGET_ID = "widget_id"
        
        private const val PREFS_NAME = "widget_prefs"
        private const val KEY_SCRIPT_INDEX = "script_index_"
        private const val KEY_IS_RUNNING = "is_running_"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, ScriptWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, ScriptWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
        
        fun setRunningState(context: Context, widgetId: Int, isRunning: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_RUNNING + widgetId, isRunning)
                .apply()
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // Проверка безопасности: разрешаем только интенты от нашего приложения
        val callingPackage = intent.getStringExtra("calling_package")
        if (callingPackage != null && callingPackage != context.packageName) {
            // Игнорируем интенты от сторонних приложений
            return
        }
        
        val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storage = ScriptStorage(context)
        val scripts = storage.getAllScripts()

        when (intent.action) {
            ACTION_START_SCRIPT -> {
                val scriptId = intent.getStringExtra(EXTRA_SCRIPT_ID)
                if (scriptId != null) {
                    FloatingWindowService.startService(context, scriptId)
                    if (widgetId != -1) {
                        setRunningState(context, widgetId, true)
                    }
                }
                updateAllWidgets(context)
            }
            ACTION_STOP_SCRIPT -> {
                FloatingWindowService.stopService(context)
                if (widgetId != -1) {
                    setRunningState(context, widgetId, false)
                }
                updateAllWidgets(context)
            }
            ACTION_NEXT_SCRIPT -> {
                if (widgetId != -1 && scripts.isNotEmpty()) {
                    val currentIndex = prefs.getInt(KEY_SCRIPT_INDEX + widgetId, 0)
                    val newIndex = (currentIndex + 1) % scripts.size
                    prefs.edit().putInt(KEY_SCRIPT_INDEX + widgetId, newIndex).apply()
                    updateAllWidgets(context)
                }
            }
            ACTION_PREV_SCRIPT -> {
                if (widgetId != -1 && scripts.isNotEmpty()) {
                    val currentIndex = prefs.getInt(KEY_SCRIPT_INDEX + widgetId, 0)
                    val newIndex = if (currentIndex > 0) currentIndex - 1 else scripts.size - 1
                    prefs.edit().putInt(KEY_SCRIPT_INDEX + widgetId, newIndex).apply()
                    updateAllWidgets(context)
                }
            }
        }
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (id in appWidgetIds) {
            editor.remove(KEY_SCRIPT_INDEX + id)
            editor.remove(KEY_IS_RUNNING + id)
        }
        editor.apply()
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storage = ScriptStorage(context)
        val scripts = storage.getAllScripts()
        
        val scriptIndex = prefs.getInt(KEY_SCRIPT_INDEX + appWidgetId, 0)
        val isRunning = prefs.getBoolean(KEY_IS_RUNNING + appWidgetId, false)
        
        val script = scripts.getOrNull(scriptIndex) ?: scripts.firstOrNull()

        val views = RemoteViews(context.packageName, R.layout.widget_script)

        // Название скрипта
        views.setTextViewText(
            R.id.tvWidgetScriptName,
            script?.name ?: "Нет скриптов"
        )
        
        // Счётчик скриптов
        if (scripts.isNotEmpty()) {
            views.setTextViewText(R.id.tvWidgetCounter, "${scriptIndex + 1}/${scripts.size}")
            views.setViewVisibility(R.id.tvWidgetCounter, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.btnWidgetPrev, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.btnWidgetNext, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.tvWidgetCounter, android.view.View.GONE)
            views.setViewVisibility(R.id.btnWidgetPrev, android.view.View.GONE)
            views.setViewVisibility(R.id.btnWidgetNext, android.view.View.GONE)
        }
        
        // Статус
        if (isRunning) {
            views.setTextViewText(R.id.tvWidgetStatus, "▶ Работает")
            views.setTextColor(R.id.tvWidgetStatus, Color.parseColor("#10B981"))
        } else {
            views.setTextViewText(R.id.tvWidgetStatus, "⏸ Остановлен")
            views.setTextColor(R.id.tvWidgetStatus, Color.parseColor("#6B7280"))
        }

        // Кнопка запуска
        val startIntent = Intent(context, ScriptWidget::class.java).apply {
            action = ACTION_START_SCRIPT
            script?.let { putExtra(EXTRA_SCRIPT_ID, it.id) }
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra("calling_package", context.packageName)
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btnWidgetStart, startPendingIntent)

        // Кнопка остановки
        val stopIntent = Intent(context, ScriptWidget::class.java).apply {
            action = ACTION_STOP_SCRIPT
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra("calling_package", context.packageName)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btnWidgetStop, stopPendingIntent)
        
        // Кнопка предыдущий скрипт
        val prevIntent = Intent(context, ScriptWidget::class.java).apply {
            action = ACTION_PREV_SCRIPT
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra("calling_package", context.packageName)
        }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + 2,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btnWidgetPrev, prevPendingIntent)
        
        // Кнопка следующий скрипт
        val nextIntent = Intent(context, ScriptWidget::class.java).apply {
            action = ACTION_NEXT_SCRIPT
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra("calling_package", context.packageName)
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + 3,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btnWidgetNext, nextPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
