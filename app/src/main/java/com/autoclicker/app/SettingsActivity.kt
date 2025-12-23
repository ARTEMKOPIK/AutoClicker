package com.autoclicker.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.autoclicker.app.base.BaseActivity
import com.autoclicker.app.service.ScreenCaptureService

class SettingsActivity : BaseActivity() {

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureService.startService(this, result.resultCode, result.data!!)
            updatePermissionStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        setupPermissionItems()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupPermissionItems() {
        findViewById<LinearLayout>(R.id.itemAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<LinearLayout>(R.id.itemOverlay).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }

        findViewById<LinearLayout>(R.id.itemCapture).setOnClickListener {
            if (!ScreenCaptureService.isRunning) {
                val mediaProjectionManager = getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) 
                    as android.media.projection.MediaProjectionManager
                screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
            }
        }

        findViewById<LinearLayout>(R.id.itemVariables).setOnClickListener {
            startActivity(Intent(this, VariablesActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.itemLogs).setOnClickListener {
            startActivity(Intent(this, LogsActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.itemQuickActions).setOnClickListener {
            startActivity(Intent(this, QuickActionsActivity::class.java))
        }
    }

    private fun updatePermissionStatus() {
        val accessibilityEnabled = isAccessibilityEnabled()
        findViewById<TextView>(R.id.tvAccessibilityStatus).text = if (accessibilityEnabled) "Включено" else "Выключено"
        findViewById<TextView>(R.id.tvAccessibilityStatus).setTextColor(
            getColor(if (accessibilityEnabled) R.color.success else R.color.error)
        )

        val overlayEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)
        findViewById<TextView>(R.id.tvOverlayStatus).text = if (overlayEnabled) "Включено" else "Выключено"
        findViewById<TextView>(R.id.tvOverlayStatus).setTextColor(
            getColor(if (overlayEnabled) R.color.success else R.color.error)
        )

        val captureEnabled = ScreenCaptureService.isRunning
        findViewById<TextView>(R.id.tvCaptureStatus).text = if (captureEnabled) "Активен" else "Не активен"
        findViewById<TextView>(R.id.tvCaptureStatus).setTextColor(
            getColor(if (captureEnabled) R.color.success else R.color.error)
        )
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(android.content.Context.ACCESSIBILITY_SERVICE) 
            as android.view.accessibility.AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any { it.id.contains(packageName) }
    }
}
