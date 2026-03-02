package com.example.myproject.Fragment.workout

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myproject.MainFragment
import com.example.myproject.R
import com.example.myproject.data.training.TrainingModel
import com.example.myproject.databinding.FragmentRecordWorkoutBinding

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

    // ✅ เก็บผลคำนวณ Pace
    private var currentPaceResult = ""
    private var currentPaceDiff = 0

    companion object {
        private const val ARG_TRAINING_DATA = "training_data"
        private const val ARG_WEEK_NUMBER = "week_number"
        private const val ARG_DAY_NUMBER = "day_number"
        const val REQUEST_KEY = "workout_saved"
        const val RESULT_WEEK_NUMBER = "week_number"
        private const val RUNNING_TIPS_URL = "https://www.youtube.com/watch?v=YOUR_VIDEO_ID"

        fun newInstance(
            trainingData: TrainingModel,
            weekNumber: Int,
            dayNumber: Int
        ): RecordWorkoutFragment {
            return RecordWorkoutFragment().also {
                it.arguments = Bundle().apply {
                    putParcelable(ARG_TRAINING_DATA, trainingData)
                    putInt(ARG_WEEK_NUMBER, weekNumber)
                    putInt(ARG_DAY_NUMBER, dayNumber)
                }
            }
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
        setupPaceCalculation()  // ✅ TextWatcher ทุกช่อง
        setupClickListeners()
        observeViewModel()
        hideBottomNavigation()
    }

    // ==================== UI ====================

    private fun setupUI() {
        trainingData?.let {
            binding.tvTrainingType.text = it.type ?: ""
            binding.tvTrainingDescription.text = it.description ?: ""
            binding.tvPlannedPace.text = "เป้าหมาย: ${it.pace ?: "-"}"
            binding.tvWeekDay.text = "สัปดาห์ที่ $weekNumber - วันที่ $dayNumber"
        }
    }

    // ==================== PACE CALCULATION ====================

    // ✅ ติด TextWatcher ทุกช่องที่เกี่ยวกับการคำนวณ
    private fun setupPaceCalculation() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateAndShowPaceFeedback()
            }
        }
        binding.etDistance.addTextChangedListener(watcher)
        binding.etHours.addTextChangedListener(watcher)
        binding.etMinutes.addTextChangedListener(watcher)
        binding.etSeconds.addTextChangedListener(watcher)
    }

    private fun calculateAndShowPaceFeedback() {
        val distance = binding.etDistance.text.toString().toDoubleOrNull()
        val hours = binding.etHours.text.toString().toLongOrNull() ?: 0L
        val minutes = binding.etMinutes.text.toString().toLongOrNull() ?: 0L
        val seconds = binding.etSeconds.text.toString().toLongOrNull() ?: 0L
        val totalSeconds = hours * 3600 + minutes * 60 + seconds

        // ต้องมีทั้งระยะทางและเวลาจึงจะคำนวณได้
        if (distance == null || distance <= 0 || totalSeconds <= 0) {
            binding.tvActualPace.visibility = View.GONE
            binding.cardPaceFeedback.visibility = View.GONE
            return
        }

        // คำนวณเพซจริง
        val actualPaceSeconds = (totalSeconds / distance).toInt()
        val actualPaceStr = String.format(
            "%d:%02d",
            actualPaceSeconds / 60,
            actualPaceSeconds % 60
        )

        binding.tvActualPace.text = "⚡ เพซของคุณ: $actualPaceStr /กม."
        binding.tvActualPace.visibility = View.VISIBLE

        // เปรียบเทียบกับเป้าหมายจาก Firebase
        val plannedSeconds = parsePaceToSeconds(trainingData?.pace ?: "")
        if (plannedSeconds > 0) {
            showPaceFeedback(actualPaceSeconds, plannedSeconds)
        }
    }

    /**
     * ✅ รองรับทุก format จาก Firebase:
     * "5:43 ~ 6:17 (E)"
     * "6:30-7:00/km"
     * "6:30/km"
     * "6:30"
     */
    private fun parsePaceToSeconds(paceStr: String): Int {
        if (paceStr.isBlank()) return 0
        val cleaned = paceStr
            .replace(Regex("(?i)/km|/กม\\.|\\([^)]*\\)"), "")
            .trim()
        return try {
            when {
                cleaned.contains("~") -> {
                    val parts = cleaned.split("~")
                    val min = parseSinglePace(parts[0].trim())
                    val max = parseSinglePace(parts[1].trim())
                    if (min > 0 && max > 0) (min + max) / 2 else 0
                }
                cleaned.contains("-") -> {
                    val parts = cleaned.split("-")
                    val min = parseSinglePace(parts[0].trim())
                    val max = parseSinglePace(parts[1].trim())
                    if (min > 0 && max > 0) (min + max) / 2 else 0
                }
                else -> parseSinglePace(cleaned.trim())
            }
        } catch (e: Exception) { 0 }
    }

    private fun parseSinglePace(p: String): Int {
        val match = Regex("""(\d{1,2}):(\d{2})""").find(p) ?: return 0
        val min = match.groupValues[1].toIntOrNull() ?: return 0
        val sec = match.groupValues[2].toIntOrNull() ?: return 0
        return min * 60 + sec
    }

    private fun showPaceFeedback(actualSeconds: Int, plannedSeconds: Int) {
        binding.cardPaceFeedback.visibility = View.VISIBLE
        val diff = actualSeconds - plannedSeconds

        when {
            // ✅ เร็วกว่าเป้า — ยินดี
            diff < 0 -> {
                currentPaceResult = "faster"
                currentPaceDiff = Math.abs(diff)
                binding.tvPaceFeedbackEmoji.text = "🏆"
                binding.tvPaceFeedbackTitle.text = "ยอดเยี่ยมมาก!"
                binding.tvPaceFeedbackMessage.text =
                    "เร็วกว่าเป้าหมาย ${Math.abs(diff)} วินาที/กม.\nฟอร์มดีมาก 💪"
                binding.cardPaceFeedback.setCardBackgroundColor(
                    resources.getColor(R.color.accent_green, null))
                binding.tvPaceTip.visibility = View.GONE
                binding.btnWatchTip.visibility = View.GONE
            }
            // ✅ ตรงเป้าพอดี
            diff == 0 -> {
                currentPaceResult = "on_target"
                currentPaceDiff = 0
                binding.tvPaceFeedbackEmoji.text = "🎯"
                binding.tvPaceFeedbackTitle.text = "ได้เป้าหมายพอดี!"
                binding.tvPaceFeedbackMessage.text =
                    "เพซตรงเป้าหมายเลย ✅\nสุดยอดมาก!"
                binding.cardPaceFeedback.setCardBackgroundColor(
                    resources.getColor(R.color.light_purple, null))
                binding.tvPaceTip.visibility = View.GONE
                binding.btnWatchTip.visibility = View.GONE
            }
            // ⚠️ ช้ากว่าเป้า 1-30 วินาที
            diff in 1..15 -> {
                currentPaceResult = "slower_slight"
                currentPaceDiff = diff
                binding.tvPaceFeedbackEmoji.text = "😅"
                binding.tvPaceFeedbackTitle.text = "ช้ากว่าเป้าหมาย $diff วินาที"
                binding.tvPaceFeedbackMessage.text =
                    "เพซเกินเป้ามา $diff วินาที/กม.\nลองเพิ่ม Cadence ดูนะ!"
                binding.cardPaceFeedback.setCardBackgroundColor(
                    resources.getColor(R.color.accent_orange, null))
                binding.tvPaceTip.text = "💡 เพิ่มความถี่ก้าวแทนที่จะยืดก้าวยาว"
                binding.tvPaceTip.visibility = View.VISIBLE
                binding.btnWatchTip.visibility = View.GONE
            }
            // ❌ ช้ากว่าเป้ามากกว่า 15 วินาที
            else -> {
                currentPaceResult = "slower_much"
                currentPaceDiff = diff
                binding.tvPaceFeedbackEmoji.text = "😢"
                binding.tvPaceFeedbackTitle.text = "ช้ากว่าเป้าหมายมาก"
                binding.tvPaceFeedbackMessage.text =
                    "เพซเกินเป้ามาถึง $diff วินาที/กม.\nไม่ต้องท้อ ดูคลิปเทคนิคได้เลย!"
                binding.cardPaceFeedback.setCardBackgroundColor(
                    resources.getColor(R.color.accent_red, null))
                binding.tvPaceTip.text = "📺 มีคลิปสอนเทคนิคการวิ่งให้เพซดีขึ้น"
                binding.tvPaceTip.visibility = View.VISIBLE
                binding.btnWatchTip.visibility = View.VISIBLE
                binding.btnWatchTip.setOnClickListener { openTipsVideo() }
            }
        }
    }

    private fun openTipsVideo() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RUNNING_TIPS_URL)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "ไม่สามารถเปิดลิงก์ได้", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== SAVE ====================

    private fun setupClickListeners() {
        binding.btnSaveWorkout.setOnClickListener { saveWorkout() }
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun saveWorkout() {
        val distanceStr = binding.etDistance.text.toString().trim()
        val hoursStr = binding.etHours.text.toString().trim()
        val minutesStr = binding.etMinutes.text.toString().trim()
        val secondsStr = binding.etSeconds.text.toString().trim()
        val heartRateStr = binding.etHeartRate.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()

        // Validate ระยะทาง
        if (distanceStr.isEmpty()) {
            Toast.makeText(requireContext(), "กรุณากรอกระยะทาง", Toast.LENGTH_SHORT).show()
            return
        }
        val distance = distanceStr.toDoubleOrNull()
        if (distance == null || distance <= 0) {
            Toast.makeText(requireContext(), "ระยะทางไม่ถูกต้อง", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate เวลา
        val hours = hoursStr.toLongOrNull() ?: 0L
        val minutes = minutesStr.toLongOrNull() ?: 0L
        val seconds = secondsStr.toLongOrNull() ?: 0L
        val totalSeconds = hours * 3600 + minutes * 60 + seconds

        if (totalSeconds <= 0) {
            Toast.makeText(requireContext(), "กรุณากรอกเวลาที่ใช้", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate นาที/วินาที ไม่เกิน 59
        if (minutes > 59 || seconds > 59) {
            Toast.makeText(requireContext(), "นาทีและวินาทีต้องไม่เกิน 59", Toast.LENGTH_SHORT).show()
            return
        }

        val heartRate = heartRateStr.toIntOrNull() ?: 0

        trainingData?.let { data ->
            viewModel.saveWorkout(
                trainingData = data,
                weekNumber = weekNumber,
                dayNumber = dayNumber,
                distance = distance,
                duration = totalSeconds,
                calories = 0,
                heartRate = heartRate,
                notes = notes,
                paceResult = currentPaceResult,
                paceDiffSeconds = currentPaceDiff
            )
        } ?: Toast.makeText(requireContext(), "ไม่พบข้อมูลการซ้อม", Toast.LENGTH_SHORT).show()
    }

    private fun observeViewModel() {
        viewModel.isSaving.observe(viewLifecycleOwner) { isSaving ->
            binding.progressBar.visibility = if (isSaving) View.VISIBLE else View.GONE
            binding.btnSaveWorkout.isEnabled = !isSaving
        }
        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "บันทึกการซ้อมเรียบร้อย ✅", Toast.LENGTH_SHORT).show()
                val result = Bundle().apply { putInt("week_number", weekNumber) }
                parentFragmentManager.setFragmentResult("workout_saved_return_to_schedule", result)
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

    // ==================== HELPERS ====================

    private fun hideBottomNavigation() {
        (parentFragment as? MainFragment)?.setBottomNavVisible(false)
    }

    private fun showBottomNavigation() {
        (parentFragment as? MainFragment)?.setBottomNavVisible(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        _binding = null
    }
}