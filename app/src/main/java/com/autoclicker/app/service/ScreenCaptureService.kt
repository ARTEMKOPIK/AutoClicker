package com.autoclicker.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.autoclicker.app.R

class ScreenCaptureService : Service() {

    companion object {
        var instance: ScreenCaptureService? = null
            private set
        
        val isRunning: Boolean get() = instance != null

        private const val CHANNEL_ID = "screen_capture_channel"
        private const val NOTIFICATION_ID = 1001

        fun startService(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, ScreenCaptureService::class.java))
        }
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Устанавливаем обработчик ошибок для этого сервиса
        Thread.currentThread().setUncaughtExceptionHandler { thread, throwable ->
            com.autoclicker.app.util.CrashHandler.logCritical(
                "ScreenCaptureService",
                "Критическая ошибка в сервисе захвата экрана: ${throwable.message}",
                throwable
            )
            com.autoclicker.app.util.CrashHandler.getInstance()?.uncaughtException(thread, throwable)
        }
        
        createNotificationChannel()
        
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.currentWindowMetrics.bounds
                screenWidth = bounds.width()
                screenHeight = bounds.height()
                screenDensity = resources.displayMetrics.densityDpi
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(metrics)
                screenWidth = metrics.widthPixels
                screenHeight = metrics.heightPixels
                screenDensity = metrics.densityDpi
            }
            
            // Валидация размеров
            if (screenWidth <= 0 || screenHeight <= 0 || screenDensity <= 0) {
                android.util.Log.e("ScreenCapture", "Invalid screen dimensions: $screenWidth x $screenHeight, density: $screenDensity")
                com.autoclicker.app.util.CrashHandler.logError(
                    "ScreenCaptureService",
                    "Неверные размеры экрана: ${screenWidth}x${screenHeight}, density: $screenDensity"
                )
                // Бросаем исключение вместо продолжения работы с невалидными данными
                // Это предотвращает крэши и странное поведение при попытке создать VirtualDisplay
                throw IllegalStateException("Invalid screen dimensions: ${screenWidth}x${screenHeight}, density: $screenDensity")
            }
        } catch (e: Exception) {
            android.util.Log.e("ScreenCapture", "Error getting screen dimensions", e)
            com.autoclicker.app.util.CrashHandler.logError("ScreenCaptureService", "Ошибка получения размеров экрана", e)
            // Fallback значения
            screenWidth = 1080
            screenHeight = 1920
            screenDensity = 420
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        intent?.let {
            val resultCode = it.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelableExtra("data", Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelableExtra("data")
            }

            if (resultCode == Activity.RESULT_OK && data != null) {
                setupMediaProjection(resultCode, data)
            }
        }

        return START_STICKY
    }

    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            android.util.Log.e("ScreenCapture", "Invalid screen dimensions, cannot setup projection")
            stopSelf()
            return
        }
        
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        // Android 14+ требует регистрации callback перед созданием VirtualDisplay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    virtualDisplay?.release()
                    imageReader?.close()
                }
            }, null)
        }

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            screenWidth, screenHeight, screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
    }

    fun takeScreenshot(): Bitmap? {
        if (screenWidth <= 0 || screenHeight <= 0) {
            android.util.Log.e("ScreenCapture", "Invalid screen dimensions: $screenWidth x $screenHeight")
            com.autoclicker.app.util.CrashHandler.logWarning("ScreenCaptureService", "Неверные размеры экрана: $screenWidth x $screenHeight")
            return null
        }
        
        val image = try {
            imageReader?.acquireLatestImage()
        } catch (e: Exception) {
            android.util.Log.e("ScreenCapture", "Error acquiring image", e)
            com.autoclicker.app.util.CrashHandler.logError("ScreenCaptureService", "Ошибка получения изображения", e)
            return null
        } ?: return null
        
        var tempBitmap: Bitmap? = null
        var resultBitmap: Bitmap? = null
        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth

            val bitmapWidth = screenWidth + rowPadding / pixelStride
            tempBitmap = Bitmap.createBitmap(
                bitmapWidth,
                screenHeight,
                Bitmap.Config.ARGB_8888
            )
            tempBitmap.copyPixelsFromBuffer(buffer)
            
            // Если размеры совпадают, возвращаем tempBitmap напрямую
            if (bitmapWidth == screenWidth) {
                resultBitmap = tempBitmap
                tempBitmap = null // Предотвращаем recycle в finally
                resultBitmap
            } else {
                resultBitmap = Bitmap.createBitmap(tempBitmap, 0, 0, screenWidth, screenHeight)
                resultBitmap
            }
        } catch (e: Exception) {
            android.util.Log.e("ScreenCapture", "Error creating bitmap", e)
            resultBitmap?.recycle()
            resultBitmap = null
            null
        } finally {
            image.close()
            tempBitmap?.recycle() // Освобождаем только если не вернули его
        }
    }

    fun getPixelColor(x: Int, y: Int): Int {
        if (x < 0 || y < 0) {
            android.util.Log.w("ScreenCapture", "Negative coordinates: $x, $y")
            return 0
        }
        
        val screenshot = takeScreenshot() ?: return 0
        return try {
            if (x in 0 until screenshot.width && y in 0 until screenshot.height) {
                screenshot.getPixel(x, y)
            } else {
                android.util.Log.w("ScreenCapture", "Coordinates out of bounds: $x, $y (screen: ${screenshot.width}x${screenshot.height})")
                0
            }
        } catch (e: Exception) {
            android.util.Log.e("ScreenCapture", "Error getting pixel color", e)
            0
        } finally {
            screenshot.recycle()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoClicker")
            .setContentText("Захват экрана активен")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            virtualDisplay?.release()
            virtualDisplay = null
        } catch (e: Exception) {
            android.util.Log.e("ScreenCapture", "Error releasing virtualDisplay", e)
        }
        try {
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            android.util.Log.e("ScreenCapture", "Error closing imageReader", e)
        }
        try {
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            android.util.Log.e("ScreenCapture", "Error stopping mediaProjection", e)
        }
        instance = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // При нехватке памяти освобождаем ресурсы
        android.util.Log.w("ScreenCapture", "Low memory warning")
    }
}
