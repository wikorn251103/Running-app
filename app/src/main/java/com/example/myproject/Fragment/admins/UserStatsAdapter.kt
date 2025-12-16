package com.example.myproject.Fragment.admins

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.data.admin.UserStat
import com.example.myproject.databinding.ItemUserStatBinding

class UserStatsAdapter : RecyclerView.Adapter<UserStatsAdapter.ViewHolder>() {

    private var userStats = listOf<UserStat>()

    fun updateData(newData: List<UserStat>) {
        userStats = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserStatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(userStats[position], position + 1)
    }

    override fun getItemCount() = userStats.size

    inner class ViewHolder(private val binding: ItemUserStatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(stat: UserStat, rank: Int) {
            binding.apply {
                // แสดงอันดับ
                tvRank.text = when (rank) {
                    1 -> "🥇"
                    2 -> "🥈"
                    3 -> "🥉"
                    else -> "$rank"
                }

                tvUserName.text = stat.name
                tvProgram.text = "โปรแกรม: ${stat.program}"
                tvDistance.text = "ระยะทาง: %.2f กม.".format(stat.totalDistance)
                tvWorkouts.text = "ครั้งที่ซ้อม: ${stat.totalWorkouts}"
                tvCompletionRate.text = "%.1f%%".format(stat.completionRate)

                // เปลี่ยนสีตามอันดับ
                when (rank) {
                    1 -> cardView.setCardBackgroundColor(Color.rgb(255, 215, 0)) // ทอง
                    2 -> cardView.setCardBackgroundColor(Color.rgb(192, 192, 192)) // เงิน
                    3 -> cardView.setCardBackgroundColor(Color.rgb(205, 127, 50)) // ทองแดง
                    else -> cardView.setCardBackgroundColor(Color.WHITE)
                }
            }
        }
    }
}