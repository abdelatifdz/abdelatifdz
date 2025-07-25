package com.spacenews.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.spacenews.app.R
import com.spacenews.app.data.model.NewsItem
import com.spacenews.app.databinding.ItemNewsBinding
import java.text.SimpleDateFormat
import java.util.*

class NewsAdapter(
    private val onItemClick: (NewsItem) -> Unit,
    private val onFavoriteClick: (NewsItem) -> Unit
) : ListAdapter<NewsItem, NewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NewsViewHolder(
        private val binding: ItemNewsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(newsItem: NewsItem) {
            binding.apply {
                // العنوان
                textTitle.text = newsItem.title
                
                // الوصف
                textDescription.text = newsItem.description.take(150) + 
                    if (newsItem.description.length > 150) "..." else ""
                
                // التاريخ
                textDate.text = formatDate(newsItem.pubDate)
                
                // المصدر/الفئة
                textCategory.text = newsItem.category ?: "أخبار الفضاء"
                
                // حالة القراءة
                if (newsItem.isRead) {
                    cardView.alpha = 0.7f
                    textTitle.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.text_secondary)
                    )
                } else {
                    cardView.alpha = 1.0f
                    textTitle.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.text_primary)
                    )
                    // إضافة مؤشر جديد
                    newIndicator.visibility = android.view.View.VISIBLE
                }
                
                // أيقونة المفضلة
                imageFavorite.setImageResource(
                    if (newsItem.isFavorite) R.drawable.ic_favorite_filled
                    else R.drawable.ic_favorite_outline
                )
                
                // تحميل الصورة
                newsItem.imageUrl?.let { imageUrl ->
                    imageNews.visibility = android.view.View.VISIBLE
                    Glide.with(itemView.context)
                        .load(imageUrl)
                        .transform(RoundedCorners(16))
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .into(imageNews)
                } ?: run {
                    imageNews.visibility = android.view.View.GONE
                }
                
                // معالجات النقر
                cardView.setOnClickListener {
                    onItemClick(newsItem)
                }
                
                imageFavorite.setOnClickListener {
                    onFavoriteClick(newsItem)
                }
            }
        }
        
        private fun formatDate(date: Date): String {
            val now = Date()
            val diffInMillis = now.time - date.time
            
            return when {
                diffInMillis < 60_000 -> "الآن"
                diffInMillis < 3600_000 -> "${diffInMillis / 60_000} دقيقة"
                diffInMillis < 86400_000 -> "${diffInMillis / 3600_000} ساعة"
                diffInMillis < 604800_000 -> "${diffInMillis / 86400_000} يوم"
                else -> SimpleDateFormat("dd/MM/yyyy", Locale("ar")).format(date)
            }
        }
    }
}

class NewsDiffCallback : DiffUtil.ItemCallback<NewsItem>() {
    override fun areItemsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean {
        return oldItem.guid == newItem.guid
    }

    override fun areContentsTheSame(oldItem: NewsItem, newItem: NewsItem): Boolean {
        return oldItem == newItem
    }
}