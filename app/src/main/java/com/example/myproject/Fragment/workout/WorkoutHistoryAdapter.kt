package com.example.myproject.Fragment.workout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R
import com.example.myproject.data.workout.WorkoutLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkoutHistoryAdapter(
    private val onItemClick: (WorkoutLog) -> Unit
) : RecyclerView.Adapter<WorkoutHistoryAdapter.WorkoutHistoryViewHolder>() {

    private var workoutLogs = mutableListOf<WorkoutLog>()

    fun updateLogs(newLogs: List<WorkoutLog>) {
        workoutLogs.clear()
        workoutLogs.addAll(newLogs)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_workout_history, parent, false)
        return WorkoutHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: WorkoutHistoryViewHolder, position: Int) {
        holder.bind(workoutLogs[position])
    }

    override fun getItemCount(): Int = workoutLogs.size

    inner class WorkoutHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvWeekDay: TextView = itemView.findViewById(R.id.tvWeekDay)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvPace: TextView = itemView.findViewById(R.id.tvPace)
        private val tvFeeling: TextView = itemView.findViewById(R.id.tvFeeling)

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("th", "TH"))
        private val timeFormat = SimpleDateFormat("HH:mm", Locale("th", "TH"))

        fun bind(workoutLog: WorkoutLog) {
            val date = Date(workoutLog.completedAt)

            tvDate.text = dateFormat.format(date)
            tvWeekDay.text = "สัปดาห์ ${workoutLog.weekNumber} - วันที่ ${workoutLog.dayNumber}"
            tvType.text = workoutLog.trainingType
            tvDistance.text = String.format("%.2f กม.", workoutLog.actualDistance)
            tvDuration.text = workoutLog.getFormattedDuration()
            tvPace.text = "${workoutLog.calculateAveragePace()}/กม."

            if (workoutLog.feeling.isNotEmpty()) {
                tvFeeling.text = getFeelingEmoji(workoutLog.feeling)
                tvFeeling.visibility = View.VISIBLE
            } else {
                tvFeeling.visibility = View.GONE
            }

            // ตั้งสีตามประเภท
            val typeColor = when (workoutLog.trainingType.lowercase()) {
                "easy" -> R.color.accent_green
                "interval" -> R.color.accent_red
                "threshold" -> R.color.accent_orange
                "rest day" -> R.color.light_purple
                "long run" -> R.color.accent_blue
                else -> R.color.grey_text
            }
            tvType.setBackgroundColor(ContextCompat.getColor(itemView.context, typeColor))

            itemView.setOnClickListener {
                onItemClick(workoutLog)
            }
        }

        private fun getFeelingEmoji(feeling: String): String {
            return when (feeling) {
                "Great" -> "😄 Great"
                "Good" -> "😊 Good"
                "Okay" -> "😐 Okay"
                "Tired" -> "😓 Tired"
                "Struggling" -> "😰 Struggling"
                else -> feeling
            }
        }
    }
}