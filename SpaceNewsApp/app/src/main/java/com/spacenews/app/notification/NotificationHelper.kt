package com.spacenews.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.spacenews.app.R
import com.spacenews.app.data.model.NewsItem
import com.spacenews.app.ui.MainActivity
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class NotificationHelper(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        const val CHANNEL_ID = "space_news_channel"
        const val CHANNEL_NAME = "أخبار الفضاء"
        const val CHANNEL_DESCRIPTION = "تنبيهات الأخبار الجديدة من الفضاء"
    }
    
    init {
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = ContextCompat.getColor(context, R.color.primary_color)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    fun showNewsNotification(newsItem: NewsItem) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("news_id", newsItem.guid)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            newsItem.guid.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_space_notification)
            .setContentTitle("🚀 خبر جديد من الفضاء")
            .setContentText(newsItem.title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(newsItem.description.take(200) + if (newsItem.description.length > 200) "..." else "")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(ContextCompat.getColor(context, R.color.primary_color))
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            
        // محاولة إضافة صورة للتنبيه
        newsItem.imageUrl?.let { imageUrl ->
            try {
                val bitmap = loadImageFromUrl(imageUrl)
                bitmap?.let {
                    notification.setLargeIcon(it)
                        .setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(it)
                                .setBigContentTitle("🚀 خبر جديد من الفضاء")
                                .setSummaryText(newsItem.title)
                        )
                }
            } catch (e: Exception) {
                // في حالة فشل تحميل الصورة، استخدم التنبيه العادي
            }
        }
        
        notificationManager.notify(newsItem.guid.hashCode(), notification.build())
    }
    
    fun showMultipleNewsNotification(newsItems: List<NewsItem>) {
        if (newsItems.isEmpty()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("🚀 ${newsItems.size} أخبار جديدة من الفضاء")
            .setSummaryText("انقر لقراءة الأخبار")
        
        newsItems.take(5).forEach { newsItem ->
            inboxStyle.addLine("• ${newsItem.title}")
        }
        
        if (newsItems.size > 5) {
            inboxStyle.addLine("و ${newsItems.size - 5} أخبار أخرى...")
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_space_notification)
            .setContentTitle("🚀 أخبار جديدة من الفضاء")
            .setContentText("${newsItems.size} أخبار جديدة متاحة")
            .setStyle(inboxStyle)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(ContextCompat.getColor(context, R.color.primary_color))
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setNumber(newsItems.size)
            .build()
        
        notificationManager.notify(999, notification)
    }
    
    private fun loadImageFromUrl(imageUrl: String): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val inputStream = connection.inputStream
            BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            null
        }
    }
}