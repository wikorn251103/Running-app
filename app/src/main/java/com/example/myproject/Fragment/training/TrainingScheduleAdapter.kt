package com.example.myproject.Fragment.training

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R
import com.example.myproject.data.training.TrainingModel

class TrainingScheduleAdapter(
    private val onStartWorkout: (TrainingModel, Int, Int) -> Unit // callback เมื่อกดเริ่มบันทึก
) : RecyclerView.Adapter<TrainingScheduleAdapter.TrainingViewHolder>() {

    private var trainingDays = mutableListOf<TrainingModel>()
    private var currentWeek: Int = 1

    fun updateTrainingDays(newDays: List<TrainingModel>, weekNumber: Int = 1) {
        trainingDays.clear()
        trainingDays.addAll(newDays)
        currentWeek = weekNumber
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training_day_with_action, parent, false)
        return TrainingViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrainingViewHolder, position: Int) {
        holder.bind(trainingDays[position], position)
    }

    override fun getItemCount(): Int = trainingDays.size

    inner class TrainingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDay: TextView = itemView.findViewById(R.id.tvDay)
        private val tvDayName: TextView = itemView.findViewById(R.id.tvDayName)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val tvPace: TextView = itemView.findViewById(R.id.tvPace)
        private val btnStartWorkout: Button = itemView.findViewById(R.id.btnStartWorkout)

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

        fun bind(trainingDay: TrainingModel, position: Int) {
            val dayNumber = position + 1

            tvDay.text = dayNumber.toString()
            tvDayName.text = getDayName(dayNumber)
            tvDescription.text = trainingDay.description
            tvType.text = trainingDay.type

            // ⭐ แสดงสถานะ 3 แบบ
            when {
                trainingDay.isCompleted -> {
                    // ✅ ทำแล้ว
                    itemView.alpha = 0.6f
                    tvDay.text = "✅"
                    btnStartWorkout.visibility = View.GONE
                }
                trainingDay.isMissed -> {
                    // ❌ ขาดซ้อม
                    itemView.alpha = 0.6f
                    tvDay.text = "❌"
                    btnStartWorkout.visibility = View.GONE
                    itemView.setBackgroundColor(
                        ContextCompat.getColor(itemView.context, R.color.light_red)
                    )
                }
                else -> {
                    // ⏳ รอทำ
                    itemView.alpha = 1.0f
                    btnStartWorkout.visibility = View.VISIBLE
                }
            }

            // ตั้งสีพื้นหลังตามประเภท
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

            // ปุ่มเริ่มบันทึก
            btnStartWorkout.setOnClickListener {
                onStartWorkout(trainingDay, currentWeek, dayNumber)
            }
        }
    }
}