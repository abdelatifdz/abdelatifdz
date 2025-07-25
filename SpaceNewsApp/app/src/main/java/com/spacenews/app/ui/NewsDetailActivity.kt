package com.spacenews.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.spacenews.app.R
import com.spacenews.app.data.model.NewsItem
import com.spacenews.app.databinding.ActivityNewsDetailBinding
import com.spacenews.app.ui.viewmodel.NewsViewModel
import java.text.SimpleDateFormat
import java.util.*

class NewsDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityNewsDetailBinding
    private val viewModel: NewsViewModel by viewModels()
    private lateinit var newsItem: NewsItem
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // الحصول على عنصر الأخبار من Intent
        newsItem = intent.getParcelableExtra("news_item") ?: run {
            finish()
            return
        }
        
        setupUI()
        displayNewsContent()
        setupClickListeners()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = ""
        
        // تطبيق التدرجات
        binding.appBarLayout.background = ContextCompat.getDrawable(this, R.drawable.gradient_background)
        
        // إضافة تأثير الشفافية للشريط العلوي
        binding.collapsingToolbar.setContentScrimColor(
            ContextCompat.getColor(this, R.color.primary_color)
        )
        binding.collapsingToolbar.setStatusBarScrimColor(
            ContextCompat.getColor(this, R.color.primary_dark)
        )
    }
    
    private fun displayNewsContent() {
        binding.apply {
            // العنوان
            textTitle.text = newsItem.title
            collapsingToolbar.title = newsItem.title
            
            // التاريخ والمصدر
            textDate.text = formatDate(newsItem.pubDate)
            textCategory.text = newsItem.category ?: "أخبار الفضاء"
            
            // المحتوى
            val formattedContent = Html.fromHtml(
                newsItem.description,
                Html.FROM_HTML_MODE_COMPACT
            )
            textContent.text = formattedContent
            
            // حالة المفضلة
            updateFavoriteButton()
            
            // تحميل الصورة الرئيسية
            newsItem.imageUrl?.let { imageUrl ->
                imageHeader.visibility = android.view.View.VISIBLE
                Glide.with(this@NewsDetailActivity)
                    .load(imageUrl)
                    .transform(RoundedCorners(0)) // بدون زوايا مدورة للصورة الرئيسية
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(imageHeader)
            } ?: run {
                imageHeader.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun setupClickListeners() {
        // زر المفضلة
        binding.fabFavorite.setOnClickListener {
            viewModel.toggleFavorite(newsItem.guid, newsItem.isFavorite)
            newsItem = newsItem.copy(isFavorite = !newsItem.isFavorite)
            updateFavoriteButton()
        }
        
        // زر المشاركة
        binding.fabShare.setOnClickListener {
            shareNews()
        }
        
        // زر فتح في المتصفح
        binding.fabOpenInBrowser.setOnClickListener {
            openInBrowser()
        }
        
        // زر الرجوع
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun updateFavoriteButton() {
        val favoriteIcon = if (newsItem.isFavorite) {
            R.drawable.ic_favorite_filled
        } else {
            R.drawable.ic_favorite_outline
        }
        
        binding.fabFavorite.setImageResource(favoriteIcon)
        
        val favoriteColor = if (newsItem.isFavorite) {
            ContextCompat.getColor(this, R.color.favorite_color)
        } else {
            ContextCompat.getColor(this, R.color.text_secondary)
        }
        
        binding.fabFavorite.imageTintList = 
            ContextCompat.getColorStateList(this, android.R.color.white)
    }
    
    private fun shareNews() {
        val shareText = """
            🚀 ${newsItem.title}
            
            ${newsItem.description.take(200)}${if (newsItem.description.length > 200) "..." else ""}
            
            اقرأ المزيد: ${newsItem.link}
            
            مشاركة من تطبيق أخبار الفضاء
        """.trimIndent()
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        startActivity(Intent.createChooser(shareIntent, "مشاركة الخبر"))
    }
    
    private fun openInBrowser() {
        if (newsItem.link.isNotEmpty()) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(newsItem.link))
            if (browserIntent.resolveActivity(packageManager) != null) {
                startActivity(browserIntent)
            }
        }
    }
    
    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("EEEE، dd MMMM yyyy 'الساعة' HH:mm", Locale("ar"))
        return formatter.format(date)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}