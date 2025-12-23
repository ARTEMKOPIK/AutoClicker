package com.autoclicker.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class ClickerAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ClickerAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    fun click(x: Float, y: Float, duration: Long = 50): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, duration: Long = 300): Boolean {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    fun longClick(x: Float, y: Float, duration: Long = 500): Boolean {
        return click(x, y, duration)
    }

    /**
     * Жест масштабирования (pinch)
     * scale > 1 = zoom in, scale < 1 = zoom out
     */
    fun pinch(centerX: Float, centerY: Float, scale: Float, duration: Long = 300): Boolean {
        val startDistance = 100f
        val endDistance = startDistance * scale

        val path1 = Path().apply {
            moveTo(centerX - startDistance, centerY)
            lineTo(centerX - endDistance, centerY)
        }

        val path2 = Path().apply {
            moveTo(centerX + startDistance, centerY)
            lineTo(centerX + endDistance, centerY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path1, 0, duration))
            .addStroke(GestureDescription.StrokeDescription(path2, 0, duration))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    /**
     * Двойной тап
     * Использует Handler для задержки вместо Thread.sleep() чтобы не блокировать поток
     */
    fun doubleTap(x: Float, y: Float): Boolean {
        click(x, y)
        // Второй клик с минимальной задержкой через GestureDescription
        val path = android.graphics.Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 60, 50)) // startTime=60ms для задержки
            .build()
        return dispatchGesture(gesture, null, null)
    }
}
