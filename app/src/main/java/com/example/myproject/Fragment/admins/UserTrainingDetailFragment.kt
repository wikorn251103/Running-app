package com.example.myproject.Fragment.admins


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.R
import com.example.myproject.data.training.TrainingModel
import com.example.myproject.databinding.FragmentUserTrainingDetailBinding
import com.example.myproject.Fragment.training.TrainingScheduleAdapter
import com.example.myproject.Fragment.workout.RecordWorkoutFragment
import com.google.firebase.firestore.FirebaseFirestore


class UserTrainingDetailFragment : Fragment() {


    private var _binding: FragmentUserTrainingDetailBinding? = null
    private val binding get() = _binding!!


    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var trainingAdapter: TrainingScheduleAdapter


    private var userId: String = ""
    private var userName: String = ""
    private var programId: String = ""
    private var currentWeek: Int = 1


    companion object {
        private const val TAG = "UserTrainingDetail"
        private const val ARG_USER_ID = "user_id"
        private const val ARG_USER_NAME = "user_name"
        private const val ARG_PROGRAM_ID = "program_id"


        fun newInstance(userId: String, userName: String, programId: String): UserTrainingDetailFragment {
            val fragment = UserTrainingDetailFragment()
            val bundle = Bundle()
            bundle.putString(ARG_USER_ID, userId)
            bundle.putString(ARG_USER_NAME, userName)
            bundle.putString(ARG_PROGRAM_ID, programId)
            fragment.arguments = bundle
            return fragment
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserTrainingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        userId = arguments?.getString(ARG_USER_ID) ?: ""
        userName = arguments?.getString(ARG_USER_NAME) ?: ""
        programId = arguments?.getString(ARG_PROGRAM_ID) ?: ""


        if (userId.isEmpty() || programId.isEmpty()) {
            Toast.makeText(requireContext(), "ข้อมูลไม่ครบถ้วน", Toast.LENGTH_SHORT).show()
            activity?.onBackPressedDispatcher?.onBackPressed()
            return
        }


        binding.tvUserName.text = "ตารางซ้อมของ: $userName"
        binding.tvProgramId.text = "โปรแกรม: ${programId.replace("_", " ").uppercase()}"


        setupRecyclerView()
        setupClickListeners()
        loadUserTrainingData(currentWeek)
    }


    private fun setupRecyclerView() {
        // ✓ ส่ง callback สำหรับเริ่มบันทึกการซ้อม
        trainingAdapter = TrainingScheduleAdapter { trainingDay, weekNumber, dayNumber ->
            navigateToRecordWorkout(trainingDay, weekNumber, dayNumber)
        }
        binding.recyclerViewTraining.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = trainingAdapter
        }
    }


    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }


        binding.btnResetProgress.setOnClickListener {
            showResetProgressDialog()
        }


        binding.btnEditDay.setOnClickListener {
            navigateToEditDay()
        }


        // Week navigation
        binding.btnWeek1.setOnClickListener { selectWeek(1) }
        binding.btnWeek2.setOnClickListener { selectWeek(2) }
        binding.btnWeek3.setOnClickListener { selectWeek(3) }
        binding.btnWeek4.setOnClickListener { selectWeek(4) }
    }


    private fun selectWeek(week: Int) {
        currentWeek = week
        binding.tvWeekTitle.text = "สัปดาห์ที่ $week"
        resetWeekButtons()
        highlightSelectedWeek(week)
        loadUserTrainingData(week)
    }


    private fun resetWeekButtons() {
        val buttons = listOf(
            binding.btnWeek1,
            binding.btnWeek2,
            binding.btnWeek3,
            binding.btnWeek4
        )
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


    /**
     * โหลดข้อมูลตารางซ้อมของผู้ใช้จาก Athletes/{userId}
     */
    private fun loadUserTrainingData(week: Int) {
        binding.progressBar.visibility = View.VISIBLE


        firestore.collection("Athletes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val weekData = document.get("week_$week") as? HashMap<*, *>


                    if (weekData != null) {
                        val trainingDays = mutableListOf<TrainingModel>()


                        for (i in 1..7) {
                            val dayData = weekData["day_$i"] as? HashMap<*, *>
                            dayData?.let {
                                val trainingDay = TrainingModel(
                                    day = i.toString(),
                                    description = it["description"] as? String ?: "",
                                    pace = it["pace"] as? String ?: "",
                                    type = it["type"] as? String ?: ""
                                )
                                trainingDays.add(trainingDay)
                            }
                        }


                        trainingAdapter.updateTrainingDays(trainingDays, week)


                        // แสดงความก้าวหน้า
                        val currentWeek = document.getLong("currentWeek")?.toInt() ?: 1
                        val progress = document.getLong("progress")?.toInt() ?: 0
                        binding.tvProgress.text = "ความก้าวหน้า: $progress% | สัปดาห์ปัจจุบัน: $currentWeek"


                        Log.d(TAG, "Loaded ${trainingDays.size} days for week $week")
                    } else {
                        Toast.makeText(requireContext(), "ไม่พบข้อมูลสัปดาห์ที่ $week", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "ไม่พบข้อมูลผู้ใช้", Toast.LENGTH_SHORT).show()
                }


                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load user training data", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "เกิดข้อผิดพลาด", Toast.LENGTH_SHORT).show()
            }
    }


    /**
     * ✓ ไปหน้าบันทึกการซ้อม
     */
    private fun navigateToRecordWorkout(trainingDay: TrainingModel, weekNumber: Int, dayNumber: Int) {
        val fragment = RecordWorkoutFragment.newInstance(
            trainingData = trainingDay,
            weekNumber = weekNumber,
            dayNumber = dayNumber
        )

        parentFragmentManager.beginTransaction()
            .replace(R.id.container_main, fragment)
            .addToBackStack(null)
            .commit()
    }


    /**
     * รีเซ็ตความก้าวหน้าของผู้ใช้
     */
    private fun showResetProgressDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ รีเซ็ตความก้าวหน้า")
            .setMessage("คุณต้องการรีเซ็ตความก้าวหน้าของ $userName หรือไม่?\n\nการดำเนินการนี้จะ:\n- รีเซ็ตสัปดาห์เป็น 1\n- รีเซ็ตความก้าวหน้าเป็น 0%\n- ล้างรายการที่ทำเสร็จ")
            .setPositiveButton("รีเซ็ต") { _, _ ->
                resetUserProgress()
            }
            .setNegativeButton("ยกเลิก", null)
            .show()
    }


    /**
     * ไปหน้าแก้ไขตารางวันต่อวัน
     */
    private fun navigateToEditDay() {
        val fragment = EditUserTrainingFragment.newInstance(
            userId = userId,
            userName = userName,
            programId = programId
        )
        (activity as? com.example.myproject.MainActivity)?.replaceFragment(fragment)
    }


    /**
     * รีเซ็ตความก้าวหน้า
     */
    private fun resetUserProgress() {
        binding.progressBar.visibility = View.VISIBLE


        firestore.collection("Athletes")
            .document(userId)
            .update(
                "currentWeek", 1,
                "progress", 0,
                "completedDays", emptyList<String>(),
                "lastUpdated", System.currentTimeMillis()
            )
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "✅ รีเซ็ตความก้าวหน้าสำเร็จ", Toast.LENGTH_SHORT).show()
                selectWeek(1)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to reset progress", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "ไม่สามารถรีเซ็ตได้", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}