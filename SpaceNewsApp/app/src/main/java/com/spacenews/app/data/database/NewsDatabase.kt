package com.spacenews.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.spacenews.app.data.model.NewsItem

@Database(
    entities = [NewsItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class NewsDatabase : RoomDatabase() {
    
    abstract fun newsDao(): NewsDao
    
    companion object {
        @Volatile
        private var INSTANCE: NewsDatabase? = null
        
        fun getDatabase(context: Context): NewsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NewsDatabase::class.java,
                    "news_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}