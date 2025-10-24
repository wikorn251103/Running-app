package com.example.myproject.Fragment.admins


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myproject.R
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.data.training.TrainingModel
import com.example.myproject.databinding.FragmentViewTrainingPlanBinding
import com.example.myproject.Fragment.training.TrainingScheduleAdapter
import com.google.firebase.firestore.FirebaseFirestore


class ViewTrainingPlanFragment : Fragment() {


    private var _binding: FragmentViewTrainingPlanBinding? = null
    private val binding get() = _binding!!


    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var trainingAdapter: TrainingScheduleAdapter
    private var programId: String = ""
    private var currentWeek: Int = 1


    companion object {
        private const val TAG = "ViewTrainingPlan"
        private const val ARG_PROGRAM_ID = "program_id"


        fun newInstance(programId: String): ViewTrainingPlanFragment {
            val fragment = ViewTrainingPlanFragment()
            val bundle = Bundle()
            bundle.putString(ARG_PROGRAM_ID, programId)
            fragment.arguments = bundle
            return fragment
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewTrainingPlanBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        programId = arguments?.getString(ARG_PROGRAM_ID) ?: ""


        if (programId.isEmpty()) {
            Toast.makeText(requireContext(), "ไม่พบข้อมูลโปรแกรม", Toast.LENGTH_SHORT).show()
            activity?.onBackPressedDispatcher?.onBackPressed()
            return
        }


        binding.tvProgramTitle.text = programId.replace("_", " ").uppercase()


        setupRecyclerView()
        setupClickListeners()
        loadWeekData(currentWeek)
    }


    private fun setupRecyclerView() {
        trainingAdapter = TrainingScheduleAdapter { trainingDay, weekNumber, dayNumber ->
            // ไม่ต้องทำอะไร เพราะเป็นหน้าดูอย่างเดียว ไม่มีปุ่มบันทึก
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
        loadWeekData(week)
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


    private fun loadWeekData(week: Int) {
        binding.progressBar.visibility = View.VISIBLE


        firestore.collection("training_plans")
            .document(programId)
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
                        Log.d(TAG, "Loaded ${trainingDays.size} days for week $week")
                    } else {
                        Toast.makeText(requireContext(), "ไม่พบข้อมูลสัปดาห์ที่ $week", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "ไม่พบโปรแกรม", Toast.LENGTH_SHORT).show()
                }


                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load week data", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "เกิดข้อผิดพลาด", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

