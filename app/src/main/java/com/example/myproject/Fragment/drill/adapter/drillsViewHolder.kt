package com.example.myproject.Fragment.drill.adapter

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myproject.R
import com.example.myproject.data.article.drill.drillModel

class drillsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
    val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
    val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)

    fun bind(drill: drillModel) {
        tvTitle.text = drill.title
        tvSubtitle.text = drill.subtitle
        tvDescription.text = drill.description
        /*Glide.with(itemView.context)
            .load(drill.imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .into(imageView)*/
    }
}