package com.example.myproject.data.article

import kotlinx.coroutines.flow.Flow

interface ArticleRepository {
    fun getArticlesFlow(): Flow<List<ArticleModel>>
}

class ArticleRepositoryImpl(
    private val articleService: ArticleService
) : ArticleRepository {
    override fun getArticlesFlow(): Flow<List<ArticleModel>> {
        return articleService.getArticlesFlow()
    }
}
