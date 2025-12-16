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

        // ✅ เช็คว่าโปรแกรมมือใหม่หมดอายุหรือยัง
        checkBeginnerProgramExpiryOnHome()

        syncProgramFromFirebase()
    }

    private fun setupClickListeners() {
        binding.nextBtn.setOnClickListener {
            binding.nextBtn.setColorFilter(ContextCompat.getColor(requireContext(), R.color.yellow))
            (activity as? MainActivity)?.replaceFragment(TargetDistanceFragment.newInstance())
        }

        binding.notSelectedStateCard.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(TargetDistanceFragment.newInstance())
        }

        binding.mainProgramCard.setOnClickListener {
            Toast.makeText(requireContext(), "ไปหน้ารายละเอียดโปรแกรม", Toast.LENGTH_SHORT).show()
        }

        binding.exitProgramButton.setOnClickListener {
            showExitProgramDialog()
        }

        // เปิดไปที่สัปดาห์ปัจจุบัน
        binding.subProgramCard.setOnClickListener {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(requireContext(), "กรุณาเข้าสู่ระบบ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ เช็คว่า binding ยังไม่เป็น null
            _binding?.let { it.progressBar?.visibility = View.VISIBLE }

            val isViewOnly = sharedPreferences.getBoolean("is_view_only_program", false)

            firestore.collection("Athletes")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    // ✅ เช็คว่า Fragment ยัง attached อยู่
                    if (!isAdded || _binding == null) return@addOnSuccessListener

                    _binding?.let { it.progressBar?.visibility = View.GONE }

                    if (document.exists()) {
                        val startDateMillis = try {
                            document.getTimestamp("startDate")?.toDate()?.time
                        } catch (e: Exception) {
                            document.getLong("startDate")
                        } ?: 0L

                        val currentWeek = calculateCurrentWeek(startDateMillis)

                        Log.d(TAG, "📅 Opening training schedule at week: $currentWeek (View Only: $isViewOnly)")

                        val fragment = TrainingScheduleFragment.newInstance(
                            initialWeek = currentWeek,
                            isViewOnly = isViewOnly
                        )
                        (activity as? MainActivity)?.replaceFragment(fragment)
                    } else {
                        Toast.makeText(requireContext(), "ไม่พบข้อมูลโปรแกรม", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .addOnFailureListener { e ->
                    if (!isAdded || _binding == null) return@addOnFailureListener

                    _binding?.let { it.progressBar?.visibility = View.GONE }
                    Log.e(TAG, "❌ Error loading program data", e)
                    Toast.makeText(requireContext(), "เกิดข้อผิดพลาด", Toast.LENGTH_SHORT).show()
                }
        }

        binding.startDrill.setOnClickListener {
            (activity as? MainActivity)?.supportFragmentManager?.commit {
                replace(R.id.container_main, ListDrillFragment.newInstance())
                addToBackStack(null)
            }
        }

        binding.trackProgress.setOnClickListener {
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

        Log.d(TAG, "📊 Start date: ${startCalendar.time}")
        Log.d(TAG, "📊 Today: ${today.time}")
        Log.d(TAG, "📊 Days since start: $daysDiff")
        Log.d(TAG, "📊 Current week: $currentWeek")

        return currentWeek.coerceIn(1, 12)
    }

    private fun checkBeginnerProgramExpiryOnHome() {
        val isViewOnly = sharedPreferences.getBoolean("is_view_only_program", false)

        if (!isViewOnly) return

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

        Log.d(TAG, "📊 Beginner Program Check - Days: $daysSinceStart / $totalDays")

        if (daysSinceStart >= totalDays) {
            Log.d(TAG, "🎉 Beginner program expired! Auto-exiting...")
            showBeginnerCompletionDialogOnHome()
        }
    }

    private fun showBeginnerCompletionDialogOnHome() {
        // ✅ เช็คว่า Fragment ยัง attached อยู่
        if (!isAdded) return

        AlertDialog.Builder(requireContext())
            .setTitle("🎉 ยินดีด้วย!")
            .setMessage("คุณซ้อมครบโปรแกรมมือใหม่ 4 สัปดาห์แล้ว!\n\nระบบจะออกจากโปรแกรมอัตโนมัติ คุณสามารถเลือกโปรแกรมใหม่ได้")
            .setPositiveButton("ตกลง") { _, _ ->
                autoExitBeginnerProgramOnHome()
            }
            .setCancelable(false)
            .show()
    }

    private fun autoExitBeginnerProgramOnHome() {
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
                Toast.makeText(requireContext(), "จบโปรแกรมมือใหม่สำเร็จ! เลือกโปรแกรมใหม่ได้เลย", Toast.LENGTH_LONG).show()
                loadAndUpdateUIState()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener

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
                        if (!isAdded || _binding == null) return@addOnSuccessListener

                        clearProgramSelection()
                        _binding?.let { it.progressBar?.visibility = View.GONE }
                        Toast.makeText(requireContext(), "จบโปรแกรมมือใหม่สำเร็จ!", Toast.LENGTH_SHORT).show()
                        loadAndUpdateUIState()
                    }
                    .addOnFailureListener { updateError ->
                        if (!isAdded || _binding == null) return@addOnFailureListener

                        Log.e(TAG, "❌ Failed to update program", updateError)
                        clearProgramSelection()
                        _binding?.let { it.progressBar?.visibility = View.GONE }
                        loadAndUpdateUIState()
                    }
            }
    }

    private fun syncProgramFromFirebase() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "❌ User not logged in")
            loadAndUpdateUIState()
            return
        }

        // ✅ เช็คว่า Fragment ยัง attached อยู่
        if (!isAdded) {
            Log.w(TAG, "⚠️ Fragment not attached, skipping sync")
            return
        }

        Log.d(TAG, "🔄 Syncing program from Firebase for userId: $userId")
        _binding?.let { it.progressBar?.visibility = View.VISIBLE }

        firestore.collection("Athletes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                // ✅ เช็คว่า Fragment ยัง attached อยู่
                if (!isAdded || _binding == null) {
                    Log.w(TAG, "⚠️ Fragment detached or binding null, ignoring result")
                    return@addOnSuccessListener
                }

                Log.d(TAG, "✅ Athletes document fetched. Exists: ${document.exists()}")

                if (document.exists()) {
                    val isActive = document.getBoolean("isActive") ?: false
                    val programId = document.getString("programId") ?: ""
                    val programDisplayName = document.getString("programDisplayName") ?: ""
                    val subProgramName = document.getString("subProgramName") ?: "โปรแกรมย่อย 5K"
                    val isViewOnly = document.getBoolean("isViewOnly") ?: false

                    Log.d(TAG, "Program data - isActive: $isActive, programId: $programId, isViewOnly: $isViewOnly")

                    if (isActive && programId.isNotEmpty()) {
                        sharedPreferences.edit().apply {
                            putBoolean("program_selected", true)
                            putString("selected_program_name", programId)
                            putString("selected_program_display_name", programDisplayName)
                            putString("selected_sub_program_name", subProgramName)
                            putBoolean("is_view_only_program", isViewOnly)
                            apply()
                        }
                        Log.d(TAG, "✅ Synced from Firebase: $programId (View Only: $isViewOnly)")
                    } else {
                        Log.d(TAG, "⚠️ Program not active or empty, clearing selection")
                        clearProgramSelection()
                    }
                } else {
                    Log.d(TAG, "⚠️ No Athletes document found, clearing selection")
                    clearProgramSelection()
                }

                _binding?.let { it.progressBar?.visibility = View.GONE }
                loadAndUpdateUIState()
            }
            .addOnFailureListener { e ->
                // ✅ เช็คว่า Fragment ยัง attached อยู่
                if (!isAdded || _binding == null) return@addOnFailureListener

                Log.e(TAG, "❌ Failed to sync from Firebase: ${e.message}", e)
                _binding?.let { it.progressBar?.visibility = View.GONE }
                loadAndUpdateUIState()
            }
    }

    private fun showExitProgramDialog() {
        // ✅ เช็คว่า Fragment ยัง attached อยู่
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
            Log.e(TAG, "❌ User not logged in")
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

                Log.d(TAG, "✅ Athletes document deleted from Firebase")

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

                Log.e(TAG, "❌ Failed to delete Athletes document: ${e.message}", e)

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

                        Log.e(TAG, "❌ Failed to update: ${updateError.message}", updateError)
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
                Log.d(TAG, "✅ Training plan received: ${weeks.keys}")

                val todayTraining = getTodayTraining(weeks)

                if (todayTraining != null) {
                    binding.recyclerTrainingDays.adapter = TrainingDayAdapter(listOf(todayTraining))
                    Log.d(TAG, "Showing today's training: ${todayTraining.day}")
                } else {
                    binding.recyclerTrainingDays.adapter = TrainingDayAdapter(emptyList())
                    Log.d(TAG, "⚠️ No training scheduled for today")
                }
            } else {
                Log.w(TAG, "⚠️ Training plan is empty")
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

    private fun getTodayTraining(weeks: Map<String, Map<String, com.example.myproject.data.training.TrainingModel>>): com.example.myproject.data.training.TrainingModel? {
        val userId = auth.currentUser?.uid ?: return null

        val startDate = sharedPreferences.getLong("program_start_date", 0L)

        if (startDate == 0L) {
            Log.w(TAG, "⚠️ No start date found")
            return null
        }

        val currentWeek = calculateCurrentWeek(startDate)
        Log.d(TAG, "📅 Current week: $currentWeek")

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
            else -> ""
        }

        Log.d(TAG, "🗓️ Today is: $todayName in Week $currentWeek")

        val weekKey = "week_$currentWeek"
        val currentWeekData = weeks[weekKey]

        if (currentWeekData != null) {
            for ((dayKey, training) in currentWeekData) {
                if (training.day?.contains(todayName, ignoreCase = true) == true) {
                    Log.d(
                        TAG,
                        "✅ Found training for $todayName in Week $currentWeek: ${training.type}"
                    )
                    return training
                }
            }
        }

        Log.d(TAG, "⚠️ No training found for $todayName in Week $currentWeek")
        return null
    }

    private fun loadAndUpdateUIState() {
        // ✅ เช็คว่า binding ยังไม่เป็น null
        if (!isAdded || _binding == null) {
            Log.w(TAG, "⚠️ Binding is null or fragment not attached, cannot update UI")
            return
        }

        val isProgramSelected = sharedPreferences.getBoolean("program_selected", false)
        val programName = sharedPreferences.getString("selected_program_name", "")
        val displayName = sharedPreferences.getString("selected_program_display_name", "")
        val subProgramName = sharedPreferences.getString("selected_sub_program_name", "")

        Log.d(TAG, "📊 loadAndUpdateUIState - isProgramSelected: $isProgramSelected")
        Log.d(TAG, "programName: $programName, displayName: $displayName")

        if (isProgramSelected && !programName.isNullOrEmpty()) {
            binding.notSelectedStateCard.visibility = View.GONE
            binding.selectedStateLayout.visibility = View.VISIBLE

            binding.mainProgramTitle.text = displayName?.ifEmpty { programName }

            if (!subProgramName.isNullOrEmpty()) {
                binding.subProgramTitle.text = subProgramName
            }

            viewModel.loadTrainingPlanFromAthlete(programName)

            Log.d(TAG, "✅ UI switched to STATE 2 (Program Selected)")
        } else {
            binding.notSelectedStateCard.visibility = View.VISIBLE
            binding.selectedStateLayout.visibility = View.GONE

            Log.d(TAG, "ℹ️ UI switched to STATE 1 (No Program)")
        }
    }

    fun saveProgramSelection(programName: String, subProgramName: String = "") {
        sharedPreferences.edit().apply {
            putBoolean("program_selected", true)
            putString("selected_program_name", programName)
            putString("selected_sub_program_name", subProgramName)
            apply()
        }
        Log.d(TAG, "✅ Program saved: $programName")
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
        Log.d(TAG, "🗑️ Program selection cleared")
        loadAndUpdateUIState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}