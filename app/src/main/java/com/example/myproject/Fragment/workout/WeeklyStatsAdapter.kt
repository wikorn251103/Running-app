package com.example.myproject.Fragment.workout

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R
import com.example.myproject.data.workout.WeeklyStats

class WeeklyStatsAdapter : RecyclerView.Adapter<WeeklyStatsAdapter.ViewHolder>() {

    private var weeklyStats = listOf<WeeklyStats>()

    fun updateStats(stats: List<WeeklyStats>) {
        weeklyStats = stats
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_weekly_stats, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(weeklyStats[position])
    }

    override fun getItemCount() = weeklyStats.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvWeek: TextView = itemView.findViewById(R.id.tvWeek)
        private val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        private val tvWorkouts: TextView = itemView.findViewById(R.id.tvWorkouts)

        fun bind(stats: WeeklyStats) {
            tvWeek.text = "สัปดาห์ ${stats.weekNumber}"
            tvDistance.text = String.format("%.2f กม.", stats.totalDistance)
            tvWorkouts.text = "${stats.totalWorkouts} ครั้ง"
        }
    }
}