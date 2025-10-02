package com.example.myproject.Fragment.drill.adapter

import androidx.recyclerview.widget.DiffUtil
import com.example.myproject.data.drill.drillModel

class drillsDiffCallback : DiffUtil.ItemCallback<drillModel>() {
    override fun areItemsTheSame(oldItem: drillModel, newItem: drillModel): Boolean {
        //เปรียบเทียบ item เหมือนกันหรือไม่โดยปกติ จะใช้ id ไม่ก๋ key ไม่ซ่้ำ
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: drillModel, newItem: drillModel): Boolean {
        // เปรียบเทียบข้อมูลในแต่ละfield
        return oldItem.title == newItem.title && oldItem.subtitle == newItem.subtitle && oldItem.description == newItem.description
    }
}