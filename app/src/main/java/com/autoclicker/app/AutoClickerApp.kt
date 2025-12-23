package com.autoclicker.app

import android.app.Application
import com.autoclicker.app.util.CrashHandler

class AutoClickerApp : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Инициализируем обработчик крашей
        CrashHandler.init(this)
    }
}
