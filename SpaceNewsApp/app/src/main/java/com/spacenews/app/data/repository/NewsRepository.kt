package com.spacenews.app.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.prof18.rssparser.RssParser
import com.spacenews.app.data.database.NewsDao
import com.spacenews.app.data.database.NewsDatabase
import com.spacenews.app.data.model.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class NewsRepository(context: Context) {
    
    private val newsDao: NewsDao = NewsDatabase.getDatabase(context).newsDao()
    private val rssParser = RssParser()
    
    // RSS URLs - يمكن إضافة المزيد من المصادر
    private val rssUrls = listOf(
        "https://www.space.com/feeds/all",
        "https://www.sciencedaily.com/rss/space_time.xml"
    )
    
    fun getAllNews(): LiveData<List<NewsItem>> = newsDao.getAllNews()
    
    fun getFavoriteNews(): LiveData<List<NewsItem>> = newsDao.getFavoriteNews()
    
    fun getUnreadNews(): LiveData<List<NewsItem>> = newsDao.getUnreadNews()
    
    fun getUnreadCount(): LiveData<Int> = newsDao.getUnreadCount()
    
    suspend fun refreshNews(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val allNewsItems = mutableListOf<NewsItem>()
            
            for (url in rssUrls) {
                try {
                    val channel = rssParser.getRssChannel(url)
                    
                    val newsItems = channel.articles.map { article ->
                        NewsItem(
                            guid = article.guid ?: article.link ?: UUID.randomUUID().toString(),
                            title = article.title ?: "عنوان غير متوفر",
                            description = article.description ?: article.content ?: "",
                            link = article.link ?: "",
                            pubDate = parseDate(article.pubDate) ?: Date(),
                            imageUrl = article.image ?: extractImageFromContent(article.content),
                            category = article.categories?.firstOrNull() ?: channel.title
                        )
                    }
                    
                    allNewsItems.addAll(newsItems)
                } catch (e: Exception) {
                    // تسجيل الخطأ ومتابعة مع المصادر الأخرى
                    e.printStackTrace()
                }
            }
            
            if (allNewsItems.isNotEmpty()) {
                newsDao.insertNews(allNewsItems)
                Result.success("تم تحديث ${allNewsItems.size} خبر جديد")
            } else {
                Result.failure(Exception("لا توجد أخبار جديدة"))
            }
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markAsRead(guid: String) = withContext(Dispatchers.IO) {
        newsDao.markAsRead(guid)
    }
    
    suspend fun updateFavoriteStatus(guid: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        newsDao.updateFavoriteStatus(guid, isFavorite)
    }
    
    suspend fun getNewsById(guid: String): NewsItem? = withContext(Dispatchers.IO) {
        newsDao.getNewsById(guid)
    }
    
    suspend fun getUnnotifiedNews(): List<NewsItem> = withContext(Dispatchers.IO) {
        newsDao.getUnnotifiedNews()
    }
    
    suspend fun markAsNotified(guid: String) = withContext(Dispatchers.IO) {
        newsDao.markAsNotified(guid)
    }
    
    private fun parseDate(dateString: String?): Date? {
        if (dateString.isNullOrEmpty()) return null
        
        val formats = listOf(
            "EEE, dd MMM yyyy HH:mm:ss zzz",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd HH:mm:ss"
        )
        
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                return sdf.parse(dateString)
            } catch (e: Exception) {
                // جرب التنسيق التالي
            }
        }
        
        return null
    }
    
    private fun extractImageFromContent(content: String?): String? {
        if (content.isNullOrEmpty()) return null
        
        val imageRegex = Regex("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>")
        val matchResult = imageRegex.find(content)
        return matchResult?.groupValues?.get(1)
    }
}