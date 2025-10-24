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

        binding.subProgramCard.setOnClickListener {
            Toast.makeText(requireContext(), "ไปหน้าโปรแกรมย่อย", Toast.LENGTH_SHORT).show()
        }

        binding.startDrill.setOnClickListener {
            (activity as? MainActivity)?.supportFragmentManager?.commit {
                replace(R.id.container_main, ListDrillFragment.newInstance())
                addToBackStack(null)
            }
        }

        binding.trackProgress.setOnClickListener {
            //Toast.makeText(requireContext(), "ไปหน้าติดตามความก้าวหน้า", Toast.LENGTH_SHORT).show()
            (activity as? MainActivity)?.replaceFragment(WorkoutHistoryFragment.newInstance())
        }
    }

    private fun syncProgramFromFirebase() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "❌ User not logged in")
            loadAndUpdateUIState()
            return
        }

        Log.d(TAG, "🔄 Syncing program from Firebase for userId: $userId")
        binding.progressBar?.visibility = View.VISIBLE

        firestore.collection("Athletes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                Log.d(TAG, "✅ Athletes document fetched. Exists: ${document.exists()}")

                if (document.exists()) {
                    val isActive = document.getBoolean("isActive") ?: false
                    val programId = document.getString("programId") ?: ""
                    val programDisplayName = document.getString("programDisplayName") ?: ""
                    val subProgramName = document.getString("subProgramName") ?: "โปรแกรมย่อย 5K"

                    Log.d(TAG, "Program data - isActive: $isActive, programId: $programId")

                    if (isActive && programId.isNotEmpty()) {
                        sharedPreferences.edit().apply {
                            putBoolean("program_selected", true)
                            putString("selected_program_name", programId)
                            putString("selected_program_display_name", programDisplayName)
                            putString("selected_sub_program_name", subProgramName)
                            apply()
                        }
                        Log.d(TAG, "✅ Synced from Firebase: $programId")
                    } else {
                        Log.d(TAG, "⚠️ Program not active or empty, clearing selection")
                        clearProgramSelection()
                    }
                } else {
                    Log.d(TAG, "⚠️ No Athletes document found, clearing selection")
                    clearProgramSelection()
                }

                binding.progressBar?.visibility = View.GONE
                loadAndUpdateUIState()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to sync from Firebase: ${e.message}", e)
                binding.progressBar?.visibility = View.GONE
                loadAndUpdateUIState()
            }
    }

    private fun showExitProgramDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("ออกจากตารางซ้อม")
            .setMessage("คุณต้องการออกจากตารางซ้อมนี้หรือไม่? ข้อมูลทั้งหมดจะถูกรีเซ็ตและคุณสามารถเลือกโปรแกรมใหม่ได้")
            .setPositiveButton("ออกจากโปรแกรม") { _, _ ->
                resetProgramCompletelyFromFirebase()
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }

    /**
     * ⭐ รีเซ็ตโปรแกรมทั้งหมดจาก Firebase - ลบข้อมูลออกทั้งหมด
     */
    private fun resetProgramCompletelyFromFirebase() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "❌ User not logged in")
            clearProgramSelection()
            return
        }

        Log.d(TAG, "🗑️ Resetting program completely from Firebase")
        binding.progressBar?.visibility = View.VISIBLE

        // ⭐ ลบ document ใน Athletes/{userId} ออกทั้งหมด
        firestore.collection("Athletes")
            .document(userId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Athletes document deleted from Firebase")

                // เคลียร์ Local
                clearProgramSelection()

                binding.progressBar?.visibility = View.GONE
                Toast.makeText(requireContext(), "ออกจากโปรแกรมและรีเซ็ตเรียบร้อย", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to delete Athletes document: ${e.message}", e)

                // ถ้าลบไม่ได้ ลอง update เป็น inactive
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
                        Log.d(TAG, "✅ Program reset by update")
                        clearProgramSelection()
                        binding.progressBar?.visibility = View.GONE
                        Toast.makeText(requireContext(), "ออกจากโปรแกรมเรียบร้อย", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { updateError ->
                        Log.e(TAG, "❌ Failed to update: ${updateError.message}", updateError)
                        clearProgramSelection()
                        binding.progressBar?.visibility = View.GONE
                        Toast.makeText(requireContext(), "เกิดข้อผิดพลาด แต่ออกจากโปรแกรมแล้ว", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun observeViewModel() {
        viewModel.trainingPlan.observe(viewLifecycleOwner) { weeks ->
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
            binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun getTodayTraining(weeks: Map<String, Map<String, com.example.myproject.data.training.TrainingModel>>): com.example.myproject.data.training.TrainingModel? {
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

        Log.d(TAG, "🗓️ Today is: $todayName")

        for ((weekKey, days) in weeks) {
            for ((dayKey, training) in days) {
                if (training.day?.contains(todayName, ignoreCase = true) == true) {
                    Log.d(TAG, "✅ Found training for $todayName in $weekKey: $dayKey")
                    return training
                }
            }
        }

        Log.d(TAG, "⚠️ No training found for $todayName")
        return null
    }

    private fun loadAndUpdateUIState() {
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