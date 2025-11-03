package com.example.myproject.Fragment.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.databinding.FragmentWorkoutHistoryBinding

class WorkoutHistoryFragment : Fragment() {

    private var _binding: FragmentWorkoutHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WorkoutHistoryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return WorkoutHistoryViewModel() as T
            }
        }
    }

    private lateinit var historyAdapter: WorkoutHistoryAdapter

    companion object {
        fun newInstance() = WorkoutHistoryFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWorkoutHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // โหลดข้อมูล
        viewModel.loadWorkoutHistory()
        viewModel.loadStatistics()
    }

    private fun setupRecyclerView() {
        historyAdapter = WorkoutHistoryAdapter { workoutLog ->
            // คลิกดูรายละเอียด
            Toast.makeText(requireContext(), "รายละเอียด: ${workoutLog.trainingType}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnFilterAll.setOnClickListener {
            viewModel.loadWorkoutHistory()
            highlightFilterButton(FilterType.ALL)
        }

        binding.btnFilterWeek.setOnClickListener {
            viewModel.loadWorkoutHistoryLastWeek()
            highlightFilterButton(FilterType.WEEK)
        }

        binding.btnFilterMonth.setOnClickListener {
            viewModel.loadWorkoutHistoryLastMonth()
            highlightFilterButton(FilterType.MONTH)
        }

        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun highlightFilterButton(filterType: FilterType) {
        // รีเซ็ตปุ่มทั้งหมด
        binding.btnFilterAll.alpha = 0.5f
        binding.btnFilterWeek.alpha = 0.5f
        binding.btnFilterMonth.alpha = 0.5f

        // ไฮไลต์ปุ่มที่เลือก
        when (filterType) {
            FilterType.ALL -> binding.btnFilterAll.alpha = 1.0f
            FilterType.WEEK -> binding.btnFilterWeek.alpha = 1.0f
            FilterType.MONTH -> binding.btnFilterMonth.alpha = 1.0f
        }
    }

    private fun observeViewModel() {
        viewModel.workoutHistory.observe(viewLifecycleOwner) { logs ->
            if (logs.isEmpty()) {
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.recyclerViewHistory.visibility = View.GONE
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.recyclerViewHistory.visibility = View.VISIBLE
                historyAdapter.updateLogs(logs)
            }
        }

        viewModel.statistics.observe(viewLifecycleOwner) { stats ->
            binding.tvTotalWorkouts.text = "${stats.totalWorkouts} ครั้ง"
            binding.tvTotalDistance.text = String.format("%.2f กม.", stats.totalDistance)
            binding.tvTotalDuration.text = formatDuration(stats.totalDuration)
            binding.tvAveragePace.text = "${stats.averagePace}/กม."
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60

        return when {
            hours > 0 -> "${hours}ชม. ${minutes}นาที"
            else -> "${minutes}นาที"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

enum class FilterType {
    ALL, WEEK, MONTH
}