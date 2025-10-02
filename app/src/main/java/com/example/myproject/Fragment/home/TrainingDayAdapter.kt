package com.example.myproject.Fragment.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R
import com.example.myproject.data.training.TrainingModel

class TrainingDayAdapter(
    private val days: List<TrainingModel>
) : RecyclerView.Adapter<TrainingDayAdapter.TrainingDayViewHolder>() {

    class TrainingDayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayText: TextView = itemView.findViewById(R.id.dayText)
        val descText: TextView = itemView.findViewById(R.id.descriptionText)
        val paceText: TextView = itemView.findViewById(R.id.paceText)
        val typeText: TextView = itemView.findViewById(R.id.typeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_train_day, parent, false)
        return TrainingDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainingDayViewHolder, position: Int) {
        val day = days[position]

        // แสดงข้อมูลพร้อม fallback ถ้าเป็น null หรือว่าง
        holder.dayText.text = day.day ?: "Day ${position + 1}"
        holder.descText.text = day.description ?: "ไม่มีรายละเอียด"

        // จัดการ pace - ซ่อนถ้าไม่มีข้อมูล
        if (!day.pace.isNullOrEmpty()) {
            holder.paceText.text = day.pace
            holder.paceText.visibility = View.VISIBLE
        } else {
            holder.paceText.visibility = View.GONE
        }

        // จัดการ type - ซ่อนถ้าไม่มีข้อมูล
        if (!day.type.isNullOrEmpty()) {
            holder.typeText.text = day.type
            holder.typeText.visibility = View.VISIBLE
        } else {
            holder.typeText.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = days.size
}