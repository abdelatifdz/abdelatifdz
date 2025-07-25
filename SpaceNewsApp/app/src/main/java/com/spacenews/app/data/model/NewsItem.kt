package com.spacenews.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
@Entity(tableName = "news_items")
data class NewsItem(
    @PrimaryKey
    val guid: String,
    val title: String,
    val description: String,
    val link: String,
    val pubDate: Date,
    val imageUrl: String?,
    val category: String?,
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
    val isNotified: Boolean = false
) : Parcelable