package com.example.myproject.Fragment.training

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.Fragment.home.HomeFragment
import com.example.myproject.Fragment.workout.RecordWorkoutFragment
import com.example.myproject.MainActivity
import com.example.myproject.ProgramSelectionActivity
import com.example.myproject.R
import com.example.myproject.data.training.TrainingModel
import com.example.myproject.data.training.TrainingRepository
import com.example.myproject.databinding.FragmentTrainingScheduleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date

class TrainingScheduleFragment : Fragment() {

    private var _binding: FragmentTrainingScheduleBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TrainingScheduleViewModel
    private lateinit var trainingAdapter: TrainingScheduleAdapter

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val sharedPreferences by lazy {
        requireContext().getSharedPreferences("running_app_prefs", Context.MODE_PRIVATE)
    }

    private var isViewOnlyMode: Boolean = false

    companion object {
        private const val TAG = "TrainingScheduleFragment"
        private const val ARG_INITIAL_WEEK = "initial_week"
        private const val ARG_TRAINING_PLAN_ID = "training_plan_id"
        private const val ARG_IS_VIEW_ONLY = "is_view_only"

        fun newInstance(initialWeek: Int = 1, trainingPlanId: String? = null, isViewOnly: Boolean = false): TrainingScheduleFragment {
            val fragment = TrainingScheduleFragment()
            val bundle = Bundle().apply {
                putInt(ARG_INITIAL_WEEK, initialWeek)
                trainingPlanId?.let { putString(ARG_TRAINING_PLAN_ID, it) }
                putBoolean(ARG_IS_VIEW_ONLY, isViewOnly)
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainingScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ เช็คว่าเป็นโหมดดูอย่างเดียวหรือไม่
        isViewOnlyMode = arguments?.getBoolean(ARG_IS_VIEW_ONLY, false) ?: false

        // ถ้าไม่มีใน arguments ให้เช็คจาก SharedPreferences
        if (!isViewOnlyMode) {
            isViewOnlyMode = sharedPreferences.getBoolean("is_view_only_program", false)
        }

        Log.d(TAG, "📖 View Only Mode: $isViewOnlyMode")

        val firestore = FirebaseFirestore.getInstance()
        val repository = TrainingRepository(firestore)
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TrainingScheduleViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return TrainingScheduleViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
        viewModel = ViewModelProvider(this, factory)[TrainingScheduleViewModel::class.java]

        // ✅ ส่ง isViewOnlyMode ไปที่ Adapter
        trainingAdapter = TrainingScheduleAdapter(isViewOnlyMode) { trainingData, weekNumber, dayNumber ->
            if (!isViewOnlyMode) {
                openRecordWorkoutFragment(trainingData, weekNumber, dayNumber)
            } else {
                Toast.makeText(requireContext(), "โปรแกรมนี้ดูได้อย่างเดียว ไม่สามารถบันทึกการซ้อมได้", Toast.LENGTH_SHORT).show()
            }
        }

        binding.recyclerViewTraining.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = trainingAdapter
        }

        setupFragmentResultListener()
        setupClickListeners()
        observeViewModel()

        val planFromArgs = arguments?.getString(ARG_TRAINING_PLAN_ID)
        viewModel.selectedTrainingPlanId = planFromArgs ?: getSavedSelectedPlan()

        if (viewModel.selectedTrainingPlanId != null) {
            showTrainingSchedule()

            // ✅ โปรแกรมมือใหม่ไม่ต้องมาร์ค missed
            if (!isViewOnlyMode) {
                checkAndMarkMissedDaysBeforeStart()
            }

            val initialWeek = arguments?.getInt(ARG_INITIAL_WEEK, 1) ?: 1
            Log.d(TAG, "📍 Opening with initial week: $initialWeek")
            selectWeek(initialWeek)
        } else {
            showInitialState()
        }
    }

    override fun onResume() {
        super.onResume()

        val isProgramSelected = sharedPreferences.getBoolean("program_selected", false)

        if (!isProgramSelected) {
            Toast.makeText(requireContext(), "กรุณาเลือกโปรแกรมก่อน", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.replaceFragment(HomeFragment.newInstance())
        } else {
            // ✅ เช็คว่าโปรแกรมมือใหม่หมดอายุหรือยัง
            if (isViewOnlyMode) {
                checkBeginnerProgramExpiry()
            } else {
                // โปรแกรมปกติ - เช็คขาดซ้อม
                checkMissedDaysImmediately()
            }

            // โหลดข้อมูลสัปดาห์ปัจจุบัน
            viewModel.currentWeek.value?.let { week ->
                viewModel.loadTrainingWeekRealtime(week)
            }
        }
    }

    /**
     * รับผลลัพธ์จาก RecordWorkoutFragment เมื่อบันทึกสำเร็จ
     */
    private fun setupFragmentResultListener() {
        parentFragmentManager.setFragmentResultListener(
            RecordWorkoutFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val savedWeekNumber = bundle.getInt(RecordWorkoutFragment.RESULT_WEEK_NUMBER, 1)

            Log.d(TAG, "✅ Workout saved for week $savedWeekNumber, refreshing...")

            // รีเฟรชข้อมูลสัปดาห์ที่บันทึก
            viewModel.loadTrainingWeekRealtime(savedWeekNumber)

            // อัปเดตสัปดาห์ปัจจุบัน
            selectWeek(savedWeekNumber)
        }
    }

    private fun setupClickListeners() {
        binding.btnStart.setOnClickListener {
            val intent = Intent(requireContext(), ProgramSelectionActivity::class.java)
            startActivity(intent)
        }

        binding.btnWeek1.setOnClickListener { selectWeek(1) }
        binding.btnWeek2.setOnClickListener { selectWeek(2) }
        binding.btnWeek3.setOnClickListener { selectWeek(3) }
        binding.btnWeek4.setOnClickListener { selectWeek(4) }
    }

    private fun observeViewModel() {
        // Observe training days
        viewModel.trainingDays.observe(viewLifecycleOwner) { days ->
            trainingAdapter.updateTrainingDays(days, viewModel.currentWeek.value ?: 1)
            Log.d(TAG, "✅ Loaded ${days.size} training days")
        }

        // Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe error
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Observe current week
        viewModel.currentWeek.observe(viewLifecycleOwner) { week ->
            Log.d(TAG, "📍 Current week changed to: $week")
            binding.tvHeaderSubtitle.text = "สัปดาห์ที่ $week"
            highlightSelectedWeek(week)
        }
    }

    private fun showInitialState() {
        binding.cardInitialState.visibility = View.VISIBLE
        binding.layoutWeekNavigation.visibility = View.GONE
        binding.recyclerViewTraining.visibility = View.GONE
        binding.tvHeaderSubtitle.visibility = View.GONE
    }

    private fun showTrainingSchedule() {
        binding.cardInitialState.visibility = View.GONE
        binding.layoutWeekNavigation.visibility = View.VISIBLE
        binding.recyclerViewTraining.visibility = View.VISIBLE
        binding.tvHeaderSubtitle.visibility = View.VISIBLE
    }

    /**
     * เลือกสัปดาห์และโหลดข้อมูล
     */
    private fun selectWeek(week: Int) {
        Log.d(TAG, "📅 Selecting week: $week")

        binding.tvHeaderSubtitle.text = "สัปดาห์ที่ $week"
        resetWeekButtons()
        highlightSelectedWeek(week)

        // โหลดข้อมูลสัปดาห์นั้น
        viewModel.loadTrainingWeekRealtime(week)
    }

    private fun resetWeekButtons() {
        val buttons = listOf(binding.btnWeek1, binding.btnWeek2, binding.btnWeek3, binding.btnWeek4)
        buttons.forEach { button ->
            button.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            button.setTextColor(resources.getColor(R.color.purple, null))
            button.alpha = 0.5f
        }
    }

    private fun highlightSelectedWeek(week: Int) {
        val selectedButton = when (week) {
            1 -> binding.btnWeek1
            2 -> binding.btnWeek2
            3 -> binding.btnWeek3
            4 -> binding.btnWeek4
            else -> binding.btnWeek1
        }
        selectedButton.setBackgroundColor(resources.getColor(R.color.purple, null))
        selectedButton.setTextColor(resources.getColor(R.color.white, null))
        selectedButton.alpha = 1.0f

        Log.d(TAG, "✅ Week button $week highlighted")
    }

    private fun getSavedSelectedPlan(): String? {
        return sharedPreferences.getString("selected_program_name", null)
    }

    private fun openRecordWorkoutFragment(
        trainingData: TrainingModel,
        weekNumber: Int,
        dayNumber: Int
    ) {
        val fragment = RecordWorkoutFragment.newInstance(trainingData, weekNumber, dayNumber)

        parentFragmentManager.commit {
            replace(R.id.container, fragment)
            addToBackStack(null)
        }
    }

    /**
     * ✅ เช็คและมาร์ควันที่ขาดก่อนเริ่มโปรแกรม (เฉพาะครั้งแรก)
     */
    private fun checkAndMarkMissedDaysBeforeStart() {
        val userId = auth.currentUser?.uid ?: return
        val startDate = sharedPreferences.getLong("program_start_date", 0L)

        if (startDate == 0L) {
            Log.d(TAG, "⚠️ No start date found, skipping initial missed check")
            return
        }

        // เช็คว่าเคยมาร์คแล้วหรือยัง
        val hasMarkedInitialMissed = sharedPreferences.getBoolean("has_marked_initial_missed", false)
        if (hasMarkedInitialMissed) {
            Log.d(TAG, "✅ Already marked initial missed days")
            return
        }

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

        Log.d(TAG, "📅 Program started on day: $programStartDay (1=จันทร์, 7=อาทิตย์)")

        if (programStartDay > 1) {
            val updates = mutableMapOf<String, Any>()

            for (day in 1 until programStartDay) {
                val fieldPath = "week_1.day_$day.isMissed"
                updates[fieldPath] = true
                Log.d(TAG, "❌ Marking day $day as missed before program start")
            }

            if (updates.isNotEmpty()) {
                firestore.collection("Athletes")
                    .document(userId)
                    .update(updates)
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Marked ${updates.size} days as missed before start")

                        sharedPreferences.edit()
                            .putBoolean("has_marked_initial_missed", true)
                            .apply()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Failed to mark initial missed days", e)
                    }
            }
        } else {
            sharedPreferences.edit()
                .putBoolean("has_marked_initial_missed", true)
                .apply()
            Log.d(TAG, "✅ Program starts on Monday, no initial missed days")
        }
    }

    /**
     * ✅ เช็คขาดซ้อมทันทีเมื่อเปิดแอพ (Real-time check)
     */
    private fun checkMissedDaysImmediately() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("Athletes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.d(TAG, "⚠️ No athlete document found")
                    return@addOnSuccessListener
                }

                val programStartDate = try {
                    document.getTimestamp("startDate")?.toDate()
                } catch (e: Exception) {
                    val startDateLong = document.getLong("startDate")
                    if (startDateLong != null) {
                        Date(startDateLong)
                    } else {
                        null
                    }
                }

                if (programStartDate == null) {
                    Log.d(TAG, "⚠️ No program start date found")
                    return@addOnSuccessListener
                }

                val programStart = Calendar.getInstance().apply {
                    time = programStartDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val updates = mutableMapOf<String, Any>()

                for (week in 1..4) {
                    val weekData = document.get("week_$week") as? HashMap<*, *>
                    if (weekData == null) continue

                    for (day in 1..7) {
                        val dayData = weekData["day_$day"] as? HashMap<*, *>
                        if (dayData == null) continue

                        val isCompleted = dayData["isCompleted"] as? Boolean ?: false
                        val isMissed = dayData["isMissed"] as? Boolean ?: false
                        val type = dayData["type"] as? String ?: ""

                        val dayDate = Calendar.getInstance().apply {
                            time = programStart.time
                            add(Calendar.DAY_OF_YEAR, ((week - 1) * 7) + (day - 1))
                        }

                        if (dayDate.before(today) &&
                            !isCompleted &&
                            !isMissed &&
                            !type.equals("Rest Day", ignoreCase = true)) {

                            val fieldPath = "week_$week.day_$day.isMissed"
                            updates[fieldPath] = true
                            Log.d(TAG, "❌ Marking as missed: Week $week, Day $day ($type)")
                        }
                    }
                }

                if (updates.isNotEmpty()) {
                    firestore.collection("Athletes")
                        .document(userId)
                        .update(updates)
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ Marked ${updates.size} days as missed")

                            viewModel.currentWeek.value?.let { week ->
                                viewModel.loadTrainingWeekRealtime(week)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Failed to mark missed days", e)
                        }
                } else {
                    Log.d(TAG, "✅ No missed days found")
                }

                checkProgramCompletion(programStart, today)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error checking missed days", e)
            }
    }

