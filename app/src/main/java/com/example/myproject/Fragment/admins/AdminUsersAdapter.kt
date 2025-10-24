package com.example.myproject.Fragment.admins

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myproject.R
import com.example.myproject.data.startprogram.UserProgramData
import com.example.myproject.databinding.ItemAdminUserBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminUsersAdapter(
    private val users: MutableList<UserProgramData>,
    private val onViewDetailsClick: (UserProgramData) -> Unit = {},
    private val onDeleteClick: (UserProgramData) -> Unit = {}
) : RecyclerView.Adapter<AdminUsersAdapter.UserViewHolder>() {

    private var filteredUsers: MutableList<UserProgramData> = users

    inner class UserViewHolder(private val binding: ItemAdminUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserProgramData) {
            binding.apply {
                // ข้อมูลผู้ใช้
                tvUserName.text = user.userName
                tvUserEmail.text = user.email

                // ข้อมูลโปรแกรม
                tvProgramName.text = user.programDisplayName
                tvSubProgram.text = user.subProgramName
                tvCurrentWeek.text = "สัปดาห์ที่: ${user.currentWeek}/4"

                // ความก้าวหน้า
                progressBar.progress = user.progress
                tvProgress.text = "${user.progress}%"

                // วันที่เลือก
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                tvSelectedDate.text = "เลือกเมื่อ: ${dateFormat.format(Date(user.selectedAt))}"

                // สถานะ
                val statusText = if (user.isActive) "กำลังใช้งาน" else "หยุดใช้งาน"
                val statusColor = if (user.isActive)
                    ContextCompat.getColor(itemView.context, R.color.accent_green)
                else
                    ContextCompat.getColor(itemView.context, R.color.accent_red)

                tvStatus.text = statusText
                tvStatus.setTextColor(statusColor)

                // Card stroke color
                cardUser.strokeColor = statusColor

                // คลิก Card เพื่อดูรายละเอียด
                cardUser.setOnClickListener {
                    onViewDetailsClick(user)
                }

                // Long Click เพื่อลบ (Optional)
                cardUser.setOnLongClickListener {
                    onDeleteClick(user)
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemAdminUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(filteredUsers[position])
    }

    override fun getItemCount() = filteredUsers.size

    /**
     * อัพเดทรายการ (สำหรับ Search/Filter)
     */
    fun updateList(newList: List<UserProgramData>) {
        filteredUsers = newList.toMutableList()
        notifyDataSetChanged()
    }

    /**
     * รีเฟรชข้อมูล (หลังโหลดจาก Firebase)
     */
    fun refresh() {
        filteredUsers = users
        notifyDataSetChanged()
    }
}