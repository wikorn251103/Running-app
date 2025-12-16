package com.example.myproject.Fragment.admins

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.data.admin.UserStat
import com.example.myproject.databinding.FragmentReportsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var statsAdapter: UserStatsAdapter

    private var selectedPeriod = "all" // all, week, month
    private var selectedProgram = "all" // all, 5K, 10K, HM, FM

    companion object {
        private const val TAG = "ReportsFragment"
        fun newInstance() = ReportsFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBackButton()
        setupSpinners()
        setupRecyclerView()
        loadAllData()
    }

    private fun setupBackButton() {
        binding.backBtn.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupSpinners() {
        // Period Spinner
        val periods = arrayOf("ทั้งหมด", "สัปดาห์นี้", "เดือนนี้")
        val periodAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periods)
        periodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPeriod.adapter = periodAdapter

        binding.spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedPeriod = when (position) {
                    0 -> "all"
                    1 -> "week"
                    2 -> "month"
                    else -> "all"
                }
                loadAllData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Program Spinner
        val programs = arrayOf("ทั้งหมด", "5K", "10K", "HM", "FM")
        val programAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, programs)
        programAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProgram.adapter = programAdapter

        binding.spinnerProgram.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedProgram = when (position) {
                    0 -> "all"
                    1 -> "5K"
                    2 -> "10K"
                    3 -> "HM"
                    4 -> "FM"
                    else -> "all"
                }
                loadAllData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        statsAdapter = UserStatsAdapter()
        binding.recyclerUserStats.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = statsAdapter
        }
    }

    private fun loadAllData() {
        binding.progressBar.visibility = View.VISIBLE

        // โหลดข้อมูลทั้งหมด
        loadOverallStatistics()
        loadProgramDistribution()
        loadCompletionRates()
        loadUserActivityData()
        loadTopUsers()
    }

    /**
     * สถิติรวมทั้งหมด - ดึงจาก Athletes collection
     */
    private fun loadOverallStatistics() {
        // นับจำนวนผู้ใช้ทั้งหมดจาก users collection
        firestore.collection("users")
            .get()
            .addOnSuccessListener { usersSnapshot ->
                val totalUsers = usersSnapshot.size()
                binding.tvTotalUsersStats.text = totalUsers.toString()
                Log.d(TAG, "Total users: $totalUsers")
            }

        // ดึงข้อมูลจาก Athletes collection
        val query = if (selectedProgram != "all") {
            firestore.collection("Athletes").whereEqualTo("programId", selectedProgram)
        } else {
            firestore.collection("Athletes")
        }

        query.get()
            .addOnSuccessListener { documents ->
                var activeUsers = 0
                var totalDistance = 0.0
                var totalWorkouts = 0
                var totalCompletedDays = 0
                var totalTrainingDays = 0

                for (doc in documents) {
                    val isActive = doc.getBoolean("isActive") ?: false
                    if (isActive) activeUsers++

                    // คำนวณระยะทางรวมและจำนวนการซ้อม
                    for (week in 1..4) {
                        val weekData = doc.get("week_$week") as? HashMap<*, *>
                        weekData?.let {
                            for (day in 1..7) {
                                val dayData = it["day_$day"] as? HashMap<*, *>
                                dayData?.let { d ->
                                    val type = d["type"] as? String

                                    // นับเฉพาะวันที่ไม่ใช่ Rest Day
                                    if (type != null && type != "Rest Day") {
                                        totalTrainingDays++

                                        val isCompleted = d["isCompleted"] as? Boolean ?: false
                                        if (isCompleted) {
                                            totalCompletedDays++
                                            totalWorkouts++

                                            val distance = (d["distance"] as? Number)?.toDouble() ?: 0.0
                                            totalDistance += distance
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // แสดงผลสถิติ
                binding.tvActiveUsersStats.text = activeUsers.toString()
                binding.tvTotalDistanceStats.text = "%.2f กม.".format(totalDistance)
                binding.tvTotalWorkoutsStats.text = totalWorkouts.toString()
                binding.tvAvgDistanceStats.text = if (totalWorkouts > 0) {
                    "%.2f กม.".format(totalDistance / totalWorkouts)
                } else "0 กม."
                binding.tvCompletionRateStats.text = if (totalTrainingDays > 0) {
                    "%.1f%%".format((totalCompletedDays.toDouble() / totalTrainingDays) * 100)
                } else "0%"

                Log.d(TAG, "Stats - Active: $activeUsers, Distance: $totalDistance, Workouts: $totalWorkouts")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load statistics", e)
                Toast.makeText(requireContext(), "ไม่สามารถโหลดสถิติได้", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * กราฟการกระจายตัวของโปรแกรม (Pie Chart)
     */
    private fun loadProgramDistribution() {
        firestore.collection("Athletes")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { documents ->
                val programCounts = mutableMapOf<String, Int>()

                for (doc in documents) {
                    val programId = doc.getString("programId") ?: "Unknown"
                    programCounts[programId] = programCounts.getOrDefault(programId, 0) + 1
                }

                if (programCounts.isEmpty()) {
                    Log.d(TAG, "No active programs found")
                    return@addOnSuccessListener
                }

                setupProgramPieChart(programCounts)
                Log.d(TAG, "Program distribution: $programCounts")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load program distribution", e)
            }
    }

    private fun setupProgramPieChart(programCounts: Map<String, Int>) {
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        val colorMap = mapOf(
            "5K" to Color.rgb(76, 175, 80),
            "10K" to Color.rgb(33, 150, 243),
            "HM" to Color.rgb(255, 152, 0),
            "FM" to Color.rgb(244, 67, 54)
        )

        for ((program, count) in programCounts) {
            entries.add(PieEntry(count.toFloat(), program))
            colors.add(colorMap[program] ?: Color.GRAY)
        }

        val dataSet = PieDataSet(entries, "โปรแกรม")
        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE
        dataSet.sliceSpace = 3f

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter(binding.chartProgramDistribution))

        binding.chartProgramDistribution.apply {
            this.data = data
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)
            legend.textSize = 12f
            animateY(1000)
            invalidate()
        }
    }

    /**
     * อัตราการทำโปรแกรมสำเร็จ (Bar Chart)
     */
    private fun loadCompletionRates() {
        firestore.collection("Athletes")
            .get()
            .addOnSuccessListener { documents ->
                val programStats = mutableMapOf<String, Pair<Int, Int>>() // total, completed

                for (doc in documents) {
                    val isActive = doc.getBoolean("isActive") ?: false
                    val programId = doc.getString("programId") ?: "Unknown"

                    if (!isActive) continue

                    var totalDays = 0
                    var completedDays = 0

                    for (week in 1..4) {
                        val weekData = doc.get("week_$week") as? HashMap<*, *>
                        weekData?.let {
                            for (day in 1..7) {
                                val dayData = it["day_$day"] as? HashMap<*, *>
                                dayData?.let { d ->
                                    val type = d["type"] as? String
                                    if (type != null && type != "Rest Day") {
                                        totalDays++
                                        val isCompleted = d["isCompleted"] as? Boolean ?: false
                                        if (isCompleted) completedDays++
                                    }
                                }
                            }
                        }
                    }

                    val current = programStats.getOrDefault(programId, Pair(0, 0))
                    programStats[programId] = Pair(
                        current.first + totalDays,
                        current.second + completedDays
                    )
                }

                if (programStats.isEmpty()) {
                    Log.d(TAG, "No program stats found")
                    return@addOnSuccessListener
                }

                setupCompletionBarChart(programStats)
                Log.d(TAG, "Completion rates: $programStats")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load completion rates", e)
            }
    }

    private fun setupCompletionBarChart(programStats: Map<String, Pair<Int, Int>>) {
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        var index = 0f

        for ((program, stats) in programStats) {
            val completionRate = if (stats.first > 0) {
                (stats.second.toFloat() / stats.first) * 100
            } else 0f

            entries.add(BarEntry(index, completionRate))
            labels.add(program)
            index++
        }

        val dataSet = BarDataSet(entries, "อัตราการทำสำเร็จ (%)")
        dataSet.color = Color.rgb(103, 58, 183)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK

        val data = BarData(dataSet)

        binding.chartCompletionRate.apply {
            this.data = data
            description.isEnabled = false
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                textSize = 12f
            }
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 100f
            axisLeft.textSize = 12f
            axisRight.isEnabled = false
            legend.textSize = 12f
            animateY(1000)
            invalidate()
        }
    }

    /**
     * กราฟกิจกรรมผู้ใช้ตามเวลา (Line Chart)
     * ดึงจากข้อมูล Athletes → วันที่ทำการซ้อม
     */
    private fun loadUserActivityData() {
        firestore.collection("Athletes")
            .get()
            .addOnSuccessListener { documents ->
                val activityByDate = mutableMapOf<String, Int>()
                val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                val calendar = Calendar.getInstance()

                // สร้างวันที่ย้อนหลัง 14 วัน
                for (i in 13 downTo 0) {
                    calendar.timeInMillis = System.currentTimeMillis()
                    calendar.add(Calendar.DAY_OF_YEAR, -i)
                    val date = dateFormat.format(calendar.time)
                    activityByDate[date] = 0
                }

                // นับจำนวนการซ้อมแต่ละวัน (จากจำนวนคนที่ทำ isCompleted = true)
                for (doc in documents) {
                    for (week in 1..4) {
                        val weekData = doc.get("week_$week") as? HashMap<*, *>
                        weekData?.let {
                            for (day in 1..7) {
                                val dayData = it["day_$day"] as? HashMap<*, *>
                                dayData?.let { d ->
                                    val isCompleted = d["isCompleted"] as? Boolean ?: false
                                    if (isCompleted) {
                                        val completedDate = d["completedDate"] as? Long
                                        if (completedDate != null) {
                                            val date = dateFormat.format(Date(completedDate))
                                            activityByDate[date] = activityByDate.getOrDefault(date, 0) + 1
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                setupActivityLineChart(activityByDate)
                Log.d(TAG, "User activity: $activityByDate")
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load user activity", e)
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun setupActivityLineChart(activityByDate: Map<String, Int>) {
        val entries = mutableListOf<Entry>()
        val labels = activityByDate.keys.toList().sorted()

        labels.forEachIndexed { index, date ->
            entries.add(Entry(index.toFloat(), activityByDate[date]?.toFloat() ?: 0f))
        }

        val dataSet = LineDataSet(entries, "จำนวนการซ้อม")
        dataSet.color = Color.rgb(255, 87, 34)
        dataSet.setCircleColor(Color.rgb(255, 87, 34))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawValues(true)
        dataSet.valueTextSize = 10f
        dataSet.valueTextColor = Color.BLACK

        val data = LineData(dataSet)

        binding.chartUserActivity.apply {
            this.data = data
            description.isEnabled = false
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                textSize = 10f
                labelRotationAngle = -45f
            }
            axisLeft.textSize = 12f
            axisRight.isEnabled = false
            legend.textSize = 12f
            animateX(1000)
            invalidate()
        }
    }

    /**
     * Top Users List - ดึงข้อมูลจาก Athletes + users
     */
    private fun loadTopUsers() {
        firestore.collection("Athletes")
            .get()
            .addOnSuccessListener { documents ->
                val userStats = mutableListOf<UserStat>()
                var processedCount = 0

                if (documents.isEmpty) {
                    Log.d(TAG, "No athletes found")
                    statsAdapter.updateData(emptyList())
                    return@addOnSuccessListener
                }

                for (doc in documents) {
                    val userId = doc.id
                    var totalDistance = 0.0
                    var totalWorkouts = 0
                    var completedDays = 0
                    var totalDays = 0

                    // คำนวณสถิติจาก week data
                    for (week in 1..4) {
                        val weekData = doc.get("week_$week") as? HashMap<*, *>
                        weekData?.let {
                            for (day in 1..7) {
                                val dayData = it["day_$day"] as? HashMap<*, *>
                                dayData?.let { d ->
                                    val type = d["type"] as? String
                                    if (type != null && type != "Rest Day") {
                                        totalDays++
                                        val isCompleted = d["isCompleted"] as? Boolean ?: false
                                        if (isCompleted) {
                                            completedDays++
                                            totalWorkouts++
                                            val distance = (d["distance"] as? Number)?.toDouble() ?: 0.0
                                            totalDistance += distance
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val completionRate = if (totalDays > 0) {
                        (completedDays.toDouble() / totalDays) * 100
                    } else 0.0

                    val programId = doc.getString("programId") ?: "-"

                    // ดึงชื่อผู้ใช้จาก users collection
                    firestore.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("name") ?: "ผู้ใช้ไม่ระบุชื่อ"

                            userStats.add(
                                UserStat(
                                    name = name,
                                    program = programId,
                                    totalDistance = totalDistance,
                                    totalWorkouts = totalWorkouts,
                                    completionRate = completionRate
                                )
                            )

                            processedCount++

                            // อัปเดต adapter เมื่อโหลดครบทุกคน
                            if (processedCount == documents.size()) {
                                val sortedStats = userStats.sortedByDescending { it.totalDistance }
                                statsAdapter.updateData(sortedStats.take(10)) // แสดงแค่ 10 อันดับแรก
                                Log.d(TAG, "Loaded ${sortedStats.size} users")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to load user name for $userId", e)
                            processedCount++

                            if (processedCount == documents.size()) {
                                val sortedStats = userStats.sortedByDescending { it.totalDistance }
                                statsAdapter.updateData(sortedStats.take(10))
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load top users", e)
                Toast.makeText(requireContext(), "ไม่สามารถโหลดข้อมูลผู้ใช้ได้", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}