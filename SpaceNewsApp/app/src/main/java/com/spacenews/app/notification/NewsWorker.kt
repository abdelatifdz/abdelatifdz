package com.spacenews.app.notification

import android.content.Context
import androidx.work.*
import com.spacenews.app.data.repository.NewsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class NewsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = NewsRepository(applicationContext)
    private val notificationHelper = NotificationHelper(applicationContext)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // تحديث الأخبار
            val refreshResult = repository.refreshNews()
            
            if (refreshResult.isSuccess) {
                // التحقق من الأخبار غير المبلغ عنها
                val unnotifiedNews = repository.getUnnotifiedNews()
                
                if (unnotifiedNews.isNotEmpty()) {
                    // إرسال تنبيه للأخبار الجديدة
                    if (unnotifiedNews.size == 1) {
                        notificationHelper.showNewsNotification(unnotifiedNews.first())
                    } else {
                        notificationHelper.showMultipleNewsNotification(unnotifiedNews)
                    }
                    
                    // وضع علامة كمبلغ عنها
                    unnotifiedNews.forEach { newsItem ->
                        repository.markAsNotified(newsItem.guid)
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "NewsUpdateWork"

        fun schedulePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<NewsWorker>(
                repeatInterval = 30, // كل 30 دقيقة
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    PeriodicWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
        }

        fun cancelPeriodicWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}