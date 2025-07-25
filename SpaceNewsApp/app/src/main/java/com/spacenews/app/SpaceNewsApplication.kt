package com.spacenews.app

import android.app.Application
import com.spacenews.app.notification.NewsWorker

class SpaceNewsApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // تشغيل خدمة مراقبة الأخبار في الخلفية
        NewsWorker.schedulePeriodicWork(this)
    }
}