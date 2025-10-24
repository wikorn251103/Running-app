package com.example.myproject.Fragment.admins

import com.example.myproject.data.admin.TrainingProgramModel
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R

class TrainingProgramAdapter(
    private var programs: List<TrainingProgramModel>,
    private val onProgramClick: (TrainingProgramModel) -> Unit,
    private val onEditClick: (TrainingProgramModel) -> Unit,
    private val onViewClick: (TrainingProgramModel) -> Unit,
    private val onStatsClick: (TrainingProgramModel) -> Unit,
    private val onDeleteClick: (TrainingProgramModel) -> Unit,
    private val onCopyClick: (TrainingProgramModel) -> Unit
) : RecyclerView.Adapter<TrainingProgramAdapter.ProgramViewHolder>() {

    inner class ProgramViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvProgramName: TextView = itemView.findViewById(R.id.tvProgramName)
        val tvCategory: TextView = itemView.findViewById(R.id.tvProgramCategory)
        val tvWeeks: TextView = itemView.findViewById(R.id.tvProgramWeeks)
        val tvActiveUsers: TextView = itemView.findViewById(R.id.tvActiveUsers)
        val tvCompletedUsers: TextView = itemView.findViewById(R.id.tvCompletedUsers)
        val tvCompletionRate: TextView = itemView.findViewById(R.id.tvCompletionRate)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditProgram)
        val btnView: ImageButton = itemView.findViewById(R.id.btnViewProgram)
        val btnStats: ImageButton = itemView.findViewById(R.id.btnProgramStats)
        val btnCopy: ImageButton = itemView.findViewById(R.id.btnCopyProgram)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteProgram)

        fun bind(program: TrainingProgramModel) {
            tvProgramName.text = program.programName

            // Category with emoji
            val categoryEmoji = when (program.category) {
                "5K" -> "🏃"
                "10K" -> "🏃‍♂️"
                "Half Marathon" -> "🏃‍♀️"
                "Marathon" -> "🏅"
                else -> "📝"
            }
            tvCategory.text = "$categoryEmoji ${program.category}"

            tvWeeks.text = "📅 ${program.weeks} สัปดาห์"
            tvActiveUsers.text = "👥 ${program.activeUsers} คน"
            tvCompletedUsers.text = "✅ ${program.completedUsers} คน"

            // Completion rate
            val completionRate = if (program.activeUsers > 0) {
                (program.completedUsers.toFloat() / program.activeUsers * 100).toInt()
            } else {
                0
            }
            tvCompletionRate.text = "📊 $completionRate%"

            // Set completion rate color
            val rateColor = when {
                completionRate >= 70 -> R.color.accent_green
                completionRate >= 40 -> R.color.accent_orange
                else -> R.color.accent_red
            }
            tvCompletionRate.setTextColor(itemView.context.getColor(rateColor))

            // Click Listeners
            itemView.setOnClickListener { onProgramClick(program) }
            btnEdit.setOnClickListener { onEditClick(program) }
            btnView.setOnClickListener { onViewClick(program) }
            btnStats.setOnClickListener { onStatsClick(program) }
            btnCopy.setOnClickListener { onCopyClick(program) }
            btnDelete.setOnClickListener { onDeleteClick(program) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgramViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_training_program_admin, parent, false)
        return ProgramViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProgramViewHolder, position: Int) {
        holder.bind(programs[position])
    }

    override fun getItemCount(): Int = programs.size

    /**
     * อัพเดทรายการโปรแกรม
     */
    fun updatePrograms(newPrograms: List<TrainingProgramModel>) {
        programs = newPrograms
        notifyDataSetChanged()
    }
}