    /**
     * ✅ เช็คว่าโปรแกรม 4 สัปดาห์จบแล้วหรือยัง
     */
    private fun checkProgramCompletion(programStart: Calendar, today: Calendar) {
        val daysSinceStart = ((today.timeInMillis - programStart.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        val totalDays = 28

        Log.d(TAG, "📊 Days since start: $daysSinceStart / $totalDays")

        if (daysSinceStart >= totalDays) {
            Log.d(TAG, "🎉 Program completed! Showing completion dialog...")
            showProgramCompletionDialog()
        }
    }

    /**
     * ✅ แสดง Dialog เมื่อจบโปรแกรม
     */
    private fun showProgramCompletionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("🎉 ยินดีด้วย!")
            .setMessage("คุณซ้อมครบตามเป้าหมาย 4 สัปดาห์แล้ว!\n\nต้องการออกจากโปรแกรมและเริ่มโปรแกรมใหม่หรือไม่?")
            .setPositiveButton("ออกจากโปรแกรม") { _, _ ->
                clearLocalProgram()
                (activity as? MainActivity)?.replaceFragment(HomeFragment.newInstance())
            }
            .setNegativeButton("ดูต่อ", null)
            .setCancelable(false)
            .show()
    }

    private fun clearLocalProgram() {
        sharedPreferences.edit().apply {
            putBoolean("program_selected", false)
            remove("selected_program_name")
            remove("selected_program_display_name")
            remove("selected_sub_program_name")
            remove("program_start_date")
            remove("has_marked_initial_missed")
            remove("is_view_only_program")
            apply()
        }
        viewModel.selectedTrainingPlanId = null
    }

    /**
     * ✅ เช็คว่าโปรแกรมมือใหม่หมดอายุหรือยัง (ครบ 4 สัปดาห์)
     */
    private fun checkBeginnerProgramExpiry() {
        val userId = auth.currentUser?.uid ?: return
        val startDate = sharedPreferences.getLong("program_start_date", 0L)

        if (startDate == 0L) {
            Log.w(TAG, "⚠️ No start date found for beginner program")
            return
        }

        val startCalendar = Calendar.getInstance().apply {
            timeInMillis = startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val daysSinceStart = ((today.timeInMillis - startCalendar.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        val totalDays = 28

        Log.d(TAG, "📊 Beginner Program - Days: $daysSinceStart / $totalDays")

        if (daysSinceStart >= totalDays) {
            Log.d(TAG, "🎉 Beginner program completed! Auto-exiting...")
            showBeginnerProgramCompletionDialog()
        }
    }

    /**
     * ✅ แสดง Dialog เมื่อจบโปรแกรมมือใหม่
     */
    private fun showBeginnerProgramCompletionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("🎉 ยินดีด้วย!")
            .setMessage("คุณซ้อมครบโปรแกรมมือใหม่ 4 สัปดาห์แล้ว!\n\nระบบจะออกจากโปรแกรมอัตโนมัติ คุณสามารถเลือกโปรแกรมใหม่ได้")
            .setPositiveButton("ตกลง") { _, _ ->
                autoExitBeginnerProgram()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * ✅ ออกจากโปรแกรมมือใหม่อัตโนมัติ
     */
    private fun autoExitBeginnerProgram() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            clearLocalProgram()
            (activity as? MainActivity)?.replaceFragment(HomeFragment.newInstance())
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("Athletes")
            .document(userId)
            .delete()
            .addOnSuccessListener {
                clearLocalProgram()
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "จบโปรแกรมมือใหม่สำเร็จ! เลือกโปรแกรมใหม่ได้เลย", Toast.LENGTH_LONG).show()
                (activity as? MainActivity)?.replaceFragment(HomeFragment.newInstance())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to delete program", e)
                firestore.collection("Athletes")
                    .document(userId)
                    .update(
                        mapOf(
                            "isActive" to false,
                            "completedAt" to System.currentTimeMillis()
                        )
                    )
                    .addOnSuccessListener {
                        clearLocalProgram()
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "จบโปรแกรมมือใหม่สำเร็จ!", Toast.LENGTH_SHORT).show()
                        (activity as? MainActivity)?.replaceFragment(HomeFragment.newInstance())
                    }
                    .addOnFailureListener { updateError ->
                        Log.e(TAG, "❌ Failed to update program", updateError)
                        clearLocalProgram()
                        binding.progressBar.visibility = View.GONE
                        (activity as? MainActivity)?.replaceFragment(HomeFragment.newInstance())
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView - cleaning up")
    }
}