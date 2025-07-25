package com.spacenews.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NewsNotificationReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // إعادة تشغيل مراقبة الأخبار بعد إعادة التشغيل
                NewsWorker.schedulePeriodicWork(context)
            }
        }
    }
}