package com.example.myproject.Fragment.workout

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myproject.MainFragment
import com.example.myproject.data.training.TrainingModel
import com.example.myproject.databinding.FragmentRecordWorkoutBinding
import com.google.android.material.chip.Chip

class RecordWorkoutFragment : Fragment() {

    private var _binding: FragmentRecordWorkoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecordWorkoutViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return RecordWorkoutViewModel() as T
            }
        }
    }

    private var trainingData: TrainingModel? = null
    private var weekNumber: Int = 0
    private var dayNumber: Int = 0

    companion object {
        private const val ARG_TRAINING_DATA = "training_data"
        private const val ARG_WEEK_NUMBER = "week_number"
        private const val ARG_DAY_NUMBER = "day_number"
        const val REQUEST_KEY = "workout_saved"
        const val RESULT_WEEK_NUMBER = "week_number"

        fun newInstance(
            trainingData: TrainingModel,
            weekNumber: Int,
            dayNumber: Int
        ): RecordWorkoutFragment {
            val fragment = RecordWorkoutFragment()
            val bundle = Bundle().apply {
                putParcelable(ARG_TRAINING_DATA, trainingData)
                putInt(ARG_WEEK_NUMBER, weekNumber)
                putInt(ARG_DAY_NUMBER, dayNumber)
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordWorkoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trainingData = arguments?.getParcelable(ARG_TRAINING_DATA)
        weekNumber = arguments?.getInt(ARG_WEEK_NUMBER) ?: 0
        dayNumber = arguments?.getInt(ARG_DAY_NUMBER) ?: 0

        setupUI()
        setupClickListeners()
        observeViewModel()

        // เพิ่ม: ซ่อน BottomNavigation
        hideBottomNavigation()
    }

    private fun setupUI() {
        trainingData?.let { data ->
            binding.tvTrainingType.text = data.type
            binding.tvTrainingDescription.text = data.description
            binding.tvPlannedPace.text = "เป้าหมาย: ${data.pace}"
            binding.tvWeekDay.text = "สัปดาห์ที่ $weekNumber - วันที่ $dayNumber"
        }

        setupFeelingChips()
    }

    private fun setupFeelingChips() {
        val feelings = listOf("Great", "Good", "Okay", "Tired", "Struggling")

        feelings.forEach { feeling ->
            val chip = Chip(requireContext()).apply {
                text = feeling
                isCheckable = true
                setOnClickListener {
                    viewModel.setFeeling(feeling)
                }
            }
            binding.chipGroupFeeling.addView(chip)
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveWorkout.setOnClickListener {
            saveWorkout()
        }

        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun saveWorkout() {
        val distanceStr = binding.etDistance.text.toString()
        val hoursStr = binding.etHours.text.toString()
        val minutesStr = binding.etMinutes.text.toString()
        val secondsStr = binding.etSeconds.text.toString()
        val heartRateStr = binding.etHeartRate.text.toString()
        val notes = binding.etNotes.text.toString()

        if (distanceStr.isEmpty()) {
            Toast.makeText(requireContext(), "กรุณากรอกระยะทาง", Toast.LENGTH_SHORT).show()
            return
        }

        if (hoursStr.isEmpty() && minutesStr.isEmpty() && secondsStr.isEmpty()) {
            Toast.makeText(requireContext(), "กรุณากรอกเวลาอย่างน้อย 1 ช่อง", Toast.LENGTH_SHORT).show()
            return
        }

        val distance = distanceStr.toDoubleOrNull() ?: 0.0

        val hours = hoursStr.toLongOrNull() ?: 0L
        val minutes = minutesStr.toLongOrNull() ?: 0L
        val seconds = secondsStr.toLongOrNull() ?: 0L
        val duration = hours * 3600 + minutes * 60 + seconds

        if (duration == 0L) {
            Toast.makeText(requireContext(), "กรุณากรอกเวลาให้ถูกต้อง", Toast.LENGTH_SHORT).show()
            return
        }

        val heartRate = heartRateStr.toIntOrNull() ?: 0

        trainingData?.let { data ->
            viewModel.saveWorkout(
                trainingData = data,
                weekNumber = weekNumber,
                dayNumber = dayNumber,
                distance = distance,
                duration = duration,
                calories = 0,
                heartRate = heartRate,
                notes = notes
            )
        }
    }

    private fun observeViewModel() {
        viewModel.isSaving.observe(viewLifecycleOwner) { isSaving ->
            binding.progressBar.visibility = if (isSaving) View.VISIBLE else View.GONE
            binding.btnSaveWorkout.isEnabled = !isSaving
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "บันทึกการซ้อมเรียบร้อย", Toast.LENGTH_SHORT).show()

                // ส่งสัญญาณไปที่ MainFragment
                val result = Bundle().apply {
                    putInt("week_number", weekNumber)
                }

                parentFragmentManager.setFragmentResult("workout_saved_return_to_schedule", result)

                // กลับไปหน้าเดิม
                parentFragmentManager.popBackStack()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    // เพิ่ม: ซ่อน BottomNavigation
    private fun hideBottomNavigation() {
        val mainFragment = parentFragment as? MainFragment
        mainFragment?.setBottomNavVisible(false)
    }

    // เพิ่ม: แสดง BottomNavigation กลับ
    private fun showBottomNavigation() {
        val mainFragment = parentFragment as? MainFragment
        mainFragment?.setBottomNavVisible(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // แสดง BottomNavigation กลับเมื่อออกจากหน้านี้
        showBottomNavigation()

        _binding = null
    }
}