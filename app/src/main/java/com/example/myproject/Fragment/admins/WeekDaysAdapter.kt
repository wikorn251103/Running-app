package com.example.myproject.Fragment.admins

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R
import com.example.myproject.data.startprogram.WeekDay
import com.example.myproject.databinding.ItemWeekDayBinding

class WeekDaysAdapter(
    private val weekDays: MutableList<WeekDay>,
    private val onEditClick: (WeekDay) -> Unit,
    private val onDeleteClick: (WeekDay) -> Unit
) : RecyclerView.Adapter<WeekDaysAdapter.WeekDayViewHolder>() {

    inner class WeekDayViewHolder(private val binding: ItemWeekDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(weekDay: WeekDay) {
            binding.apply {
                // วัน
                tvDayName.text = "วัน: ${weekDay.dayName}"

                // ประเภท
                tvType.text = "ประเภท: ${weekDay.type}"

                // ระยะทาง
                if (weekDay.distance > 0) {
                    tvDistance.visibility = View.VISIBLE
                    tvDistance.text = "ระยะทาง: ${weekDay.distance} กม."
                } else {
                    tvDistance.visibility = View.GONE
                }

                // ระยะเวลา
                if (weekDay.duration > 0) {
                    tvDuration.visibility = View.VISIBLE
                    tvDuration.text = "เวลา: ${weekDay.duration} นาที"
                } else {
                    tvDuration.visibility = View.GONE
                }

                // คำอธิบาย
                if (weekDay.description.isNotEmpty()) {
                    tvDescription.visibility = View.VISIBLE
                    tvDescription.text = weekDay.description
                } else {
                    tvDescription.visibility = View.GONE
                }

                // สัปดาห์และวัน
                tvWeekDay.text = "สัปดาห์ ${weekDay.week} วันที่ ${weekDay.day}"

                // สีตามประเภท
                val bgColor = when (weekDay.type) {
                    "Easy Run" -> ContextCompat.getColor(itemView.context, R.color.accent_blue)
                    "Interval" -> ContextCompat.getColor(itemView.context, R.color.accent_orange)
                    "Tempo" -> ContextCompat.getColor(itemView.context, R.color.light_purple)
                    "Long Run" -> ContextCompat.getColor(itemView.context, R.color.purple)
                    "Recovery" -> ContextCompat.getColor(itemView.context, R.color.accent_green)
                    "Rest", "พัก" -> ContextCompat.getColor(itemView.context, R.color.type_rest)
                    "ฝึกซ้อน" -> ContextCompat.getColor(itemView.context, R.color.type_default)
                    else -> ContextCompat.getColor(itemView.context, R.color.type_default)
                }

                cardWeekDay.setCardBackgroundColor(bgColor)

                // ปุ่มแก้ไข
                btnEdit.setOnClickListener {
                    onEditClick(weekDay)
                }

                // ปุ่มลบ
                btnDelete.setOnClickListener {
                    onDeleteClick(weekDay)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeekDayViewHolder {
        val binding = ItemWeekDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WeekDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WeekDayViewHolder, position: Int) {
        holder.bind(weekDays[position])
    }

    override fun getItemCount() = weekDays.size

    // ฟังก์ชันเพิ่มเติม
    fun updateData(newList: List<WeekDay>) {
        weekDays.clear()
        weekDays.addAll(newList)
        notifyDataSetChanged()
    }

    fun addItem(weekDay: WeekDay) {
        weekDays.add(weekDay)
        notifyItemInserted(weekDays.size - 1)
    }

    fun removeItem(position: Int) {
        weekDays.removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateItem(position: Int, weekDay: WeekDay) {
        weekDays[position] = weekDay
        notifyItemChanged(position)
    }
}