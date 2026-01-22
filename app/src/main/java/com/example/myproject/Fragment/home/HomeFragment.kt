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
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.Fragment.workout.WorkoutHistoryFragment
import com.example.myproject.Fragment.drill.ListDrillFragment
import com.example.myproject.Fragment.target.TargetDistanceFragment
import com.example.myproject.Fragment.training.TrainingScheduleFragment
import com.example.myproject.MainActivity
import com.example.myproject.R
import com.example.myproject.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

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

        binding.recyclerTrainingDays.layoutManager = LinearLayoutManager(requireContext())

        observeViewModel()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - reloading UI state")

        //  เช็คว่าโปรแกรมหมดอายุหรือยัง (ทั้งมือใหม่และปกติ)
        checkProgramCompletionOnHome()

        syncProgramFromFirebase()

        //  โหลดข้อมูลใหม่ทุกครั้งที่ onResume เพื่อแสดงวันปัจจุบัน
        refreshCurrentDayTraining()
    }

    /**
     *  โหลดข้อมูลการซ้อมของวันปัจจุบันใหม่
     */
    private fun refreshCurrentDayTraining() {
        val isProgramSelected = sharedPreferences.getBoolean("program_selected", false)
        val programName = sharedPreferences.getString("selected_program_name", "")

        if (isProgramSelected && !programName.isNullOrEmpty()) {
            viewModel.refreshTrainingPlan(programName)
        }
    }

    private fun setupClickListeners() {
        binding.nextBtn.setOnClickListener {
            binding.nextBtn.setColorFilter(ContextCompat.getColor(requireContext(), R.color.yellow))
            (activity as? MainActivity)?.replaceFragment(TargetDistanceFragment.newInstance())
        }

        binding.notSelectedStateCard.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(TargetDistanceFragment.newInstance())
        }

        binding.exitProgramButton.setOnClickListener {
            showExitProgramDialog()
        }

        binding.startDrill.setOnClickListener {
            (activity as? MainActivity)?.supportFragmentManager?.commit {
                replace(R.id.container_main, ListDrillFragment.newInstance())
                addToBackStack(null)
            }
        }

        binding.trackProgress.setOnClickListener {
            val isProgramSelected = sharedPreferences.getBoolean("program_selected", false)

            if (!isProgramSelected) {
                Toast.makeText(
                    requireContext(),
                    "กรุณาเลือกโปรแกรมการซ้อมก่อนดูความก้าวหน้า",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            (activity as? MainActivity)?.replaceFragment(WorkoutHistoryFragment.newInstance())
        }
    }

    private fun calculateCurrentWeek(startDateMillis: Long): Int {
        if (startDateMillis == 0L) {
            Log.w(TAG, "⚠️ No start date found, defaulting to week 1")
            return 1
        }

        val startCalendar = Calendar.getInstance().apply {
            timeInMillis = startDateMillis
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

        val daysDiff =
            ((today.timeInMillis - startCalendar.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        val currentWeek = (daysDiff / 7) + 1

        return currentWeek.coerceIn(1, 12)
    }

    /**
     *  เช็คว่าโปรแกรมหมดอายุหรือยัง (ทั้งมือใหม่และปกติ)
     */
    private fun checkProgramCompletionOnHome() {
        val isProgramSelected = sharedPreferences.getBoolean("program_selected", false)
        if (!isProgramSelected) return

        val startDate = sharedPreferences.getLong("program_start_date", 0L)
        if (startDate == 0L) {
            Log.w(TAG, "⚠️ No start date found for program")
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


        if (daysSinceStart >= totalDays) {
            Log.d(TAG, "🎉 Program completed! Auto-exiting...")
            showProgramCompletionDialogOnHome()
        }
    }

    /**
     *  แสดง Dialog เมื่อจบโปรแกรม
     */
    private fun showProgramCompletionDialogOnHome() {
        if (!isAdded) return

        val isViewOnly = sharedPreferences.getBoolean("is_view_only_program", false)
        val message = if (isViewOnly) {
            "คุณซ้อมครบโปรแกรมมือใหม่ 4 สัปดาห์แล้ว!\n\nระบบจะออกจากโปรแกรมอัตโนมัติ คุณสามารถเลือกโปรแกรมใหม่ได้"
        } else {
            "คุณซ้อมครบตามเป้าหมาย 4 สัปดาห์แล้ว!\n\nระบบจะออกจากโปรแกรมอัตโนมัติ คุณสามารถเลือกโปรแกรมใหม่ได้"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("🎉 ยินดีด้วย!")
            .setMessage(message)
            .setPositiveButton("ตกลง") { _, _ ->
                autoExitProgramOnHome()
            }
            .setCancelable(false)
            .show()
    }

    /**
     *  ออกจากโปรแกรมอัตโนมัติ (ทั้งมือใหม่และปกติ)
     */
    private fun autoExitProgramOnHome() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            clearProgramSelection()
            loadAndUpdateUIState()
            return
        }

        _binding?.let { it.progressBar?.visibility = View.VISIBLE }

        firestore.collection("Athletes")
            .document(userId)
            .delete()
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener

                clearProgramSelection()
                _binding?.let { it.progressBar?.visibility = View.GONE }
                Toast.makeText(requireContext(), "ยินดีด้วย! คุณซ้อมเสร็จแล้ว เลือกโปรแกรมใหม่ได้เลย", Toast.LENGTH_LONG).show()
                loadAndUpdateUIState()
            }
            .addOnFailureListener { e ->
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

                        clearProgramSelection()
                        _binding?.let { it.progressBar?.visibility = View.GONE }
                        Toast.makeText(requireContext(), "ยินดีด้วย! คุณซ้อมเสร็จแล้ว!", Toast.LENGTH_SHORT).show()
                        loadAndUpdateUIState()
                    }
                    .addOnFailureListener { updateError ->
                        if (!isAdded || _binding == null) return@addOnFailureListener

                        clearProgramSelection()
                        _binding?.let { it.progressBar?.visibility = View.GONE }
                        loadAndUpdateUIState()
                    }
            }
    }

    private fun syncProgramFromFirebase() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            loadAndUpdateUIState()
            return
        }

        if (!isAdded) {
            return
        }

        _binding?.let { it.progressBar?.visibility = View.VISIBLE }

        firestore.collection("Athletes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) {
                    Log.w(TAG, "⚠️ Fragment detached or binding null, ignoring result")
                    return@addOnSuccessListener
                }


                if (document.exists()) {
                    val isActive = document.getBoolean("isActive") ?: false
                    val programId = document.getString("programId") ?: ""
                    val programDisplayName = document.getString("programDisplayName") ?: ""
                    val subProgramName = document.getString("subProgramName") ?: "โปรแกรมย่อย 5K"
                    val isViewOnly = document.getBoolean("isViewOnly") ?: false

                    val startDateMillis = try {
                        document.getTimestamp("startDate")?.toDate()?.time
                    } catch (e: Exception) {
                        document.getLong("startDate")
                    } ?: 0L


                    if (isActive && programId.isNotEmpty()) {
                        sharedPreferences.edit().apply {
                            putBoolean("program_selected", true)
                            putString("selected_program_name", programId)
                            putString("selected_program_display_name", programDisplayName)
                            putString("selected_sub_program_name", subProgramName)
                            putBoolean("is_view_only_program", isViewOnly)
                            putLong("program_start_date", startDateMillis)
                            apply()
                        }

                    } else {

                        clearProgramSelection()
                    }
                } else {

                    clearProgramSelection()
                }

                _binding?.let { it.progressBar?.visibility = View.GONE }
                loadAndUpdateUIState()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener

                Log.e(TAG, "❌ Failed to sync from Firebase: ${e.message}", e)
                _binding?.let { it.progressBar?.visibility = View.GONE }
                loadAndUpdateUIState()
            }
    }

    private fun showExitProgramDialog() {
        if (!isAdded) return

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

            clearProgramSelection()
            return
        }

        Log.d(TAG, "🗑️ Resetting program completely from Firebase")
        _binding?.let { it.progressBar?.visibility = View.VISIBLE }

        firestore.collection("Athletes")
            .document(userId)
            .delete()
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener


                clearProgramSelection()

                _binding?.let { it.progressBar?.visibility = View.GONE }

                loadAndUpdateUIState()

                Toast.makeText(
                    requireContext(),
                    "ออกจากโปรแกรมและรีเซ็ตเรียบร้อย",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener



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
                        if (!isAdded || _binding == null) return@addOnSuccessListener

                        Log.d(TAG, "✅ Program reset by update")
                        clearProgramSelection()
                        _binding?.let { it.progressBar?.visibility = View.GONE }

                        loadAndUpdateUIState()

                        Toast.makeText(
                            requireContext(),
                            "ออกจากโปรแกรมเรียบร้อย",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { updateError ->
                        if (!isAdded || _binding == null) return@addOnFailureListener


                        clearProgramSelection()
                        _binding?.let { it.progressBar?.visibility = View.GONE }

                        loadAndUpdateUIState()

                        Toast.makeText(
                            requireContext(),
                            "เกิดข้อผิดพลาด แต่ออกจากโปรแกรมแล้ว",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
    }

    private fun observeViewModel() {
        viewModel.trainingPlan.observe(viewLifecycleOwner) { weeks ->
            if (!isAdded || _binding == null) return@observe

            if (weeks.isNotEmpty()) {


                weeks.forEach { (weekKey, weekData) ->
                    Log.d(TAG, "📊 $weekKey has ${weekData.size} days")
                    weekData.forEach { (dayKey, training) ->

                    }
                }

                val todayTraining = getTodayTraining(weeks)

                if (todayTraining != null) {
                    binding.recyclerTrainingDays.adapter = TrainingDayAdapter(listOf(todayTraining))

                } else {
                    binding.recyclerTrainingDays.adapter = TrainingDayAdapter(emptyList())

                }
            } else {

            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            _binding?.let { it.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!isAdded) return@observe

            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    /**
     * ✅ ดึงการซ้อมของวันนี้ในสัปดาห์ปัจจุบัน (Real-time Daily View)
     */
    private fun getTodayTraining(weeks: Map<String, Map<String, com.example.myproject.data.training.TrainingModel>>): com.example.myproject.data.training.TrainingModel? {
        val startDate = sharedPreferences.getLong("program_start_date", 0L)

        if (startDate == 0L) {

            return null
        }

        val currentWeek = calculateCurrentWeek(startDate)


        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val todayName = when (dayOfWeek) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> {
                return null
            }
        }

        val currentDate = calendar.time

        val weekKey = "week_$currentWeek"
        val currentWeekData = weeks[weekKey]

        if (currentWeekData == null) {
            return null
        }

        for ((dayKey, training) in currentWeekData) {
            if (training.day?.contains(todayName, ignoreCase = true) == true) {
                Log.d(TAG, "✅ Found training for $todayName in Week $currentWeek: ${training.type}")
                return training
            }
        }

        return null
    }

    private fun loadAndUpdateUIState() {
        if (!isAdded || _binding == null) {

            return
        }

        val isProgramSelected = sharedPreferences.getBoolean("program_selected", false)
        val programName = sharedPreferences.getString("selected_program_name", "")
        val displayName = sharedPreferences.getString("selected_program_display_name", "")
        val subProgramName = sharedPreferences.getString("selected_sub_program_name", "")
        val isViewOnly = sharedPreferences.getBoolean("is_view_only_program", false)


        if (isProgramSelected && !programName.isNullOrEmpty()) {
            // ✅ STATE 2: เลือกโปรแกรมแล้ว

            binding.defaultHeaderLayout.visibility = View.GONE
            binding.selectedHeaderLayout.visibility = View.VISIBLE
            binding.headerProgramTitle.text = displayName?.ifEmpty { programName }

            binding.selectProgramSection.visibility = View.GONE

            binding.notSelectedStateCard.visibility = View.GONE
            binding.selectedStateLayout.visibility = View.VISIBLE

            if (!subProgramName.isNullOrEmpty()) {
                binding.subProgramTitle.text = subProgramName
            }

            if (isViewOnly) {
                binding.trackProgress.visibility = View.GONE
                Log.d(TAG, "🙈 Hidden 'Track Progress' button for View Only program")
            } else {
                binding.trackProgress.visibility = View.VISIBLE
                binding.trackProgress.isEnabled = true
                binding.trackProgress.alpha = 1.0f
            }

            updateBottomNavigationVisibility(true)

            viewModel.loadTrainingPlanFromAthlete(programName)

        } else {
            // ✅ STATE 1: ยังไม่เลือกโปรแกรม

            binding.defaultHeaderLayout.visibility = View.VISIBLE
            binding.selectedHeaderLayout.visibility = View.GONE

            binding.selectProgramSection.visibility = View.VISIBLE

            binding.notSelectedStateCard.visibility = View.VISIBLE
            binding.selectedStateLayout.visibility = View.GONE

            binding.trackProgress.visibility = View.VISIBLE
            binding.trackProgress.isEnabled = false
            binding.trackProgress.alpha = 0.5f

            updateBottomNavigationVisibility(false)

        }
    }

    private fun updateBottomNavigationVisibility(showScheduleMenu: Boolean) {
        val activity = activity as? MainActivity
        activity?.updateScheduleMenuVisibility(showScheduleMenu)
    }

    fun saveProgramSelection(programName: String, subProgramName: String = "") {
        sharedPreferences.edit().apply {
            putBoolean("program_selected", true)
            putString("selected_program_name", programName)
            putString("selected_sub_program_name", subProgramName)
            apply()
        }
        loadAndUpdateUIState()
    }

    fun clearProgramSelection() {
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

        viewModel.clearTrainingPlan()

        loadAndUpdateUIState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}