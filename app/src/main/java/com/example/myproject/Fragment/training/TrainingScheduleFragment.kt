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

    companion object {
        private const val TAG = "TrainingScheduleFragment"
        private const val ARG_INITIAL_WEEK = "initial_week"
        private const val ARG_TRAINING_PLAN_ID = "training_plan_id"

        /**
         * ⭐ เปลี่ยนจาก String? เป็น Int สำหรับ initialWeek
         */
        fun newInstance(initialWeek: Int = 1, trainingPlanId: String? = null): TrainingScheduleFragment {
            val fragment = TrainingScheduleFragment()
            val bundle = Bundle().apply {
                putInt(ARG_INITIAL_WEEK, initialWeek)
                trainingPlanId?.let { putString(ARG_TRAINING_PLAN_ID, it) }
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

        trainingAdapter = TrainingScheduleAdapter { trainingData, weekNumber, dayNumber ->
            openRecordWorkoutFragment(trainingData, weekNumber, dayNumber)
        }

        binding.recyclerViewTraining.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = trainingAdapter
        }

        setupClickListeners()
        observeViewModel()

        val planFromArgs = arguments?.getString(ARG_TRAINING_PLAN_ID)
        viewModel.selectedTrainingPlanId = planFromArgs ?: getSavedSelectedPlan()

        if (viewModel.selectedTrainingPlanId != null) {
            showTrainingSchedule()

            // ⭐ รับสัปดาห์ที่ส่งมาและเปิดที่สัปดาห์นั้น
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
            // ⭐ เช็คขาดซ้อมทุกสัปดาห์ (1-4) เพื่อให้แน่ใจว่าไม่พลาด
            for (week in 1..4) {
                viewModel.checkMissedDays(week)
            }

            // โหลดข้อมูลสัปดาห์ปัจจุบัน
            viewModel.currentWeek.value?.let { week ->
                viewModel.loadTrainingWeekRealtime(week)
            }
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

        binding.btnExitPlan.setOnClickListener {
            showExitConfirmDialog()
        }
    }

    private fun showExitConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("ออกจากตารางซ้อม")
            .setMessage("คุณต้องการออกจากตารางซ้อมนี้หรือไม่? ข้อมูลทั้งหมดจะถูกรีเซ็ตและคุณสามารถเลือกโปรแกรมใหม่ได้")
            .setPositiveButton("ออกจากโปรแกรม") { _, _ ->
                resetProgramCompletelyFromFirebase()
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun resetProgramCompletelyFromFirebase() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            clearLocalProgram()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("Athletes")
            .document(userId)
            .delete()
            .addOnSuccessListener {
                clearLocalProgram()
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "ออกจากโปรแกรมและรีเซ็ตเรียบร้อย", Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.replaceFragment(HomeFragment.newInstance())
            }
            .addOnFailureListener { e ->
                firestore.collection("Athletes")
                    .document(userId)
                    .update(
                        mapOf(
                            "isActive" to false,
                            "programId" to "",
                            "programDisplayName" to "",
                            "subProgramName" to "",
                            "exitedAt" to System.currentTimeMillis()
                        )
                    )
                    .addOnSuccessListener {
                        clearLocalProgram()
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "ออกจากโปรแกรมเรียบร้อย", Toast.LENGTH_SHORT).show()
                        (activity as? MainActivity)?.replaceFragment(HomeFragment.newInstance())
                    }
                    .addOnFailureListener { updateError ->
                        clearLocalProgram()
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "เกิดข้อผิดพลาด แต่ออกจากโปรแกรมแล้ว", Toast.LENGTH_SHORT).show()
                        (activity as? MainActivity)?.replaceFragment(HomeFragment.newInstance())
                    }
            }
    }

    private fun clearLocalProgram() {
        sharedPreferences.edit().apply {
            putBoolean("program_selected", false)
            remove("selected_program_name")
            remove("selected_program_display_name")
            remove("selected_sub_program_name")
            apply()
        }
        viewModel.selectedTrainingPlanId = null
    }

    private fun observeViewModel() {
        // ⭐ Observe training days
        viewModel.trainingDays.observe(viewLifecycleOwner) { days ->
            trainingAdapter.updateTrainingDays(days, viewModel.currentWeek.value ?: 1)
            Log.d(TAG, "✅ Loaded ${days.size} training days")
        }

        // ⭐ Observe loading state
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // ⭐ Observe error
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // ⭐ Observe current week
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
        binding.btnExitPlan.visibility = View.INVISIBLE
    }

    private fun showTrainingSchedule() {
        binding.cardInitialState.visibility = View.GONE
        binding.layoutWeekNavigation.visibility = View.VISIBLE
        binding.recyclerViewTraining.visibility = View.VISIBLE
        binding.tvHeaderSubtitle.visibility = View.VISIBLE
        binding.btnExitPlan.visibility = View.VISIBLE
    }

    /**
     * ⭐ เลือกสัปดาห์และโหลดข้อมูล
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

        (activity as? MainActivity)?.supportFragmentManager?.commit {
            replace(R.id.container_main, fragment)
            addToBackStack(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Log.d(TAG, "onDestroyView - cleaning up")
    }
}