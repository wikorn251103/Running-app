package com.example.myproject.Fragment.drill.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R
import com.example.myproject.data.drill.drillModel

class drillsAdapter(private val listener: DrillListener) : ListAdapter<drillModel, drillsViewHolder>(drillsDiffCallback()){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): drillsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_drill, parent, false)
        val holder = drillsViewHolder(view)
        holder.itemView.setOnClickListener {
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val drill = getItem(position)
                listener.onDrillClicked(drill)
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: drillsViewHolder, position: Int) {
        val drill = getItem(position)
        holder.bind(drill)
    }

}

interface DrillListener {
    fun onDrillClicked(drill: drillModel)

}