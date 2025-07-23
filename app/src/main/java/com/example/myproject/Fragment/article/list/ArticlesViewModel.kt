package com.example.myproject.Fragment.article.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.data.article.ArticleModel
import com.example.myproject.data.article.ArticleRepository
import kotlinx.coroutines.launch

class ArticlesViewModel(private val articleRepository: ArticleRepository) : ViewModel() {

    private val _articles = MutableLiveData<List<ArticleModel>>()
    val articles: LiveData<List<ArticleModel>> = _articles

    fun getArticles() {
        viewModelScope.launch {
            articleRepository.getArticlesFlow().collect { articles ->
                _articles.value = articles
            }
        }
    }
}