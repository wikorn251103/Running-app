package com.example.myproject.Fragment.article.list.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R
import com.example.myproject.data.article.ArticleModel

class ArticleAdapter(private val listener: ArticleListener) : ListAdapter<ArticleModel, ArticleViewHolder>(ArticleDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_article, parent, false)

        val holder = ArticleViewHolder(view)
        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val article = getItem(position)
                listener.onArticleClicked(article)
            }
        }

        return holder
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = getItem(position)
        holder.bind(article)
    }

}

interface ArticleListener {
    fun onArticleClicked(article: ArticleModel)

}