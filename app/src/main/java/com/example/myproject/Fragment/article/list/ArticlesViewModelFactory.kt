package com.example.myproject.Fragment.article.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myproject.data.article.ArticleRepository

class ArticlesViewModelFactory(
    private val repository: ArticleRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticlesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArticlesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}