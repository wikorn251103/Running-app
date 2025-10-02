package com.example.myproject.Fragment.detailprogram

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.myproject.Fragment.training.TrainingScheduleFragment
import com.example.myproject.MainActivity
import com.example.myproject.R
import com.example.myproject.databinding.FragmentRunningGoal5kBinding


class RunningGoal5kFragment : Fragment() {

    private var _binding: FragmentRunningGoal5kBinding? = null
    private val binding get() = _binding!!

    private val timeOptions = listOf("20:00", "22:30", "25:00", "27:30", "30:00", "35:00")
    private var selectedTime: String = ""

    // เพิ่ม SharedPreferences
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
        // ใช้ binding โดยตรง
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

            // บันทึกว่าเลือกโปรแกรมแล้ว
            saveProgramSelection(trainingPlanId, "5 กิโลเมตร - $time")

            Log.d(TAG, "Saved program: $trainingPlanId")

            // ไปหน้าตารางซ้อม
            val fragment = TrainingScheduleFragment.newInstance(trainingPlanId)
            (activity as? MainActivity)?.replaceFragment(fragment)

            Toast.makeText(requireContext(), "เริ่มโปรแกรมด้วยเวลา $time", Toast.LENGTH_SHORT).show()
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

    /**
     * บันทึกโปรแกรมที่เลือกลง SharedPreferences
     */
    private fun saveProgramSelection(programName: String, displayName: String) {
        sharedPreferences.edit().apply {
            putBoolean("program_selected", true)
            putString("selected_program_name", programName)
            putString("selected_program_display_name", displayName)
            putString("selected_sub_program_name", "โปรแกรมย่อย 5K")
            apply()
        }
        Log.d(TAG, "Program saved to SharedPreferences: $programName")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}