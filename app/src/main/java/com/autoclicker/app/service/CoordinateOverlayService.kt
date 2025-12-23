package com.autoclicker.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.autoclicker.app.R

/**
 * Сервис для отображения координат при касании экрана
 */
class CoordinateOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "coordinate_overlay_channel"
        private const val NOTIFICATION_ID = 1004

        fun startService(context: Context) {
            val intent = Intent(context, CoordinateOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, CoordinateOverlayService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var coordinateView: TextView? = null
    private var coordinateParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        setupOverlay()
    }

    private fun setupOverlay() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Прозрачный оверлей на весь экран для отслеживания касаний
        overlayView = View(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        // TextView для отображения координат
        coordinateView = TextView(this).apply {
            setBackgroundColor(Color.parseColor("#CC000000"))
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(16, 8, 16, 8)
            text = "X: 0  Y: 0"
            visibility = View.GONE
        }

        coordinateParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(overlayView, overlayParams)
        windowManager.addView(coordinateView, coordinateParams)

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    showCoordinates(event.rawX.toInt(), event.rawY.toInt())
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    hideCoordinates()
                }
            }
            false // Пропускаем событие дальше
        }
    }

    private fun showCoordinates(x: Int, y: Int) {
        coordinateView?.apply {
            text = "X: $x  Y: $y"
            visibility = View.VISIBLE
        }

        coordinateParams?.let { params ->
            params.x = x + 20
            params.y = y - 60
            coordinateView?.let { windowManager.updateViewLayout(it, params) }
        }
    }

    private fun hideCoordinates() {
        coordinateView?.visibility = View.GONE
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Coordinate Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Координаты")
            .setContentText("Касайтесь экрана для просмотра координат")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            overlayView?.setOnTouchListener(null)
            overlayView?.let { windowManager.removeView(it) }
            coordinateView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            // Views already removed
        }
        overlayView = null
        coordinateView = null
        coordinateParams = null
    }
}
