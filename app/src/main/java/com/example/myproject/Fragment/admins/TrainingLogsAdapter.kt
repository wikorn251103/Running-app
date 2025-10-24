package com.example.myproject.Fragment.admins

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.data.startprogram.TrainingLog
import com.example.myproject.databinding.ItemTrainingLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrainingLogsAdapter(
    private val logs: MutableList<TrainingLog>,
    private val onEditClick: (TrainingLog) -> Unit,
    private val onDeleteClick: (TrainingLog) -> Unit
) : RecyclerView.Adapter<TrainingLogsAdapter.LogViewHolder>() {

    inner class LogViewHolder(private val binding: ItemTrainingLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: TrainingLog) {
            binding.apply {
                // ชื่อ session
                tvSessionName.text = log.sessionName

                // ข้อมูลการซ้อม
                val durationMin = log.duration / 60
                tvStats.text = "${log.distance} km • $durationMin นาที • ${log.pace}/km"

                // วันที่
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                tvDate.text = dateFormat.format(Date(log.completedAt))

                // Notes (ถ้ามี)
                if (log.notes.isNotEmpty()) {
                    tvNotes.visibility = View.VISIBLE
                    tvNotes.text = "📝 ${log.notes}"
                } else {
                    tvNotes.visibility = View.GONE
                }

                // สถานะ
                val statusText = when (log.status) {
                    "completed" -> "✅ เสร็จสมบูรณ์"
                    "skipped" -> "⏭️ ข้าม"
                    "partial" -> "⚠️ ทำไม่ครบ"
                    else -> log.status
                }
                tvStatus.text = statusText

                // ปุ่มแก้ไข
                btnEdit.setOnClickListener {
                    onEditClick(log)
                }

                // ปุ่มลบ
                btnDelete.setOnClickListener {
                    onDeleteClick(log)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemTrainingLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount() = logs.size
}