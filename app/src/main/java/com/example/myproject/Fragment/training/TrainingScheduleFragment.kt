package com.example.myproject.Fragment.training

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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
        private const val ARG_INITIAL_WEEK = "initial_week"
        private const val ARG_TRAINING_PLAN_ID = "training_plan_id"
        private const val ARG_IS_VIEW_ONLY = "is_view_only"

        fun newInstance(initialWeek: Int = -1, trainingPlanId: String? = null, isViewOnly: Boolean = false): TrainingScheduleFragment {
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

        isViewOnlyMode = arguments?.getBoolean(ARG_IS_VIEW_ONLY, false) ?: false

        if (!isViewOnlyMode) {
            isViewOnlyMode = sharedPreferences.getBoolean("is_view_only_program", false)
        }

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

            if (!isViewOnlyMode) {
                checkAndMarkMissedDaysBeforeStart()
            }

            val initialWeekFromArgs = arguments?.getInt(ARG_INITIAL_WEEK, -1) ?: -1

            if (initialWeekFromArgs > 0) {
                selectWeek(initialWeekFromArgs)
            } else {
                viewModel.calculateAndLoadCurrentWeek()
            }
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
            checkProgramCompletionInTrainingSchedule()

            viewModel.currentWeek.value?.let { week ->
                viewModel.loadTrainingWeekRealtime(week)
            }
        }
    }

    private fun setupFragmentResultListener() {
        parentFragmentManager.setFragmentResultListener(
            RecordWorkoutFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val savedWeekNumber = bundle.getInt(RecordWorkoutFragment.RESULT_WEEK_NUMBER, 1)
            viewModel.loadTrainingWeekRealtime(savedWeekNumber)
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
        viewModel.trainingDays.observe(viewLifecycleOwner) { days ->
            trainingAdapter.updateTrainingDays(days, viewModel.currentWeek.value ?: 1)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.currentWeek.observe(viewLifecycleOwner) { week ->
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

    private fun selectWeek(week: Int) {
        binding.tvHeaderSubtitle.text = "สัปดาห์ที่ $week"
        highlightSelectedWeek(week)
        viewModel.loadTrainingWeekRealtime(week)
    }

    private fun resetWeekButtons() {
        val weekData = listOf(
            Triple(binding.btnWeek1, binding.root.findViewById<TextView>(R.id.tvWeek1),
                binding.root.findViewById<android.widget.ImageView>(R.id.iconWeek1)),
            Triple(binding.btnWeek2, binding.root.findViewById<TextView>(R.id.tvWeek2),
                binding.root.findViewById<android.widget.ImageView>(R.id.iconWeek2)),
            Triple(binding.btnWeek3, binding.root.findViewById<TextView>(R.id.tvWeek3),
                binding.root.findViewById<android.widget.ImageView>(R.id.iconWeek3)),
            Triple(binding.btnWeek4, binding.root.findViewById<TextView>(R.id.tvWeek4),
                binding.root.findViewById<android.widget.ImageView>(R.id.iconWeek4))
        )

        weekData.forEach { (card, textView, icon) ->
            card.apply {
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
                strokeColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                strokeWidth = 2
                cardElevation = 2f
            }
            textView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_text))
            icon?.visibility = View.GONE
        }
    }

    private fun highlightSelectedWeek(week: Int) {
        resetWeekButtons()

        val selectedData = when (week) {
            1 -> Triple(binding.btnWeek1, binding.root.findViewById<TextView>(R.id.tvWeek1),
                binding.root.findViewById<android.widget.ImageView>(R.id.iconWeek1))
            2 -> Triple(binding.btnWeek2, binding.root.findViewById<TextView>(R.id.tvWeek2),
                binding.root.findViewById<android.widget.ImageView>(R.id.iconWeek2))
            3 -> Triple(binding.btnWeek3, binding.root.findViewById<TextView>(R.id.tvWeek3),
                binding.root.findViewById<android.widget.ImageView>(R.id.iconWeek3))
            4 -> Triple(binding.btnWeek4, binding.root.findViewById<TextView>(R.id.tvWeek4),
                binding.root.findViewById<android.widget.ImageView>(R.id.iconWeek4))
            else -> Triple(binding.btnWeek1, binding.root.findViewById<TextView>(R.id.tvWeek1),
                binding.root.findViewById<android.widget.ImageView>(R.id.iconWeek1))
        }

        val (selectedCard, selectedText, selectedIcon) = selectedData

        selectedCard.apply {
            setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.darkBlue))
            strokeColor = ContextCompat.getColor(requireContext(), R.color.darkBlue)
            strokeWidth = 0
            cardElevation = 8f
        }
        selectedText?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        selectedIcon?.apply {
            visibility = View.VISIBLE
            setColorFilter(ContextCompat.getColor(requireContext(), R.color.white))
        }

        binding.root.findViewById<TextView>(R.id.tvWeekProgress)?.text = "สัปดาห์ $week/4"
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

    private fun checkAndMarkMissedDaysBeforeStart() {
        val userId = auth.currentUser?.uid ?: return
        val startDate = sharedPreferences.getLong("program_start_date", 0L)

        if (startDate == 0L) return

        val hasMarkedInitialMissed = sharedPreferences.getBoolean("has_marked_initial_missed", false)
        if (hasMarkedInitialMissed) return

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
            firestore.collection("Athletes")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val weekData = document.get("week_1") as? HashMap<*, *>
                    val updates = mutableMapOf<String, Any>()

                    for (day in 1 until programStartDay) {
                        val dayData = weekData?.get("day_$day") as? HashMap<*, *>
                        val type = dayData?.get("type") as? String ?: ""

                        val isRestDay = type.equals("Rest Day", ignoreCase = true) ||
                                type.equals("RestDay", ignoreCase = true) ||
                                type.equals("Rest", ignoreCase = true) ||
                                type.contains("พักผ่อน", ignoreCase = true) ||
                                type.contains("พัก", ignoreCase = true)

                        if (!isRestDay) {
                            val fieldPath = "week_1.day_$day.isMissed"
                            updates[fieldPath] = true
                        }
                    }

                    if (updates.isNotEmpty()) {
                        firestore.collection("Athletes")
                            .document(userId)
                            .update(updates)
                            .addOnSuccessListener {
                                sharedPreferences.edit()
                                    .putBoolean("has_marked_initial_missed", true)
                                    .apply()
                            }
                    } else {
                        sharedPreferences.edit()
                            .putBoolean("has_marked_initial_missed", true)
                            .apply()
                    }
                }
        } else {
            sharedPreferences.edit()
                .putBoolean("has_marked_initial_missed", true)
                .apply()
        }
    }

    private fun checkProgramCompletionInTrainingSchedule() {
        val userId = auth.currentUser?.uid ?: return
        val startDate = sharedPreferences.getLong("program_start_date", 0L)

        if (startDate == 0L) return

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

        if (daysSinceStart >= totalDays) {
            showProgramCompletionDialog()
        } else if (!isViewOnlyMode) {
            checkMissedDaysImmediately()
        }
    }

    private fun checkMissedDaysImmediately() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("Athletes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) return@addOnSuccessListener

                val programStartDate = try {
                    document.getTimestamp("startDate")?.toDate()
                } catch (e: Exception) {
                    val startDateLong = document.getLong("startDate")
                    if (startDateLong != null) Date(startDateLong) else null
                }

                if (programStartDate == null) return@addOnSuccessListener

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

                        val isRestDay = type.equals("Rest Day", ignoreCase = true) ||
                                type.equals("RestDay", ignoreCase = true) ||
                                type.equals("Rest", ignoreCase = true) ||
                                type.contains("พักผ่อน", ignoreCase = true) ||
                                type.contains("พัก", ignoreCase = true)

                        if (dayDate.before(today) && !isCompleted && !isMissed && !isRestDay) {
                            val fieldPath = "week_$week.day_$day.isMissed"
                            updates[fieldPath] = true
                        }
                    }
                }

                if (updates.isNotEmpty()) {
                    firestore.collection("Athletes")
                        .document(userId)
                        .update(updates)
                        .addOnSuccessListener {
                            viewModel.currentWeek.value?.let { week ->
                                viewModel.loadTrainingWeekRealtime(week)
                            }
                        }
                }
            }
    }

    private fun showProgramCompletionDialog() {
        if (!isAdded) return

        val isViewOnly = sharedPreferences.getBoolean("is_view_only_program", false)
        val message = if (isViewOnly) {
            "คุณซ้อมครบโปรแกรมมือใหม่ 4 สัปดาห์แล้ว!\n\nระบบจะออกจากโปรแกรมอัตโนมัติและพาคุณกลับไปหน้าเลือกโปรแกรมใหม่"
        } else {
            "คุณซ้อมครบตามเป้าหมาย 4 สัปดาห์แล้ว!\n\nระบบจะออกจากโปรแกรมอัตโนมัติและพาคุณกลับไปหน้าเลือกโปรแกรมใหม่"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("🎉 ยินดีด้วย!")
            .setMessage(message)
            .setPositiveButton("ตกลง") { _, _ ->
                autoExitProgram()
            }
            .setCancelable(false)
            .show()
    }

    private fun autoExitProgram() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            clearLocalProgram()
            navigateToHome()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("Athletes")
            .document(userId)
            .delete()
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener

                clearLocalProgram()
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "ยินดีด้วย! คุณซ้อมเสร็จแล้ว เลือกโปรแกรมใหม่ได้เลย", Toast.LENGTH_LONG).show()
                navigateToHome()
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener

                firestore.collection("Athletes")
                    .document(userId)
                    .update(
                        mapOf(
                            "isActive" to false,
                            "completedAt" to System.currentTimeMillis()
                        )
                    )
                    .addOnSuccessListener {
                        if (!isAdded || _binding == null) return@addOnSuccessListener

                        clearLocalProgram()
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "ยินดีด้วย! คุณซ้อมเสร็จแล้ว!", Toast.LENGTH_SHORT).show()
                        navigateToHome()
                    }
                    .addOnFailureListener {
                        if (!isAdded || _binding == null) return@addOnFailureListener

                        clearLocalProgram()
                        binding.progressBar.visibility = View.GONE
                        navigateToHome()
                    }
            }
    }

    private fun navigateToHome() {
        (activity as? MainActivity)?.replaceFragment(HomeFragment.newInstance())
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}