package com.example.myproject.Fragment.article.list.adapter

import androidx.recyclerview.widget.DiffUtil
import com.example.myproject.data.article.ArticleModel

// สร้าง DiffUtil สำหรับ ArticleModel
class ArticleDiffCallback : DiffUtil.ItemCallback<ArticleModel>() {
    override fun areItemsTheSame(oldItem: ArticleModel, newItem: ArticleModel): Boolean {
        // เปรียบเทียบว่า item เหมือนกันหรือไม่ โดยปกติจะใช้ id หรือ key ที่ไม่ซ้ำกัน
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: ArticleModel, newItem: ArticleModel): Boolean {
        // เปรียบเทียบข้อมูลในแต่ละ field
        return oldItem.title == newItem.title && oldItem.description == newItem.description && oldItem.imageUrl == newItem.imageUrl
    }
}