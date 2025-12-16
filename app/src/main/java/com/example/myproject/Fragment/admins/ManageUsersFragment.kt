package com.example.myproject.Fragment.admins

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.MainActivity
import com.example.myproject.data.admin.UserModel
import com.example.myproject.databinding.FragmentManageUsersBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ManageUsersFragment : Fragment() {

    private var _binding: FragmentManageUsersBinding? = null
    private val binding get() = _binding!!

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var userAdapter: UserAdapter
    private val usersList = mutableListOf<UserModel>()
    private val filteredList = mutableListOf<UserModel>()

    companion object {
        private const val TAG = "ManageUsers"
        fun newInstance() = ManageUsersFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchView()
        setupClickListeners()
        loadUsers()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(
            filteredList,
            onUserClick = { user ->
                showUserDetailDialog(user)
            },
            onViewTrainingClick = { user ->
                navigateToUserTraining(user)
            },
            onDeleteClick = { user ->
                showDeleteConfirmDialog(user)
            },
            onToggleActiveClick = { user ->
                toggleUserActive(user)
            }
        )

        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterUsers(newText ?: "")
                return true
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.btnRefresh.setOnClickListener {
            loadUsers()
        }

        // Filter buttons
        binding.chipAll.setOnClickListener {
            filterUsers("")
        }

        binding.chipActive.setOnClickListener {
            filterUsersByStatus(true)
        }

        binding.chipInactive.setOnClickListener {
            filterUsersByStatus(false)
        }
    }

    /**
     * โหลดรายการผู้ใช้ทั้งหมด - แก้ไขให้ดึงข้อมูลจาก Athletes ที่ถูกต้อง
     */
    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        firestore.collection("Athletes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { athleteDocs ->
                usersList.clear()

                if (athleteDocs.isEmpty) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                val totalDocs = athleteDocs.size()
                var processed = 0

                for (athleteDoc in athleteDocs) {
                    val userId = athleteDoc.getString("userId") ?: athleteDoc.id

                    // ✅ ดึงข้อมูลจาก users/{userId}
                    firestore.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("name") ?: "ไม่มีชื่อ"
                            val email = userDoc.getString("email") ?: ""
                            val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""

                            // ✅ FIX: ดึงข้อมูลโปรแกรมจาก Athletes collection ที่ถูกต้อง
                            val currentProgramId = athleteDoc.getString("programId")
                                ?: athleteDoc.getString("currentProgramId")
                                ?: ""

                            val programDisplayName = athleteDoc.getString("programName")
                                ?: athleteDoc.getString("programDisplayName")
                                ?: ""

                            val isActive = athleteDoc.getBoolean("isActive")
                                ?: athleteDoc.getBoolean("hasActiveProgram")
                                ?: !currentProgramId.isEmpty()

                            val user = UserModel(
                                userId = userId,
                                name = name,
                                email = email,
                                profileImageUrl = profileImageUrl,
                                role = "athlete",
                                createdAt = athleteDoc.getLong("createdAt") ?: 0,
                                hasActiveProgram = isActive,
                                currentProgramId = currentProgramId,
                                programDisplayName = if (programDisplayName.isEmpty() && currentProgramId.isNotEmpty())
                                    "มีโปรแกรม" else programDisplayName
                            )

                            usersList.add(user)

                            // 🔍 Debug log
                            Log.d(TAG, "User: ${user.name}, ProgramID: ${user.currentProgramId}, Active: ${user.hasActiveProgram}")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to fetch user info for $userId", e)
                        }
                        .addOnCompleteListener {
                            processed++
                            if (processed == totalDocs) {
                                // โหลดครบทุกคนแล้ว
                                filteredList.clear()
                                filteredList.addAll(usersList)
                                userAdapter.notifyDataSetChanged()
                                binding.progressBar.visibility = View.GONE
                                binding.tvTotalUsers.text = "ผู้ใช้ทั้งหมด: ${usersList.size} คน"

                                if (usersList.isEmpty()) {
                                    binding.tvEmptyState.visibility = View.VISIBLE
                                }
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load athletes", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "ไม่สามารถโหลดข้อมูลได้", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * กรองผู้ใช้ตามคำค้นหา
     */
    private fun filterUsers(query: String) {
        filteredList.clear()

        if (query.isEmpty()) {
            filteredList.addAll(usersList)
        } else {
            val searchQuery = query.lowercase()
            filteredList.addAll(
                usersList.filter {
                    it.name.lowercase().contains(searchQuery) ||
                            it.email.lowercase().contains(searchQuery)
                }
            )
        }

        userAdapter.notifyDataSetChanged()
        binding.tvTotalUsers.text = "พบ ${filteredList.size} คน"
    }

    /**
     * กรองตามสถานะ Active/Inactive
     */
    private fun filterUsersByStatus(isActive: Boolean) {
        filteredList.clear()
        filteredList.addAll(usersList.filter { it.hasActiveProgram == isActive })
        userAdapter.notifyDataSetChanged()
        binding.tvTotalUsers.text = "พบ ${filteredList.size} คน"
    }

    /**
     * ✅ FIX: ไปหน้าดูและแก้ไขตารางซ้อมของผู้ใช้
     */
    private fun navigateToUserTraining(user: UserModel) {
        if (user.currentProgramId.isEmpty() || user.currentProgramId.isBlank()) {
            Toast.makeText(requireContext(), "${user.name} ยังไม่มีโปรแกรมการซ้อม", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "User ${user.userId} has no program ID")
            return
        }

        // ✅ ข้ามการตรวจสอบและเปิดหน้าแก้ไขโดยตรง (เพราะข้อมูลอยู่ใน Athletes/{userId})
        Log.d(TAG, "✅ Opening training detail for user: ${user.name}, programId: ${user.currentProgramId}")

        val fragment = UserTrainingDetailFragment.newInstance(
            userId = user.userId,
            userName = user.name,
            programId = user.currentProgramId
        )
        (activity as? MainActivity)?.replaceFragment(fragment)
    }

    /**
     * แสดง Dialog รายละเอียดผู้ใช้
     */
    private fun showUserDetailDialog(user: UserModel) {
        val message = """
        👤 ชื่อ: ${user.name}
        📧 อีเมล: ${user.email}
        🏷️ บทบาท: ${user.role}
        📅 สมัครเมื่อ: ${formatDate(user.createdAt)}
        🏃 สถานะ: ${if (user.hasActiveProgram) "กำลังใช้โปรแกรม" else "ไม่มีโปรแกรม"}
        📝 โปรแกรม: ${if (user.programDisplayName.isEmpty()) "ไม่มี" else user.programDisplayName}
        🔑 Program ID: ${if (user.currentProgramId.isEmpty()) "ไม่มี" else user.currentProgramId}
    """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("รายละเอียดผู้ใช้")
            .setMessage(message)
            .setPositiveButton("ปิด", null)
            .setNeutralButton("แก้ไขตาราง") { _, _ ->
                navigateToUserTraining(user)
            }
            .show()
    }

    /**
     * ✅ FIX: Toggle สถานะ Active/Inactive - อัปเดตทั้ง 2 collections
     */
    private fun toggleUserActive(user: UserModel) {
        val newStatus = !user.hasActiveProgram
        val action = if (newStatus) "เปิดใช้งาน" else "ปิดการใช้งาน"

        AlertDialog.Builder(requireContext())
            .setTitle("$action ผู้ใช้")
            .setMessage("คุณต้องการ$action ${user.name} หรือไม่?")
            .setPositiveButton("ยืนยัน") { _, _ ->
                binding.progressBar.visibility = View.VISIBLE

                // อัปเดต Athletes collection
                firestore.collection("Athletes")
                    .document(user.userId)
                    .update("isActive", newStatus)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "${action}สำเร็จ", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to toggle user status", e)
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "เกิดข้อผิดพลาด", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    /**
     * แสดง Dialog ยืนยันการลบ
     */
    private fun showDeleteConfirmDialog(user: UserModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ ลบผู้ใช้")
            .setMessage("คุณต้องการลบ ${user.name} หรือไม่?\n\nการดำเนินการนี้ไม่สามารถย้อนกลับได้")
            .setPositiveButton("ลบ") { _, _ ->
                deleteUser(user)
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    /**
     * ลบผู้ใช้
     */
    private fun deleteUser(user: UserModel) {
        binding.progressBar.visibility = View.VISIBLE

        // ลบจาก Athletes collection ก่อน
        firestore.collection("Athletes")
            .document(user.userId)
            .delete()
            .addOnSuccessListener {
                // ลบจาก users collection
                firestore.collection("users")
                    .document(user.userId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "ลบผู้ใช้สำเร็จ", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to delete from users", e)
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "ลบไม่สมบูรณ์", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete from Athletes", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "ไม่สามารถลบได้", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * แปลง timestamp เป็นวันที่
     */
    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "ไม่ทราบ"
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("th", "TH"))
        return format.format(date)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}