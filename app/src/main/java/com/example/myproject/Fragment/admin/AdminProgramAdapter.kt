package com.example.myproject.Fragment.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.data.admin.AthleteModel
import com.example.myproject.databinding.ItemProgramBinding

class AdminProgramAdapter(
    private val onCancelClick: (AthleteModel) -> Unit,
    private val onDetailClick: (AthleteModel) -> Unit
) : ListAdapter<AthleteModel, AdminProgramAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemProgramBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AthleteModel) {
            binding.tvProgramName.text = item.name
            binding.tvDuration.text = item.trainingPlan.ifEmpty { "ไม่มีแผน" }
            binding.tvStatus.text = if (item.trainingPlan.isEmpty()) "ขาดซ้อม" else "ปกติ"
            binding.btnDelete.setOnClickListener { onCancelClick(item) }
            binding.btnEdit.setOnClickListener { onDetailClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProgramBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<AthleteModel>() {
        override fun areItemsTheSame(oldItem: AthleteModel, newItem: AthleteModel) = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: AthleteModel, newItem: AthleteModel) = oldItem == newItem
    }
}