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
import androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
import com.example.myproject.Fragment.training.TrainingScheduleFragment
import com.example.myproject.MainActivity
import com.example.myproject.MainFragment
import com.example.myproject.R
import com.example.myproject.databinding.FragmentRunningGoal5kBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class RunningGoal5kFragment : Fragment() {

    private var _binding: FragmentRunningGoal5kBinding? = null
    private val binding get() = _binding!!

    private val timeOptions = listOf("20:00", "22:30", "25:00", "27:30", "30:00", "35:00")
    private var selectedTime: String = ""

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val sharedPreferences by lazy {
        requireContext().getSharedPreferences("running_app_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "RunningGoal5k"
        fun newInstance() = RunningGoal5kFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRunningGoal5kBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTimeButtons()
        setupStartButton()
        setupBackButton()
    }

    private fun setupTimeButtons() {
        val timeButtons = listOf(
            binding.button1,
            binding.button2,
            binding.button3,
            binding.button4,
            binding.button5,
            binding.button6
        )

        timeButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectedTime = timeOptions[index]
                binding.editTextTime.setText(selectedTime)
                updateButtonStates(timeButtons, index)
                Log.d(TAG, "Selected time: $selectedTime")
            }
        }
    }

    private fun updateButtonStates(buttons: List<View>, selectedIndex: Int) {
        buttons.forEachIndexed { i, btn ->
            if (i == selectedIndex) {
                btn.setBackgroundResource(R.drawable.time_button_selected_bg)
                (btn as? androidx.appcompat.widget.AppCompatButton)?.setTextColor(
                    resources.getColor(R.color.white, null)
                )
            } else {
                btn.setBackgroundResource(R.drawable.white_bg)
                (btn as? androidx.appcompat.widget.AppCompatButton)?.setTextColor(
                    resources.getColor(R.color.black, null)
                )
            }
        }
    }

    private fun setupStartButton() {
        binding.startProgramBtn.setOnClickListener {
            val time = binding.editTextTime.text.toString().trim()

            if (time.isBlank()) {
                Toast.makeText(requireContext(), "กรุณาเลือกหรือกรอกเวลา", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val trainingPlanId = getTrainingPlanId(time)
            val displayName = "5 กิโลเมตร - $time"

            checkExistingProgram(trainingPlanId, displayName)
        }
    }

    private fun setupBackButton() {
        binding.backBtn.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }
    }

    private fun getTrainingPlanId(time: String): String {
        return when (time) {
            "20:00" -> "5k_sub20"
            "22:30" -> "5k_sub22_30"
            "25:00" -> "5k_sub25"
            "27:30" -> "5k_sub27_30"
            "30:00" -> "5k_sub30"
            "35:00" -> "5k_sub35"
            else -> "5k_sub25"
        }
    }

    private fun checkExistingProgram(newProgramId: String, newDisplayName: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "กรุณา Login ก่อน", Toast.LENGTH_SHORT).show()
            return
        }

        binding.startProgramBtn.isEnabled = false
        binding.startProgramBtn.text = "กำลังตรวจสอบ..."

        firestore.collection("Athletes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // ⭐ เปลี่ยนจาก currentProgramId เป็น programId
                    val currentProgramId = document.getString("programId")
                    val isActive = document.getBoolean("isActive") ?: false

                    if (isActive && !currentProgramId.isNullOrEmpty()) {
                        showReplaceConfirmDialog(currentProgramId, newProgramId, newDisplayName)
                    } else {
                        startCreatingProgram(newProgramId, newDisplayName)
                    }
                } else {
                    startCreatingProgram(newProgramId, newDisplayName)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to check existing program", e)
                binding.startProgramBtn.isEnabled = true
                binding.startProgramBtn.text = "เริ่มโปรแกรม"
                Toast.makeText(requireContext(), "เกิดข้อผิดพลาด กรุณาลองใหม่", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showReplaceConfirmDialog(oldProgramId: String, newProgramId: String, newDisplayName: String) {
        binding.startProgramBtn.isEnabled = true
        binding.startProgramBtn.text = "เริ่มโปรแกรม"

        AlertDialog.Builder(requireContext())
            .setTitle("คุณมีโปรแกรมอยู่แล้ว")
            .setMessage("คุณต้องการเปลี่ยนไปใช้โปรแกรมใหม่หรือไม่?\n\nโปรแกรมเก่าจะถูกปิดและความก้าวหน้าจะถูกรีเซ็ต")
            .setPositiveButton("เปลี่ยนโปรแกรม") { _, _ ->
                startCreatingProgram(newProgramId, newDisplayName)
            }
            .setNegativeButton("ยกเลิก") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(requireContext(), "ยกเลิกการเปลี่ยนโปรแกรม", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun startCreatingProgram(programId: String, displayName: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "กรุณา Login ก่อน", Toast.LENGTH_SHORT).show()
            return
        }

        binding.startProgramBtn.isEnabled = false
        binding.startProgramBtn.text = "กำลังสร้างโปรแกรม..."

        firestore.collection("training_plans")
            .document(programId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val trainingData = document.data?.toMutableMap() ?: mutableMapOf()

                    // ปรับโครงสร้างให้สอดคล้องกับระบบบันทึกการซ้อม
                    trainingData["userId"] = userId
                    trainingData["programId"] = programId
                    trainingData["programDisplayName"] = displayName
                    trainingData["subProgramName"] = "โปรแกรมย่อย 5K" // เพิ่มชื่อโปรแกรมย่อย
                    trainingData["isActive"] = true
                    trainingData["createdAt"] = System.currentTimeMillis()
                    trainingData["updatedAt"] = System.currentTimeMillis()

                    // ลบ field ที่ไม่จำเป็น
                    trainingData.remove("currentProgramId") // ใช้ programId แทน
                    trainingData.remove("lastUpdated") // ใช้ updatedAt แทน

                    // เพิ่ม isCompleted = false ให้ทุกวันในตาราง
                    val weeks = trainingData["weeks"] as? Map<*, *>
                    if (weeks != null) {
                        val updatedWeeks = mutableMapOf<String, Any>()
                        weeks.forEach { (weekKey, weekData) ->
                            val days = (weekData as? Map<*, *>) ?: emptyMap<String, Any>()
                            val updatedDays = mutableMapOf<String, Any>()

                            days.forEach { (dayKey, dayData) ->
                                val dayMap = (dayData as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                                dayMap["isCompleted"] = false // เพิ่ม isCompleted
                                dayMap["isMissed"] = false // เพิ่มบรรทัดนี้ - ขาดซ้อมหรือยัง
                                updatedDays[dayKey.toString()] = dayMap
                            }

                            updatedWeeks[weekKey.toString()] = updatedDays
                        }
                        trainingData["weeks"] = updatedWeeks
                    }

                    saveToAthletesCollection(userId, trainingData, programId, displayName)
                } else {
                    binding.startProgramBtn.isEnabled = true
                    binding.startProgramBtn.text = "เริ่มโปรแกรม"
                    Toast.makeText(requireContext(), "ไม่พบข้อมูลโปรแกรม $programId", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch training plan", e)
                binding.startProgramBtn.isEnabled = true
                binding.startProgramBtn.text = "เริ่มโปรแกรม"
                Toast.makeText(requireContext(), "เกิดข้อผิดพลาดในการดึงข้อมูล: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveToAthletesCollection(
        userId: String,
        trainingData: MutableMap<String, Any>,
        programId: String,
        displayName: String
    ) {
        firestore.collection("Athletes")
            .document(userId)
            .set(trainingData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Training program saved to Athletes/$userId successfully")

                saveProgramToLocal(programId, displayName)

                binding.startProgramBtn.isEnabled = true
                binding.startProgramBtn.text = "เริ่มโปรแกรม"

                Toast.makeText(requireContext(), "เริ่มโปรแกรมสำเร็จ", Toast.LENGTH_SHORT).show()

//                val fragment = TrainingScheduleFragment.newInstance(programId)
//                (activity as? MainActivity)?.replaceFragment(fragment)

                activity?.supportFragmentManager?.popBackStack()
                activity?.supportFragmentManager?.popBackStack()

            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to save to Athletes collection", e)
                binding.startProgramBtn.isEnabled = true
                binding.startProgramBtn.text = "เริ่มโปรแกรม"
                Toast.makeText(requireContext(), "ไม่สามารถบันทึกได้: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveProgramToLocal(programName: String, displayName: String) {
        sharedPreferences.edit().apply {
            putBoolean("program_selected", true)
            putString("selected_program_name", programName)
            putString("selected_program_display_name", displayName)
            putString("selected_sub_program_name", "โปรแกรมย่อย 5K")
            putLong("selected_at", System.currentTimeMillis())
            apply()
        }
        Log.d(TAG, "Program saved to Local Storage: $programName")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}