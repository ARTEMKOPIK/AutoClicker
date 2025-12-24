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

/**
 * Мини-виджет 1x1 для быстрого запуска/остановки
 */
class QuickLaunchWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.autoclicker.app.ACTION_TOGGLE"
        private const val PREFS_NAME = "quick_widget_prefs"
        private const val KEY_IS_RUNNING = "is_running"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, QuickLaunchWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, QuickLaunchWidget::class.java))
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

        if (intent.action == ACTION_TOGGLE) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
            
            if (isRunning) {
                FloatingWindowService.stopService(context)
                prefs.edit().putBoolean(KEY_IS_RUNNING, false).apply()
            } else {
                FloatingWindowService.startService(context)
                prefs.edit().putBoolean(KEY_IS_RUNNING, true).apply()
            }
            
            updateAllWidgets(context)
            ScriptWidget.updateAllWidgets(context)
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isRunning = prefs.getBoolean(KEY_IS_RUNNING, false)

        val views = RemoteViews(context.packageName, R.layout.widget_quick_launch)

        // Меняем иконку в зависимости от состояния
        if (isRunning) {
            views.setImageViewResource(R.id.btnQuickLaunch, R.drawable.ic_stop)
            views.setInt(R.id.btnQuickLaunch, "setBackgroundResource", R.drawable.widget_button_stop_large)
        } else {
            views.setImageViewResource(R.id.btnQuickLaunch, R.drawable.ic_play)
            views.setInt(R.id.btnQuickLaunch, "setBackgroundResource", R.drawable.widget_button_play_large)
        }

        // Toggle intent
        val toggleIntent = Intent(context, QuickLaunchWidget::class.java).apply {
            action = ACTION_TOGGLE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btnQuickLaunch, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
