package com.spacenews.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.spacenews.app.R
import com.spacenews.app.data.model.NewsItem
import com.spacenews.app.databinding.ActivityMainBinding
import com.spacenews.app.notification.NewsWorker
import com.spacenews.app.ui.adapter.NewsAdapter
import com.spacenews.app.ui.viewmodel.NewsViewModel

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val viewModel: NewsViewModel by viewModels()
    private lateinit var newsAdapter: NewsAdapter
    
    private var currentFilter = FilterType.ALL
    
    enum class FilterType {
        ALL, UNREAD, FAVORITES
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupRecyclerView()
        setupObservers()
        setupRefresh()
        setupTabs()
        
        // بدء خدمة التنبيهات
        NewsWorker.schedulePeriodicWork(this)
        
        // تحديث أولي للأخبار
        viewModel.refreshNews()
        
        // التحقق من التنبيه المحدد
        handleNotificationIntent()
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "🚀 أخبار الفضاء"
        
        // تطبيق الألوان والتدرجات
        binding.appBarLayout.background = ContextCompat.getDrawable(this, R.drawable.gradient_background)
    }
    
    private fun setupRecyclerView() {
        newsAdapter = NewsAdapter(
            onItemClick = { newsItem ->
                openNewsDetail(newsItem)
            },
            onFavoriteClick = { newsItem ->
                viewModel.toggleFavorite(newsItem.guid, newsItem.isFavorite)
            }
        )
        
        binding.recyclerView.apply {
            adapter = newsAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            setHasFixedSize(true)
        }
    }
    
    private fun setupObservers() {
        // مراقبة الأخبار حسب الفلتر المختار
        updateNewsObserver()
        
        // مراقبة حالة التحديث
        viewModel.isRefreshing.observe(this) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }
        
        // مراقبة نتيجة التحديث
        viewModel.refreshResult.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
        
        // مراقبة عدد الأخبار غير المقروءة
        viewModel.unreadCount.observe(this) { count ->
            updateUnreadBadge(count)
        }
    }
    
    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshNews()
        }
        
        // ألوان التحديث
        binding.swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.primary_color),
            ContextCompat.getColor(this, R.color.accent_color)
        )
    }
    
    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("الكل"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("غير مقروء"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("المفضلة"))
        
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> currentFilter = FilterType.ALL
                    1 -> currentFilter = FilterType.UNREAD
                    2 -> currentFilter = FilterType.FAVORITES
                }
                updateNewsObserver()
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun updateNewsObserver() {
        // إزالة المراقبين القدامى
        viewModel.allNews.removeObservers(this)
        viewModel.unreadNews.removeObservers(this)
        viewModel.favoriteNews.removeObservers(this)
        
        // إضافة المراقب الجديد حسب الفلتر
        when (currentFilter) {
            FilterType.ALL -> {
                viewModel.allNews.observe(this) { newsList ->
                    newsAdapter.submitList(newsList)
                    updateEmptyState(newsList.isEmpty())
                }
            }
            FilterType.UNREAD -> {
                viewModel.unreadNews.observe(this) { newsList ->
                    newsAdapter.submitList(newsList)
                    updateEmptyState(newsList.isEmpty())
                }
            }
            FilterType.FAVORITES -> {
                viewModel.favoriteNews.observe(this) { newsList ->
                    newsAdapter.submitList(newsList)
                    updateEmptyState(newsList.isEmpty())
                }
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyState.visibility = android.view.View.VISIBLE
            binding.recyclerView.visibility = android.view.View.GONE
            
            val message = when (currentFilter) {
                FilterType.ALL -> "لا توجد أخبار متاحة\nاسحب للأسفل لتحديث الأخبار"
                FilterType.UNREAD -> "لا توجد أخبار غير مقروءة"
                FilterType.FAVORITES -> "لا توجد أخبار مفضلة\nانقر على ❤️ لإضافة أخبار للمفضلة"
            }
            binding.emptyMessage.text = message
        } else {
            binding.emptyState.visibility = android.view.View.GONE
            binding.recyclerView.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun updateUnreadBadge(count: Int) {
        val unreadTab = binding.tabLayout.getTabAt(1)
        if (count > 0) {
            unreadTab?.text = "غير مقروء ($count)"
        } else {
            unreadTab?.text = "غير مقروء"
        }
    }
    
    private fun openNewsDetail(newsItem: NewsItem) {
        val intent = Intent(this, NewsDetailActivity::class.java).apply {
            putExtra("news_item", newsItem)
        }
        startActivity(intent)
        
        // وضع علامة كمقروء
        if (!newsItem.isRead) {
            viewModel.markAsRead(newsItem.guid)
        }
    }
    
    private fun handleNotificationIntent() {
        intent.getStringExtra("news_id")?.let { newsId ->
            viewModel.selectNewsItem(newsId)
            viewModel.selectedNewsItem.observe(this, Observer { newsItem ->
                newsItem?.let {
                    openNewsDetail(it)
                    viewModel.clearSelectedNewsItem()
                }
            })
        }
    }
}