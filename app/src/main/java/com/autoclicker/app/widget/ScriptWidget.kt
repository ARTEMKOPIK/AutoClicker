package com.autoclicker.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.autoclicker.app.R
import com.autoclicker.app.service.FloatingWindowService
import com.autoclicker.app.util.PrefsManager
import com.autoclicker.app.util.ScriptStorage

/**
 * Виджет для быстрого запуска скриптов с рабочего стола
 */
class ScriptWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_START_SCRIPT = "com.autoclicker.app.ACTION_START_SCRIPT"
        const val ACTION_STOP_SCRIPT = "com.autoclicker.app.ACTION_STOP_SCRIPT"
        const val EXTRA_SCRIPT_ID = "script_id"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, ScriptWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, ScriptWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
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

        when (intent.action) {
            ACTION_START_SCRIPT -> {
                val scriptId = intent.getStringExtra(EXTRA_SCRIPT_ID)
                if (scriptId != null) {
                    FloatingWindowService.startService(context, scriptId)
                } else {
                    FloatingWindowService.startService(context)
                }
                updateAllWidgets(context)
            }
            ACTION_STOP_SCRIPT -> {
                FloatingWindowService.stopService(context)
                updateAllWidgets(context)
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = PrefsManager(context)
        val storage = ScriptStorage(context)
        
        val lastScriptId = prefs.lastScriptId
        val script = if (lastScriptId.isNotEmpty()) {
            storage.getScript(lastScriptId)
        } else {
            storage.getAllScripts().firstOrNull()
        }

        val views = RemoteViews(context.packageName, R.layout.widget_script)

        // Название скрипта
        views.setTextViewText(
            R.id.tvWidgetScriptName,
            script?.name ?: "Нет скрипта"
        )

        // Кнопка запуска
        val startIntent = Intent(context, ScriptWidget::class.java).apply {
            action = ACTION_START_SCRIPT
            script?.let { putExtra(EXTRA_SCRIPT_ID, it.id) }
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btnWidgetStart, startPendingIntent)

        // Кнопка остановки
        val stopIntent = Intent(context, ScriptWidget::class.java).apply {
            action = ACTION_STOP_SCRIPT
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId + 1000,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btnWidgetStop, stopPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
