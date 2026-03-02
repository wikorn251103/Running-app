package com.example.myproject.Fragment.workout

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R
import com.example.myproject.data.workout.WorkoutLog
import java.text.SimpleDateFormat
import java.util.*

class WorkoutHistoryAdapter(
    private val onItemClick: (WorkoutLog) -> Unit = {}
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

    override fun getItemCount() = workoutLogs.size

    inner class WorkoutHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvWeekDay: TextView = itemView.findViewById(R.id.tvWeekDay)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvPace: TextView = itemView.findViewById(R.id.tvPace)
        private val tvFeeling: TextView = itemView.findViewById(R.id.tvFeeling)

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("th", "TH"))

        fun bind(log: WorkoutLog) {
            tvDate.text = dateFormat.format(Date(log.completedAt))
            tvWeekDay.text = "สัปดาห์ ${log.weekNumber} - วันที่ ${log.dayNumber}"
            tvType.text = log.trainingType
            tvDistance.text = String.format("%.2f กม.", log.actualDistance)
            tvDuration.text = log.getFormattedDuration()
            tvPace.text = "${log.calculateAveragePace()}/กม."

            // ✅ แสดง Emoji จาก paceResult แทน feeling ที่เคยให้เลือก
            val emoji = log.getPaceEmoji()
            if (emoji.isNotEmpty()) {
                tvFeeling.text = emoji
                tvFeeling.visibility = View.VISIBLE
            } else {
                tvFeeling.visibility = View.GONE
            }

            // สีตามประเภทการซ้อม
            val typeColor = when (log.trainingType.lowercase()) {
                "easy", "easy run", "recovery run", "recovery" -> R.color.accent_green
                "interval", "race day"                          -> R.color.accent_red
                "threshold"                                     -> R.color.accent_orange
                "rest day", "rest"                              -> R.color.light_purple
                "long run"                                      -> R.color.accent_blue
                "easy run&repetition", "easy&repetition"        -> R.color.yellow
                else                                            -> R.color.grey_text
            }
            tvType.setBackgroundColor(ContextCompat.getColor(itemView.context, typeColor))

            // ✅ กดการ์ด → Dialog แสดง Feedback
            itemView.setOnClickListener { showPaceFeedbackDialog(log) }
        }

        private fun showPaceFeedbackDialog(log: WorkoutLog) {
            val context = itemView.context
            val emoji = log.getPaceEmoji()

            val title = if (emoji.isNotEmpty()) "$emoji  ${log.getPaceFeedbackTitle()}"
            else "ผลการซ้อม"

            val message = buildString {
                // Feedback หลัก
                if (log.paceResult.isNotEmpty()) {
                    append(log.getPaceFeedbackMessage())
                    append("\n\n")
                }
                // รายละเอียด
                append("━━━━━━━━━━━━━━\n")
                append("📅  ${dateFormat.format(Date(log.completedAt))}\n")
                append("🏃  ${log.trainingType}\n")
                append("📏  ${String.format("%.2f", log.actualDistance)} กม.\n")
                append("⏱  ${log.getFormattedDuration()}\n")
                append("⚡  เพซจริง: ${log.calculateAveragePace()} /กม.\n")
                if (log.plannedPace.isNotEmpty()) {
                    append("🎯  เพซเป้า: ${log.plannedPace}\n")
                }
                if (log.averageHeartRate > 0) {
                    append("❤️  HR: ${log.averageHeartRate} bpm\n")
                }
                if (log.notes.isNotEmpty()) {
                    append("\n📝  ${log.notes}")
                }
            }

            val builder = AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("ปิด", null)

            // ✅ ถ้า pace ช้ามาก → ปุ่มดูคลิป
            if (log.paceResult == "slower_much") {
                builder.setNeutralButton("📺 ดูคลิปเทคนิค") { _, _ ->
                    try {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.youtube.com/watch?v=YOUR_VIDEO_ID")
                            )
                        )
                    } catch (e: Exception) { /* ignore */ }
                }
            }

            builder.show()
        }
    }
}