package com.example.myproject.Fragment.training

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R
import com.example.myproject.data.training.TrainingModel
import com.example.myproject.databinding.ItemTrainingDayBinding

class TrainingScheduleAdapter : RecyclerView.Adapter<TrainingScheduleAdapter.TrainingViewHolder>() {

    private var trainingDays = mutableListOf<TrainingModel>()

    fun updateTrainingDays(newDays: List<TrainingModel>) {
        trainingDays.clear()
        trainingDays.addAll(newDays)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training_day, parent, false)
        return TrainingViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainingViewHolder, position: Int) {
        holder.bind(trainingDays[position])
    }

    override fun getItemCount(): Int = trainingDays.size

    inner class TrainingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val tvDayName: TextView = itemView.findViewById(R.id.tvDayName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val tvPace: TextView = itemView.findViewById(R.id.tvPace)

        private fun getDayName(dayNumber: Int): String {
            return when (dayNumber) {
                1 -> "จันทร์"
                2 -> "อังคาร"
                3 -> "พุธ"
                4 -> "พฤหัสบดี"
                5 -> "ศุกร์"
                6 -> "เสาร์"
                7 -> "อาทิตย์"
                else -> ""
            }
        }

        fun bind(trainingDay: TrainingModel) {
            tvDay.text = trainingDay.day
            tvDayName.text = getDayName(trainingDay.day.toIntOrNull() ?: 0)
            tvDescription.text = trainingDay.description
            tvType.text = trainingDay.type

            // ตั้งสีพื้นหลังตามประเภท (ถ้ามี)
            val typeColor = when (trainingDay.type.lowercase()) {
                "easy" -> R.color.accent_green
                "interval" -> R.color.accent_red
                "threshold" -> R.color.accent_orange
                "rest day" -> R.color.light_purple
                "long run" -> R.color.accent_blue
                else -> R.color.grey_text
            }
            tvType.setBackgroundColor(ContextCompat.getColor(itemView.context, typeColor))

            if (trainingDay.pace.isNotEmpty()) {
                tvPace.text = "เป้าหมาย ${trainingDay.pace}"
                tvPace.visibility = View.VISIBLE
            } else {
                tvPace.visibility = View.GONE
            }
        }
    }
}


