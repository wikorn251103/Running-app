package com.example.myproject.Fragment.training

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.Fragment.home.HomeFragment
import com.example.myproject.Fragment.target.TargetDistanceFragment
import com.example.myproject.MainActivity
import com.example.myproject.R
import com.example.myproject.data.training.TrainingRepository
import com.example.myproject.databinding.FragmentTrainingScheduleBinding
import com.google.firebase.firestore.FirebaseFirestore

class TrainingScheduleFragment : Fragment() {

    private var _binding: FragmentTrainingScheduleBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TrainingScheduleViewModel
    private lateinit var trainingAdapter: TrainingScheduleAdapter

    // ใช้ SharedPreferences เดียวกับ HomeFragment
    private val sharedPreferences by lazy {
        requireContext().getSharedPreferences("running_app_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val ARG_TRAINING_PLAN_ID = "training_plan_id"

        fun newInstance(trainingPlanId: String? = null): TrainingScheduleFragment {
            val fragment = TrainingScheduleFragment()
            val bundle = Bundle()
            trainingPlanId?.let { bundle.putString(ARG_TRAINING_PLAN_ID, it) }
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

        // สร้าง Repository และ ViewModel
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

        // Setup RecyclerView
        trainingAdapter = TrainingScheduleAdapter()
        binding.recyclerViewTraining.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = trainingAdapter
        }

        setupClickListeners()
        observeViewModel()

        // โหลด plan จาก arguments หรือ SharedPreferences
        val planFromArgs = arguments?.getString(ARG_TRAINING_PLAN_ID)
        viewModel.selectedTrainingPlanId = planFromArgs ?: getSavedSelectedPlan()

        // ถ้ามี planId → แสดงตาราง, ถ้าไม่มีก็แสดงหน้าเริ่มต้น
        if (viewModel.selectedTrainingPlanId != null) {
            showTrainingSchedule()
        } else {
            showInitialState()
        }
    }

    override fun onResume() {
        super.onResume()

        // เช็คว่ายังมีโปรแกรมอยู่หรือไม่
        val isProgramSelected = sharedPreferences.getBoolean("program_selected", false)

        if (!isProgramSelected) {
            // ถ้าไม่มีโปรแกรมแล้ว กลับไปหน้า Home
            Toast.makeText(requireContext(), "กรุณาเลือกโปรแกรมก่อน", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.replaceFragment(HomeFragment.newInstance())
        }
    }

    private fun setupClickListeners() {
        // ปุ่มเลือกแผนใหม่
        binding.btnStart.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(TargetDistanceFragment.newInstance())
        }

        // ปุ่มเลือกสัปดาห์
        binding.btnWeek1.setOnClickListener { selectWeek(1) }
        binding.btnWeek2.setOnClickListener { selectWeek(2) }
        binding.btnWeek3.setOnClickListener { selectWeek(3) }
        binding.btnWeek4.setOnClickListener { selectWeek(4) }

        // ปุ่มออกจากแผน - ต้องเคลียร์ทั้ง 2 ที่
        binding.btnExitPlan.setOnClickListener {
            showExitConfirmDialog()
        }
    }

    private fun showExitConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("ออกจากตารางซ้อม")
            .setMessage("คุณต้องการออกจากตารางซ้อมนี้หรือไม่? ความก้าวหน้าจะถูกรีเซ็ต")
            .setPositiveButton("ออกจากโปรแกรม") { _, _ ->
                exitProgram()
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun exitProgram() {
        // เคลียร์ SharedPreferences ทั้งหมด (ทั้ง HomeFragment และ TrainingScheduleFragment)
        sharedPreferences.edit().apply {
            putBoolean("program_selected", false)
            remove("selected_program_name")
            remove("selected_program_display_name")
            remove("selected_sub_program_name")
            apply()
        }

        // เคลียร์ ViewModel
        viewModel.selectedTrainingPlanId = null

        Toast.makeText(requireContext(), "ออกจากโปรแกรมเรียบร้อย", Toast.LENGTH_SHORT).show()

        // กลับไปหน้า Home
        (activity as? MainActivity)?.replaceFragment(HomeFragment.newInstance())
    }

    private fun observeViewModel() {
        viewModel.trainingDays.observe(viewLifecycleOwner) { days ->
            trainingAdapter.updateTrainingDays(days)
        }

        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
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

        // เริ่มต้นที่สัปดาห์ที่ 1
        selectWeek(1)
    }

    private fun selectWeek(week: Int) {
        viewModel.currentWeek = week
        binding.tvHeaderSubtitle.text = "สัปดาห์ที่ $week"
        resetWeekButtons()
        highlightSelectedWeek(week)

        viewModel.selectedTrainingPlanId?.let { planId ->
            viewModel.loadTrainingWeek(planId, week)
        } ?: run {
            Toast.makeText(context, "ไม่พบ Training Plan ID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetWeekButtons() {
        val buttons = listOf(binding.btnWeek1, binding.btnWeek2, binding.btnWeek3, binding.btnWeek4)
        buttons.forEach { button ->
            button.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            button.setTextColor(resources.getColor(R.color.purple, null))
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
    }

    // ใช้ SharedPreferences เดียวกับ HomeFragment
    private fun getSavedSelectedPlan(): String? {
        return sharedPreferences.getString("selected_program_name", null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


