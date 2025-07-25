package com.spacenews.app.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.spacenews.app.data.model.NewsItem

@Dao
interface NewsDao {
    
    @Query("SELECT * FROM news_items ORDER BY pubDate DESC")
    fun getAllNews(): LiveData<List<NewsItem>>
    
    @Query("SELECT * FROM news_items WHERE isFavorite = 1 ORDER BY pubDate DESC")
    fun getFavoriteNews(): LiveData<List<NewsItem>>
    
    @Query("SELECT * FROM news_items WHERE isRead = 0 ORDER BY pubDate DESC")
    fun getUnreadNews(): LiveData<List<NewsItem>>
    
    @Query("SELECT * FROM news_items WHERE guid = :guid")
    suspend fun getNewsById(guid: String): NewsItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(newsItems: List<NewsItem>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNewsItem(newsItem: NewsItem)
    
    @Update
    suspend fun updateNews(newsItem: NewsItem)
    
    @Delete
    suspend fun deleteNews(newsItem: NewsItem)
    
    @Query("DELETE FROM news_items")
    suspend fun deleteAllNews()
    
    @Query("UPDATE news_items SET isRead = 1 WHERE guid = :guid")
    suspend fun markAsRead(guid: String)
    
    @Query("UPDATE news_items SET isFavorite = :isFavorite WHERE guid = :guid")
    suspend fun updateFavoriteStatus(guid: String, isFavorite: Boolean)
    
    @Query("SELECT COUNT(*) FROM news_items WHERE isRead = 0")
    fun getUnreadCount(): LiveData<Int>
    
    @Query("SELECT * FROM news_items WHERE isNotified = 0 ORDER BY pubDate DESC LIMIT 10")
    suspend fun getUnnotifiedNews(): List<NewsItem>
    
    @Query("UPDATE news_items SET isNotified = 1 WHERE guid = :guid")
    suspend fun markAsNotified(guid: String)
}