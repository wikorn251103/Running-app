package com.example.myproject.Fragment.admins

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myproject.R
import com.example.myproject.data.admin.UserModel
import java.text.SimpleDateFormat
import java.util.*

class UserAdapter(
    private val users: List<UserModel>,
    private val onUserClick: (UserModel) -> Unit,
    private val onViewTrainingClick: (UserModel) -> Unit,
    private val onDeleteClick: (UserModel) -> Unit,
    private val onToggleActiveClick: (UserModel) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivProfile: ImageView = itemView.findViewById(R.id.ivUserProfile)
        val tvName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        val tvRole: TextView = itemView.findViewById(R.id.tvUserRole)
        val tvStatus: TextView = itemView.findViewById(R.id.tvUserStatus)
        val tvProgram: TextView = itemView.findViewById(R.id.tvUserProgram)
        val tvCreatedDate: TextView = itemView.findViewById(R.id.tvCreatedDate)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteUser)
        val btnToggle: ImageButton = itemView.findViewById(R.id.btnToggleActive)

        fun bind(user: UserModel) {
            tvName.text = user.name
            tvEmail.text = user.email
            tvRole.text = when (user.role) {
                "admin" -> "🔧 Admin"
                else -> "👤 ผู้ใช้"
            }

            // สถานะ
            if (user.hasActiveProgram) {
                tvStatus.text = "🏃 Active"
                tvStatus.setTextColor(itemView.context.getColor(R.color.accent_green))
                btnToggle.setImageResource(R.drawable.ic_toggle_on)
            } else {
                tvStatus.text = "⏸️ Inactive"
                tvStatus.setTextColor(itemView.context.getColor(R.color.grey_text))
                btnToggle.setImageResource(R.drawable.ic_toggle_off)
            }

            // โปรแกรมปัจจุบัน
            tvProgram.text = if (user.currentProgramId.isNotEmpty()) {
                "📝 ${user.currentProgramId}"
            } else {
                "ไม่มีโปรแกรม"
            }

            // วันที่สมัคร
            tvCreatedDate.text = "📅 ${formatDate(user.createdAt)}"

            // โหลดรูปโปรไฟล์
            if (user.profileImageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.ic_user_placeholder)
                    .circleCrop()
                    .into(ivProfile)
            } else {
                ivProfile.setImageResource(R.drawable.ic_user_placeholder)
            }

            // Click Listeners
            itemView.setOnClickListener { onUserClick(user) }
            btnDelete.setOnClickListener { onDeleteClick(user) }
            btnToggle.setOnClickListener { onToggleActiveClick(user) }
        }

        private fun formatDate(timestamp: Long): String {
            if (timestamp == 0L) return "ไม่ทราบ"
            val date = Date(timestamp)
            val format = SimpleDateFormat("dd/MM/yy", Locale("th", "TH"))
            return format.format(date)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_admin, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size
}