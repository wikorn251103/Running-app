package com.example.myproject.Fragment.admins

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.MainActivity
import com.example.myproject.R
import com.example.myproject.data.admin.TrainingProgramModel
import com.example.myproject.databinding.FragmentManageTrainingPlansBinding
import com.google.firebase.firestore.FirebaseFirestore

class ManageTrainingPlansFragment : Fragment() {

    private var _binding: FragmentManageTrainingPlansBinding? = null
    private val binding get() = _binding!!

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var programAdapter: TrainingProgramAdapter
    private val programsList = mutableListOf<TrainingProgramModel>()

    companion object {
        private const val TAG = "ManageTrainingPlans"
        fun newInstance() = ManageTrainingPlansFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageTrainingPlansBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadTrainingPlans()
    }

    private fun setupRecyclerView() {
        programAdapter = TrainingProgramAdapter(
            programsList,
            onProgramClick = { program ->
                showProgramDetailDialog(program)
            },
            onEditClick = { program ->
                navigateToEditProgram(program)
            },
            onViewClick = { program ->
                navigateToViewProgram(program)
            },
            onStatsClick = { program ->
                showProgramStats(program)
            },
            onDeleteClick = { program ->
                showDeleteConfirmDialog(program)
            },
            onCopyClick = { program ->
                showCopyProgramDialog(program)
            }
        )

        binding.recyclerViewPrograms.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = programAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.btnRefresh.setOnClickListener {
            loadTrainingPlans()
        }

        binding.fabAddProgram.setOnClickListener {
            showCreateProgramDialog()
        }

        // Filter by category
        binding.chip5K.setOnClickListener {
            filterProgramsByCategory("5K")
        }

        binding.chip10K.setOnClickListener {
            filterProgramsByCategory("10K")
        }

        binding.chipHalfMarathon.setOnClickListener {
            filterProgramsByCategory("Half Marathon")
        }

        binding.chipMarathon.setOnClickListener {
            filterProgramsByCategory("Marathon")
        }

        binding.chipAll.setOnClickListener {
            programAdapter.updatePrograms(programsList)
            binding.tvTotalPrograms.text = "โปรแกรมทั้งหมด: ${programsList.size} โปรแกรม"
        }
    }

    /**
     * โหลดรายการโปรแกรมทั้งหมด
     */
    private fun loadTrainingPlans() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        firestore.collection("training_plans")
            .get()
            .addOnSuccessListener { documents ->
                programsList.clear()

                for (doc in documents) {
                    // นับจำนวนผู้ใช้ที่ใช้โปรแกรมนี้
                    countProgramUsers(doc.id) { userCount ->
                        val program = TrainingProgramModel(
                            programId = doc.id,
                            programName = doc.id.replace("_", " ").uppercase(),
                            category = getCategoryFromId(doc.id),
                            weeks = countWeeks(doc.data),
                            daysPerWeek = 7,
                            activeUsers = userCount,
                            completedUsers = 0, // TODO: นับจำนวนคนที่จบจริง
                            createdAt = System.currentTimeMillis()
                        )
                        programsList.add(program)

                        // Update UI เมื่อโหลดครบทั้งหมด
                        if (programsList.size == documents.size()) {
                            programsList.sortByDescending { it.activeUsers }
                            programAdapter.updatePrograms(programsList)
                            binding.progressBar.visibility = View.GONE
                            binding.tvTotalPrograms.text = "โปรแกรมทั้งหมด: ${programsList.size} โปรแกรม"

                            if (programsList.isEmpty()) {
                                binding.tvEmptyState.visibility = View.VISIBLE
                            }
                        }
                    }
                }

                if (documents.isEmpty()) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                }

                Log.d(TAG, "Loaded ${documents.size()} training plans")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load training plans", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "ไม่สามารถโหลดข้อมูลได้", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * นับจำนวนผู้ใช้ที่ใช้โปรแกรมนี้
     */
    private fun countProgramUsers(programId: String, callback: (Int) -> Unit) {
        firestore.collection("Athletes")
            .whereEqualTo("programId", programId)
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { athletes ->
                callback(athletes.size())
            }
            .addOnFailureListener {
                callback(0)
            }
    }

    /**
     * นับจำนวนสัปดาห์ในโปรแกรม
     */
    private fun countWeeks(data: Map<String, Any>?): Int {
        if (data == null) return 0
        return data.keys.count { it.startsWith("week_") }
    }

    /**
     * แยกหมวดหมู่จาก programId
     */
    private fun getCategoryFromId(programId: String): String {
        return when {
            programId.contains("5k", ignoreCase = true) -> "5K"
            programId.contains("10k", ignoreCase = true) -> "10K"
            programId.contains("half", ignoreCase = true) -> "Half Marathon"
            programId.contains("marathon", ignoreCase = true) -> "Marathon"
            else -> "Other"
        }
    }

    /**
     * กรองโปรแกรมตามหมวดหมู่
     */
    private fun filterProgramsByCategory(category: String) {
        val filtered = programsList.filter { it.category == category }
        programAdapter.updatePrograms(filtered)
        binding.tvTotalPrograms.text = "พบ ${filtered.size} โปรแกรม"
    }

