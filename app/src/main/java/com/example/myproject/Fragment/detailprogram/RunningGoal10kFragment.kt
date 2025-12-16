package com.example.myproject.Fragment.detailprogram

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.myproject.R
import com.example.myproject.databinding.FragmentRunningGoal10kBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class RunningGoal10kFragment : Fragment() {

    private var _binding: FragmentRunningGoal10kBinding? = null
    private val binding get() = _binding!!

    private val timeOptions = listOf("45:00", "50:00", "60:00", "70:00")
    private var selectedTime: String = ""

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val sharedPreferences by lazy {
        requireContext().getSharedPreferences("running_app_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        fun newInstance() = RunningGoal10kFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRunningGoal10kBinding.inflate(inflater, container, false)
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
            binding.button1,    // 45:00
            binding.button2,    // 50:00
            binding.button3,    // 60:00
            binding.button4     // 70:00
        )

        timeButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectedTime = timeOptions[index]
                binding.editTextTime.setText(selectedTime)
                updateButtonStates(timeButtons, index)
            }
        }
    }

    private fun updateButtonStates(buttons: List<View>, selectedIndex: Int) {
        buttons.forEachIndexed { i, btn ->
            if (i == selectedIndex) {
                btn.setBackgroundResource(R.drawable.txt_bg)
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
            val displayName = "10 กิโลเมตร - $time"

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
            "45:00" -> "10k_sub45"
            "50:00" -> "10k_sub50"
            "60:00" -> "10k_sub60"
            "70:00" -> "10k_sub70"
            else -> "10k_sub60" // default
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

                    val currentTime = System.currentTimeMillis()

                    trainingData["userId"] = userId
                    trainingData["programId"] = programId
                    trainingData["programDisplayName"] = displayName
                    trainingData["subProgramName"] = "โปรแกรมย่อย 10K"
                    trainingData["isActive"] = true
                    trainingData["startDate"] = currentTime
                    trainingData["createdAt"] = currentTime
                    trainingData["updatedAt"] = currentTime

                    trainingData.remove("currentProgramId")
                    trainingData.remove("lastUpdated")

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

                    saveToAthletesCollectionAndMarkMissed(userId, trainingData, programId, displayName, currentTime)
                } else {
                    binding.startProgramBtn.isEnabled = true
                    binding.startProgramBtn.text = "เริ่มโปรแกรม"
                    Toast.makeText(requireContext(), "ไม่พบข้อมูลโปรแกรม $programId", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                binding.startProgramBtn.isEnabled = true
                binding.startProgramBtn.text = "เริ่มโปรแกรม"
                Toast.makeText(requireContext(), "เกิดข้อผิดพลาดในการดึงข้อมูล: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveToAthletesCollectionAndMarkMissed(
        userId: String,
        trainingData: MutableMap<String, Any>,
        programId: String,
        displayName: String,
        startDate: Long
    ) {
        firestore.collection("Athletes")
            .document(userId)
            .set(trainingData)
            .addOnSuccessListener {
                markMissedDaysBeforeStart(userId, startDate)

                saveProgramToLocal(programId, displayName, startDate)

                binding.startProgramBtn.isEnabled = true
                binding.startProgramBtn.text = "เริ่มโปรแกรม"

                Toast.makeText(requireContext(), "เริ่มโปรแกรมสำเร็จ", Toast.LENGTH_SHORT).show()

                activity?.supportFragmentManager?.popBackStack()
                activity?.supportFragmentManager?.popBackStack()
            }
            .addOnFailureListener { e ->
                binding.startProgramBtn.isEnabled = true
                binding.startProgramBtn.text = "เริ่มโปรแกรม"
                Toast.makeText(requireContext(), "ไม่สามารถบันทึกได้: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun markMissedDaysBeforeStart(userId: String, startDate: Long) {
        val startCalendar = Calendar.getInstance().apply {
            timeInMillis = startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startDayOfWeek = startCalendar.get(Calendar.DAY_OF_WEEK)
        val programStartDay = when (startDayOfWeek) {
            Calendar.SUNDAY -> 7
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 1
        }

        if (programStartDay > 1) {
            val updates = mutableMapOf<String, Any>()

            for (day in 1 until programStartDay) {
                val fieldPath = "weeks.week_1.day_$day.isMissed"
                updates[fieldPath] = true
            }

            if (updates.isNotEmpty()) {
                firestore.collection("Athletes")
                    .document(userId)
                    .update(updates)
            }
        }
    }

    private fun saveProgramToLocal(programName: String, displayName: String, startDate: Long) {
        sharedPreferences.edit().apply {
            putBoolean("program_selected", true)
            putString("selected_program_name", programName)
            putString("selected_program_display_name", displayName)
            putString("selected_sub_program_name", "โปรแกรมย่อย 10K")
            putLong("program_start_date", startDate)
            putLong("selected_at", System.currentTimeMillis())
            apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}