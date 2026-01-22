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
import com.example.myproject.databinding.FragmentNewbieDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
            .setMessage("คุณต้องการเปลี่ยนไปใช้โปรแกรมมือใหม่หรือไม่?\n\nโปรแกรมเก่าจะถูกปิดและความก้าวหน้าจะถูกรีเซ็ต\n\n⚠️ หมายเหตุ: โปรแกรมมือใหม่เป็นโปรแกรมดูอย่างเดียว ไม่สามารถบันทึกการซ้อมได้")
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
                    trainingData["isViewOnly"] = true // ✅ โปรแกรมมือใหม่ = ดูได้อย่างเดียว ไม่บันทึก
                    trainingData["startDate"] = currentTime
                    trainingData["createdAt"] = currentTime
                    trainingData["updatedAt"] = currentTime

                    // ลบฟิลด์ที่ไม่จำเป็น
                    trainingData.remove("currentProgramId")
                    trainingData.remove("lastUpdated")

                    // ✅ รีเซ็ต isCompleted และ isMissed ทุกวัน (เพื่อให้เป็นตารางสะอาด)
                    resetWeeksDaysStatus(trainingData)

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
     *  รีเซ็ต isCompleted และ isMissed ของทุกวันในโปรแกรม
     * (ทำให้ตารางสะอาด เพื่อให้ดูง่าย แต่ไม่มีการบันทึกการซ้อมจริง)
     */
    private fun resetWeeksDaysStatus(trainingData: MutableMap<String, Any>) {
        try {
            // ✅ ลองทั้ง 2 รูปแบบ: "weeks" object หรือ "week_1", "week_2" แยกกัน
            val weeksData = trainingData["weeks"] as? Map<*, *>

            if (weeksData != null) {
                // ✅ กรณีมี "weeks" object
                val updatedWeeks = mutableMapOf<String, Any>()
                weeksData.forEach { (weekKey, weekValue) ->
                    val weekData = weekValue as? Map<*, *> ?: return@forEach
                    val updatedDays = mutableMapOf<String, Any>()

                    weekData.forEach { (dayKey, dayValue) ->
                        val dayMap = (dayValue as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                        dayMap["isCompleted"] = false
                        dayMap["isMissed"] = false
                        updatedDays[dayKey.toString()] = dayMap
                    }

                    updatedWeeks[weekKey.toString()] = updatedDays
                }
                trainingData["weeks"] = updatedWeeks
                Log.d(TAG, "✅ Reset status using 'weeks' structure")
            } else {
                // ✅ กรณีใช้ "week_1", "week_2", "week_3", "week_4" แยกกัน
                for (weekNum in 1..4) {
                    val weekKey = "week_$weekNum"
                    val weekData = trainingData[weekKey] as? Map<*, *>

                    if (weekData != null) {
                        val updatedDays = mutableMapOf<String, Any>()
                        weekData.forEach { (dayKey, dayValue) ->
                            val dayMap = (dayValue as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                            dayMap["isCompleted"] = false
                            dayMap["isMissed"] = false
                            updatedDays[dayKey.toString()] = dayMap
                        }
                        trainingData[weekKey] = updatedDays
                    }
                }
                Log.d(TAG, "✅ Reset status using 'week_X' structure")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error resetting weeks status", e)
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

                Toast.makeText(requireContext(), "เริ่มโปรแกรมสำเร็จ (โหมดดูอย่างเดียว)", Toast.LENGTH_SHORT).show()

                // ✅ กลับไปหน้าก่อนหน้า
                returnToPreviousScreen()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to save to Athletes collection", e)
                binding.backBtnRookie.isEnabled = true
                binding.backBtnRookie.text = "เริ่มโปรแกรม"
                Toast.makeText(requireContext(), "ไม่สามารถบันทึกได้: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     *  กลับไปหน้าก่อนหน้า (แบบเดียวกับ RunningGoal5kFragment)
     */
    private fun returnToPreviousScreen() {
        try {
            Log.d(TAG, "🔙 Returning to previous screen...")

            val fragmentManager = activity?.supportFragmentManager

            if (fragmentManager != null) {
                val backStackCount = fragmentManager.backStackEntryCount


                //  Pop back stack 2 ครั้ง (เหมือน RunningGoal5kFragment)
                if (backStackCount >= 2) {
                    fragmentManager.popBackStack()
                    fragmentManager.popBackStack()

                } else if (backStackCount > 0) {
                    // Pop ทั้งหมดที่มี
                    fragmentManager.popBackStack(
                        null,
                        androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )

                } else {
                    Log.w(TAG, "⚠️ No back stack to pop")
                }

                Log.d(TAG, "✅ Returned successfully")
            } else {
                Log.e(TAG, "❌ FragmentManager is null")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error returning to previous screen", e)
            e.printStackTrace()

            //  Fallback: ลองปิด fragment ปัจจุบัน
            try {
                activity?.supportFragmentManager?.popBackStack()
            } catch (fallbackError: Exception) {
                Log.e(TAG, "❌ Fallback also failed", fallbackError)
            }
        }
    }

    /**
     *  บันทึกโปรแกรมลง Local Storage (แยกเก็บว่าเป็นโหมด View Only)
     */
    private fun saveProgramToLocal(startDate: Long) {
        sharedPreferences.edit().apply {
            putBoolean("program_selected", true)
            putString("selected_program_name", PROGRAM_ID)
            putString("selected_program_display_name", DISPLAY_NAME)
            putString("selected_sub_program_name", SUB_PROGRAM_NAME)
            putBoolean("is_view_only_program", true) //  โปรแกรมมือใหม่ = ดูอย่างเดียว ไม่บันทึก
            putLong("program_start_date", startDate)
            putLong("selected_at", System.currentTimeMillis())
            apply()
        }
        Log.d(TAG, "✅ Program saved to Local Storage: $PROGRAM_ID (View Only Mode - No Recording)")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}