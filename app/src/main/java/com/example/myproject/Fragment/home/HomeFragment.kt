package com.example.myproject.Fragment.home

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.Fragment.drill.ListDrillFragment
import com.example.myproject.Fragment.target.TargetDistanceFragment
import com.example.myproject.MainActivity
import com.example.myproject.R
import com.example.myproject.databinding.FragmentHomeBinding
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ViewModel
    private val viewModel: HomeViewModel by viewModels()

    // SharedPreferences
    private val sharedPreferences by lazy {
        requireContext().getSharedPreferences("running_app_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val TAG = "HomeFragment"
        fun newInstance() = HomeFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        binding.recyclerTrainingDays.layoutManager = LinearLayoutManager(requireContext())

        // Observe ViewModel
        observeViewModel()

        // Setup Click Listeners
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // โหลดสถานะทุกครั้งที่กลับมาหน้านี้
        Log.d(TAG, "onResume - reloading UI state")
        loadAndUpdateUIState()
    }

    private fun setupClickListeners() {
        // ======= STATE 1: ยังไม่เลือกตารางซ้อม =======
        binding.nextBtn.setOnClickListener {
            binding.nextBtn.setColorFilter(ContextCompat.getColor(requireContext(), R.color.yellow))
            (activity as? MainActivity)?.replaceFragment(TargetDistanceFragment.newInstance())
        }

        binding.notSelectedStateCard.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(TargetDistanceFragment.newInstance())
        }

        // ======= STATE 2: เลือกตารางซ้อมแล้ว =======
        binding.mainProgramCard.setOnClickListener {
            // TODO: ไปหน้ารายละเอียดโปรแกรม
            Toast.makeText(requireContext(), "ไปหน้ารายละเอียดโปรแกรม", Toast.LENGTH_SHORT).show()
        }

        binding.exitProgramButton.setOnClickListener {
            showExitProgramDialog()
        }

        binding.subProgramCard.setOnClickListener {
            // TODO: ไปหน้ารายการโปรแกรมย่อย
            Toast.makeText(requireContext(), "ไปหน้าโปรแกรมย่อย", Toast.LENGTH_SHORT).show()
        }

        // ======= ฟีเจอร์เสริม =======
        binding.startDrill.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(ListDrillFragment.newInstance())
        }

        binding.trackProgress.setOnClickListener {
            // TODO: ไปหน้าติดตามความก้าวหน้า
            Toast.makeText(requireContext(), "ไปหน้าติดตามความก้าวหน้า", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showExitProgramDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("ออกจากตารางซ้อม")
            .setMessage("คุณต้องการออกจากตารางซ้อมนี้หรือไม่? ความก้าวหน้าจะถูกรีเซ็ต")
            .setPositiveButton("ออกจากโปรแกรม") { _, _ ->
                clearProgramSelection()
                Toast.makeText(requireContext(), "ออกจากโปรแกรมเรียบร้อย", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    private fun observeViewModel() {
        // Observe training plan data
        viewModel.trainingPlan.observe(viewLifecycleOwner) { weeks ->
            if (weeks.isNotEmpty()) {
                Log.d(TAG, "Training plan received: ${weeks.keys}")

                // แสดงเฉพาะวันปัจจุบัน
                val todayTraining = getTodayTraining(weeks)

                if (todayTraining != null) {
                    binding.recyclerTrainingDays.adapter = TrainingDayAdapter(listOf(todayTraining))
                    Log.d(TAG, "Showing today's training: ${todayTraining.day}")
                } else {
                    // ถ้าไม่มีตารางวันนี้ แสดงข้อความ
                    binding.recyclerTrainingDays.adapter = TrainingDayAdapter(emptyList())
                    Log.d(TAG, "No training scheduled for today")
                }
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    /**
     * หาตารางซ้อมของวันปัจจุบัน
     */
    private fun getTodayTraining(weeks: Map<String, Map<String, com.example.myproject.data.training.TrainingModel>>): com.example.myproject.data.training.TrainingModel? {
        // หาวันปัจจุบัน (Monday = 2, Tuesday = 3, ..., Sunday = 1)
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // แปลงเป็นชื่อวัน
        val todayName = when (dayOfWeek) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> ""
        }

        Log.d(TAG, "Today is: $todayName")

        // ค้นหาในทุก week
        for ((weekKey, days) in weeks) {
            for ((dayKey, training) in days) {
                // เช็คว่า day ตรงกับวันนี้หรือไม่
                if (training.day?.contains(todayName, ignoreCase = true) == true) {
                    Log.d(TAG, "Found training for $todayName in $weekKey: $dayKey")
                    return training
                }
            }
        }

        Log.d(TAG, "No training found for $todayName")
        return null
    }

    private fun loadAndUpdateUIState() {
        val isProgramSelected = sharedPreferences.getBoolean("program_selected", false)
        val programName = sharedPreferences.getString("selected_program_name", "")
        val displayName = sharedPreferences.getString("selected_program_display_name", "")
        val subProgramName = sharedPreferences.getString("selected_sub_program_name", "")

        Log.d(TAG, "loadAndUpdateUIState - isProgramSelected: $isProgramSelected")
        Log.d(TAG, "programName: $programName, displayName: $displayName")

        if (isProgramSelected && !programName.isNullOrEmpty()) {
            // STATE 2: เลือกตารางซ้อมแล้ว
            binding.notSelectedStateCard.visibility = View.GONE
            binding.selectedStateLayout.visibility = View.VISIBLE

            // อัพเดทชื่อโปรแกรม
            binding.mainProgramTitle.text = displayName?.ifEmpty { programName }

            if (!subProgramName.isNullOrEmpty()) {
                binding.subProgramTitle.text = subProgramName
            }

            // โหลดข้อมูลจาก ViewModel
            viewModel.loadTrainingPlan(programName)

            Log.d(TAG, "UI switched to STATE 2")
        } else {
            // STATE 1: ยังไม่เลือกตารางซ้อม
            binding.notSelectedStateCard.visibility = View.VISIBLE
            binding.selectedStateLayout.visibility = View.GONE

            Log.d(TAG, "UI switched to STATE 1")
        }
    }

    fun saveProgramSelection(programName: String, subProgramName: String = "") {
        sharedPreferences.edit().apply {
            putBoolean("program_selected", true)
            putString("selected_program_name", programName)
            putString("selected_sub_program_name", subProgramName)
            apply()
        }
        Log.d(TAG, "Program saved: $programName")
        loadAndUpdateUIState()
    }

    fun clearProgramSelection() {
        sharedPreferences.edit().apply {
            putBoolean("program_selected", false)
            remove("selected_program_name")
            remove("selected_program_display_name")
            remove("selected_sub_program_name")
            apply()
        }
        Log.d(TAG, "Program selection cleared")
        loadAndUpdateUIState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}