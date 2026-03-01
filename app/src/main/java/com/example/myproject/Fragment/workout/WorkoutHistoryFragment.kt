package com.example.myproject.Fragment.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.R
import com.example.myproject.databinding.FragmentWorkoutHistoryBinding
import com.example.myproject.data.workout.WorkoutLog
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

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
    private lateinit var weeklyStatsAdapter: WeeklyStatsAdapter
    private lateinit var chartDistance: LineChart

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

        initializeViews()
        setupRecyclerViews()
        setupChart()
        setupClickListeners()
        observeViewModel()

        // โหลดข้อมูลครั้งแรก
        viewModel.loadWorkoutHistory()
        viewModel.loadStatistics()
    }

    private fun initializeViews() {
        chartDistance = binding.chartDistance
    }

    private fun setupRecyclerViews() {
        // History RecyclerView
        historyAdapter = WorkoutHistoryAdapter { workoutLog ->
            showWorkoutDetails(workoutLog)
        }

        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
            isNestedScrollingEnabled = false
        }

        // Weekly Stats RecyclerView
        weeklyStatsAdapter = WeeklyStatsAdapter()

        binding.recyclerViewWeeklyStats.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = weeklyStatsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupChart() {
        chartDistance.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            // X Axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = ContextCompat.getColor(requireContext(), R.color.black)
            }

            // Left Y Axis
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.grey_text)
                textColor = ContextCompat.getColor(requireContext(), R.color.black)
                axisMinimum = 0f
            }

            // Right Y Axis
            axisRight.isEnabled = false

            // Legend
            legend.apply {
                isEnabled = true
                textColor = ContextCompat.getColor(requireContext(), R.color.black)
            }
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
        // Workout History
        viewModel.workoutHistory.observe(viewLifecycleOwner) { logs ->
            if (logs.isEmpty()) {
                showEmptyState()
            } else {
                showContent()
                historyAdapter.updateLogs(logs)
                updateChart(logs)
                viewModel.calculateWeeklyStats(logs)
            }
        }

        // Statistics
        viewModel.statistics.observe(viewLifecycleOwner) { stats ->
            binding.tvTotalWorkouts.text = "${stats.totalWorkouts} ครั้ง"
            binding.tvTotalDistance.text = String.format("%.2f กม.", stats.totalDistance)
            binding.tvTotalDuration.text = formatDuration(stats.totalDuration)
            binding.tvAveragePace.text = "${stats.averagePace}/กม."
        }

        // Weekly Stats
        viewModel.weeklyStats.observe(viewLifecycleOwner) { stats ->
            if (stats.isNotEmpty()) {
                binding.cardWeeklyStats.visibility = View.VISIBLE
                weeklyStatsAdapter.updateStats(stats)
            } else {
                binding.cardWeeklyStats.visibility = View.GONE
            }
        }

        // Loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Error
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun updateChart(workoutLogs: List<WorkoutLog>) {
        if (workoutLogs.isEmpty()) {
            binding.cardChart.visibility = View.GONE
            return
        }

        binding.cardChart.visibility = View.VISIBLE

        // สร้าง entries สำหรับกราฟ
        val entries = workoutLogs.mapIndexed { index, log ->
            Entry(index.toFloat(), log.actualDistance.toFloat())
        }

        // สร้าง labels สำหรับแกน X
        val labels = workoutLogs.mapIndexed { index, _ ->
            "ครั้งที่ ${index + 1}"
        }

        // สร้าง DataSet
        val dataSet = LineDataSet(entries, "ระยะทาง (กม.)").apply {
            color = ContextCompat.getColor(requireContext(), R.color.purple)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.black)
            valueTextSize = 9f
            lineWidth = 2.5f
            circleRadius = 5f
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.purple))
            setDrawCircleHole(true)
            setDrawValues(true)
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(requireContext(), R.color.light_purple)
            fillAlpha = 100
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        // ตั้งค่า X Axis labels
        chartDistance.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        chartDistance.xAxis.labelCount = labels.size.coerceAtMost(10)
        chartDistance.xAxis.setLabelRotationAngle(-45f)

        // ใส่ข้อมูลลงกราฟ
        chartDistance.data = LineData(dataSet)
        chartDistance.animateX(800)
        chartDistance.invalidate() // refresh
    }

    private fun showEmptyState() {
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.recyclerViewHistory.visibility = View.GONE
        binding.cardChart.visibility = View.GONE
        binding.cardWeeklyStats.visibility = View.GONE
    }

    private fun showContent() {
        binding.tvEmptyState.visibility = View.GONE
        binding.recyclerViewHistory.visibility = View.VISIBLE
    }

    private fun showWorkoutDetails(workoutLog: WorkoutLog) {
        val details = buildString {
            append("ประเภท: ${workoutLog.trainingType}\n")
            append("ระยะทาง: ${String.format("%.2f", workoutLog.actualDistance)} กม.\n")
            append("เวลา: ${workoutLog.getFormattedDuration()}\n")
            append("เพซเฉลี่ย: ${workoutLog.calculateAveragePace()}/กม.\n")
            if (workoutLog.feeling.isNotEmpty()) {
                append("ความรู้สึก: ${workoutLog.feeling}\n")
            }
            if (workoutLog.notes.isNotEmpty()) {
                append("หมายเหตุ: ${workoutLog.notes}")
            }
        }

        Toast.makeText(requireContext(), details, Toast.LENGTH_LONG).show()
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