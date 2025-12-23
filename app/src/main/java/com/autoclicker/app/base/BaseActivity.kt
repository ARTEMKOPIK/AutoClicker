package com.autoclicker.app.base

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import com.autoclicker.app.R

/**
 * Базовый класс Activity с анимациями переходов
 */
abstract class BaseActivity : AppCompatActivity() {

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        applyEnterTransition()
    }

    override fun finish() {
        super.finish()
        applyExitTransition()
    }

    private fun applyEnterTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun applyExitTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }
}
