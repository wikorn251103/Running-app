package com.example.myproject.Fragment.article.list.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myproject.R
import com.example.myproject.data.article.ArticleModel

class ArticleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imageView: ImageView = itemView.findViewById(R.id.imageView)
    val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
    val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)

    fun bind(article: ArticleModel) {
        tvTitle.text = article.title
        tvDescription.text = article.description
        Glide.with(itemView.context)
            .load(article.imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .into(imageView)
    }
}