    /**
     * แสดง Dialog รายละเอียดโปรแกรม
     */
    private fun showProgramDetailDialog(program: TrainingProgramModel) {
        val completionRate = if (program.activeUsers > 0) {
            (program.completedUsers.toFloat() / program.activeUsers * 100).toInt()
        } else {
            0
        }

        val message = """
            📝 โปรแกรม: ${program.programName}
            🏷️ หมวดหมู่: ${program.category}
            📅 จำนวนสัปดาห์: ${program.weeks} สัปดาห์
            📆 วัน/สัปดาห์: ${program.daysPerWeek} วัน
            
            👥 กำลังใช้งาน: ${program.activeUsers} คน
            ✅ จบโปรแกรม: ${program.completedUsers} คน
            📊 อัตราความสำเร็จ: $completionRate%
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("รายละเอียดโปรแกรม")
            .setMessage(message)
            .setPositiveButton("ปิด", null)
            .setNeutralButton("ดูตาราง") { _, _ ->
                navigateToViewProgram(program)
            }
            .show()
    }

    /**
     * Dialog สร้างโปรแกรมใหม่
     */
    private fun showCreateProgramDialog() {
        val input = android.widget.EditText(requireContext())
        input.hint = "ชื่อโปรแกรม เช่น 5k_sub20"

        AlertDialog.Builder(requireContext())
            .setTitle("➕ สร้างโปรแกรมใหม่")
            .setMessage("กรุณากรอกชื่อโปรแกรม (ใช้ _ แทนช่องว่าง)")
            .setView(input)
            .setPositiveButton("สร้าง") { _, _ ->
                val programName = input.text.toString().trim().lowercase().replace(" ", "_")
                if (programName.isNotEmpty()) {
                    createNewProgram(programName)
                } else {
                    Toast.makeText(requireContext(), "กรุณากรอกชื่อโปรแกรม", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    /**
     * สร้างโปรแกรมใหม่ (Template)
     */
    private fun createNewProgram(programId: String) {
        binding.progressBar.visibility = View.VISIBLE

        // สร้าง Template 4 สัปดาห์
        val templateData = hashMapOf<String, Any>()

        for (week in 1..4) {
            val weekData = hashMapOf<String, Any>()
            for (day in 1..7) {
                val dayData = hashMapOf(
                    "day" to getDayName(day),
                    "description" to "ฝึกซ้อม",
                    "pace" to "",
                    "type" to if (day == 7) "Rest Day" else "Easy"
                )
                weekData["day_$day"] = dayData
            }
            templateData["week_$week"] = weekData
        }

        firestore.collection("training_plans")
            .document(programId)
            .set(templateData)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "สร้างโปรแกรม $programId สำเร็จ", Toast.LENGTH_SHORT).show()
                loadTrainingPlans()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create program", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "ไม่สามารถสร้างโปรแกรมได้", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * แสดงสถิติโปรแกรม
     */
    private fun showProgramStats(program: TrainingProgramModel) {
        Toast.makeText(requireContext(), "สถิติโปรแกรม ${program.programName}\nComing soon...", Toast.LENGTH_SHORT).show()
    }

    /**
     * Copy โปรแกรม
     */
    private fun showCopyProgramDialog(program: TrainingProgramModel) {
        val input = android.widget.EditText(requireContext())
        input.hint = "ชื่อโปรแกรมใหม่"
        input.setText("${program.programId}_copy")

        AlertDialog.Builder(requireContext())
            .setTitle("📋 Copy โปรแกรม")
            .setMessage("Copy จาก: ${program.programName}")
            .setView(input)
            .setPositiveButton("Copy") { _, _ ->
                val newProgramId = input.text.toString().trim().lowercase().replace(" ", "_")
                if (newProgramId.isNotEmpty() && newProgramId != program.programId) {
                    copyProgram(program.programId, newProgramId)
                } else {
                    Toast.makeText(requireContext(), "ชื่อไม่ถูกต้อง", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    /**
     * Copy โปรแกรม
     */
    private fun copyProgram(sourceProgramId: String, newProgramId: String) {
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("training_plans")
            .document(sourceProgramId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data
                    firestore.collection("training_plans")
                        .document(newProgramId)
                        .set(data!!)
                        .addOnSuccessListener {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Copy สำเร็จ!", Toast.LENGTH_SHORT).show()
                            loadTrainingPlans()
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to copy program", e)
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Copy ไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "ไม่พบโปรแกรมต้นฉบับ", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * ลบโปรแกรม
     */
    private fun showDeleteConfirmDialog(program: TrainingProgramModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ ลบโปรแกรม")
            .setMessage("คุณต้องการลบโปรแกรม ${program.programName} หรือไม่?\n\n⚠️ มีผู้ใช้ ${program.activeUsers} คนกำลังใช้โปรแกรมนี้อยู่\n\nการลบจะส่งผลต่อผู้ใช้ทั้งหมด!")
            .setPositiveButton("ลบ") { _, _ ->
                deleteProgram(program)
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    /**
     * ลบโปรแกรม
     */
    private fun deleteProgram(program: TrainingProgramModel) {
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("training_plans")
            .document(program.programId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "ลบโปรแกรมสำเร็จ", Toast.LENGTH_SHORT).show()
                loadTrainingPlans()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete program", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "ไม่สามารถลบได้", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * ไปหน้าแก้ไขโปรแกรม
     */
    private fun navigateToEditProgram(program: TrainingProgramModel) {
        val fragment = EditTrainingPlanFragment.newInstance(program.programId)
        (activity as? MainActivity)?.replaceFragment(fragment)
    }

    /**
     * ไปหน้าดูตารางโปรแกรม
     */
    private fun navigateToViewProgram(program: TrainingProgramModel) {
        val fragment = ViewTrainingPlanFragment.newInstance(program.programId)
        (activity as? MainActivity)?.replaceFragment(fragment)
    }

    private fun getDayName(day: Int): String {
        return when (day) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}