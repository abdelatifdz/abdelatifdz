package com.spacenews.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.spacenews.app.data.model.NewsItem
import com.spacenews.app.data.repository.NewsRepository
import kotlinx.coroutines.launch

class NewsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = NewsRepository(application)
    
    val allNews: LiveData<List<NewsItem>> = repository.getAllNews()
    val favoriteNews: LiveData<List<NewsItem>> = repository.getFavoriteNews()
    val unreadNews: LiveData<List<NewsItem>> = repository.getUnreadNews()
    val unreadCount: LiveData<Int> = repository.getUnreadCount()
    
    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing
    
    private val _refreshResult = MutableLiveData<String>()
    val refreshResult: LiveData<String> = _refreshResult
    
    private val _selectedNewsItem = MutableLiveData<NewsItem?>()
    val selectedNewsItem: LiveData<NewsItem?> = _selectedNewsItem
    
    fun refreshNews() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val result = repository.refreshNews()
                if (result.isSuccess) {
                    _refreshResult.value = result.getOrNull() ?: "تم التحديث بنجاح"
                } else {
                    _refreshResult.value = "خطأ في التحديث: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _refreshResult.value = "خطأ في التحديث: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    fun markAsRead(guid: String) {
        viewModelScope.launch {
            repository.markAsRead(guid)
        }
    }
    
    fun toggleFavorite(guid: String, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.updateFavoriteStatus(guid, !isFavorite)
        }
    }
    
    fun selectNewsItem(guid: String) {
        viewModelScope.launch {
            val newsItem = repository.getNewsById(guid)
            _selectedNewsItem.value = newsItem
            // وضع علامة كمقروء عند الاختيار
            if (newsItem != null && !newsItem.isRead) {
                repository.markAsRead(guid)
            }
        }
    }
    
    fun clearSelectedNewsItem() {
        _selectedNewsItem.value = null
    }
}