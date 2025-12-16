package com.example.myproject.Fragment.detailprogram

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.myproject.Fragment.home.HomeFragment
import com.example.myproject.MainActivity
import com.example.myproject.MainFragment
import com.example.myproject.databinding.FragmentNewbieDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class NewbieDetailsFragment : Fragment() {

    private var _binding: FragmentNewbieDetailsBinding? = null
    private val binding get() = _binding!!

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val sharedPreferences by lazy {
        requireContext().getSharedPreferences("running_app_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "NewbieDetailsFragment"
        private const val PROGRAM_ID = "beginner"
        private const val DISPLAY_NAME = "โปรแกรมมือใหม่ - เริ่มต้นฝึกวิ่ง"
        private const val SUB_PROGRAM_NAME = "โปรแกรมสำหรับผู้เริ่มต้น"

        fun newInstance() = NewbieDetailsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewbieDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        // ปุ่มย้อนกลับ
        binding.startBtn1.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // ปุ่มเริ่มโปรแกรม
        binding.backBtnRookie.setOnClickListener {
            checkExistingProgram()
        }
    }

    /**
     * ✅ เช็คว่ามีโปรแกรมอยู่แล้วหรือไม่
     */
    private fun checkExistingProgram() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "กรุณา Login ก่อน", Toast.LENGTH_SHORT).show()
            return
        }

        binding.backBtnRookie.isEnabled = false
        binding.backBtnRookie.text = "กำลังตรวจสอบ..."

        firestore.collection("Athletes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentProgramId = document.getString("programId")
                    val isActive = document.getBoolean("isActive") ?: false

                    if (isActive && !currentProgramId.isNullOrEmpty()) {
                        showReplaceConfirmDialog()
                    } else {
                        startCreatingProgram()
                    }
                } else {
                    startCreatingProgram()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to check existing program", e)
                binding.backBtnRookie.isEnabled = true
                binding.backBtnRookie.text = "เริ่มโปรแกรม"
                Toast.makeText(requireContext(), "เกิดข้อผิดพลาด กรุณาลองใหม่", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * ✅ แสดง Dialog ถามว่าจะเปลี่ยนโปรแกรมหรือไม่
     */
    private fun showReplaceConfirmDialog() {
        binding.backBtnRookie.isEnabled = true
        binding.backBtnRookie.text = "เริ่มโปรแกรม"

        AlertDialog.Builder(requireContext())
            .setTitle("คุณมีโปรแกรมอยู่แล้ว")
            .setMessage("คุณต้องการเปลี่ยนไปใช้โปรแกรมมือใหม่หรือไม่?\n\nโปรแกรมเก่าจะถูกปิดและความก้าวหน้าจะถูกรีเซ็ต")
            .setPositiveButton("เปลี่ยนโปรแกรม") { _, _ ->
                startCreatingProgram()
            }
            .setNegativeButton("ยกเลิก") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(requireContext(), "ยกเลิกการเปลี่ยนโปรแกรม", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * ✅ ดึงข้อมูลโปรแกรมจาก Firebase และสร้างโปรแกรมใหม่
     */
    private fun startCreatingProgram() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "กรุณา Login ก่อน", Toast.LENGTH_SHORT).show()
            return
        }

        binding.backBtnRookie.isEnabled = false
        binding.backBtnRookie.text = "กำลังสร้างโปรแกรม..."

        // ดึงข้อมูลจาก training_plans/beginner
        firestore.collection("training_plans")
            .document(PROGRAM_ID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val trainingData = document.data?.toMutableMap() ?: mutableMapOf()

                    // เพิ่มข้อมูลผู้ใช้
                    val currentTime = System.currentTimeMillis()
                    trainingData["userId"] = userId
                    trainingData["programId"] = PROGRAM_ID
                    trainingData["programDisplayName"] = DISPLAY_NAME
                    trainingData["subProgramName"] = SUB_PROGRAM_NAME
                    trainingData["isActive"] = true
                    trainingData["isViewOnly"] = true // ✅ โปรแกรมมือใหม่ = ดูได้อย่างเดียว
                    trainingData["startDate"] = currentTime
                    trainingData["createdAt"] = currentTime
                    trainingData["updatedAt"] = currentTime

                    // ลบฟิลด์ที่ไม่จำเป็น
                    trainingData.remove("currentProgramId")
                    trainingData.remove("lastUpdated")

                    // รีเซ็ต isCompleted และ isMissed ทุกวัน
                    val weeks = trainingData["weeks"] as? Map<*, *>
                    if (weeks != null) {
                        val updatedWeeks = mutableMapOf<String, Any>()
                        weeks.forEach { (weekKey, weekData) ->
                            val days = (weekData as? Map<*, *>) ?: emptyMap<String, Any>()
                            val updatedDays = mutableMapOf<String, Any>()

                            days.forEach { (dayKey, dayData) ->
                                val dayMap = (dayData as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                                dayMap["isCompleted"] = false
                                dayMap["isMissed"] = false
                                updatedDays[dayKey.toString()] = dayMap
                            }

                            updatedWeeks[weekKey.toString()] = updatedDays
                        }
                        trainingData["weeks"] = updatedWeeks
                    }

                    // บันทึกลง Firebase Athletes collection
                    saveToAthletesCollection(userId, trainingData, currentTime)
                } else {
                    binding.backBtnRookie.isEnabled = true
                    binding.backBtnRookie.text = "เริ่มโปรแกรม"
                    Toast.makeText(requireContext(), "ไม่พบข้อมูลโปรแกรม", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "❌ Program document 'beginner' not found in training_plans")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch training plan", e)
                binding.backBtnRookie.isEnabled = true
                binding.backBtnRookie.text = "เริ่มโปรแกรม"
                Toast.makeText(requireContext(), "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * ✅ บันทึกโปรแกรมลง Athletes collection
     */
    private fun saveToAthletesCollection(
        userId: String,
        trainingData: MutableMap<String, Any>,
        startDate: Long
    ) {
        firestore.collection("Athletes")
            .document(userId)
            .set(trainingData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Newbie program saved to Athletes/$userId successfully")

                // บันทึกลง Local Storage
                saveProgramToLocal(startDate)

                binding.backBtnRookie.isEnabled = true
                binding.backBtnRookie.text = "เริ่มโปรแกรม"

                Toast.makeText(requireContext(), "เริ่มโปรแกรมสำเร็จ", Toast.LENGTH_SHORT).show()

                // ✅ กลับไป MainFragment (พร้อม BottomNavigation)
                returnToMainFragment()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to save to Athletes collection", e)
                binding.backBtnRookie.isEnabled = true
                binding.backBtnRookie.text = "เริ่มโปรแกรม"
                Toast.makeText(requireContext(), "ไม่สามารถบันทึกได้: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * ✅ กลับไป MainFragment (แก้ปัญหา NullPointerException)
     */
    private fun returnToMainFragment() {
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            // ล้าง back stack ทั้งหมด
            mainActivity.supportFragmentManager.popBackStack(
                null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
            )

            // กลับไป MainFragment ใหม่เลย (รับประกันว่า BottomNavigation จะแสดง)
            mainActivity.replaceFragment(
                MainFragment.newInstance(),
                addToBackStack = false,
                tag = MainFragment.TAG
            )

            Log.d(TAG, "✅ Returned to MainFragment successfully")
        } else {
            Log.e(TAG, "❌ MainActivity is null, cannot return")
        }
    }

    /**
     * ✅ บันทึกโปรแกรมลง Local Storage
     */
    private fun saveProgramToLocal(startDate: Long) {
        sharedPreferences.edit().apply {
            putBoolean("program_selected", true)
            putString("selected_program_name", PROGRAM_ID)
            putString("selected_program_display_name", DISPLAY_NAME)
            putString("selected_sub_program_name", SUB_PROGRAM_NAME)
            putBoolean("is_view_only_program", true) // ✅ โปรแกรมมือใหม่ = ดูอย่างเดียว
            putLong("program_start_date", startDate)
            putLong("selected_at", System.currentTimeMillis())
            apply()
        }
        Log.d(TAG, "✅ Program saved to Local Storage: $PROGRAM_ID (View Only Mode)")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}