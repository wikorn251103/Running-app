package com.example.myproject.Fragment.admins

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.example.myproject.R
import com.example.myproject.databinding.FragmentEditTrainingPlanBinding
import com.google.firebase.firestore.FirebaseFirestore

class EditTrainingPlanFragment : Fragment() {

    private var _binding: FragmentEditTrainingPlanBinding? = null
    private val binding get() = _binding!!

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var programId: String = ""
    private var currentWeek: Int = 1
    private var currentDay: Int = 1

    private val trainingTypes = listOf("Easy", "Interval", "Threshold", "Long Run", "Rest Day", "Speed Work", "Recovery")

    companion object {
        private const val TAG = "EditTrainingPlan"
        private const val ARG_PROGRAM_ID = "program_id"

        fun newInstance(programId: String): EditTrainingPlanFragment {
            val fragment = EditTrainingPlanFragment()
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
        _binding = FragmentEditTrainingPlanBinding.inflate(inflater, container, false)
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

        binding.tvProgramTitle.text = "แก้ไข: ${programId.replace("_", " ").uppercase()}"

        setupTypeSpinner()
        setupClickListeners()
        loadDayData(currentWeek, currentDay)
    }

    private fun setupTypeSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, trainingTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerType.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.btnSave.setOnClickListener {
            saveDayData()
        }

        // Week navigation
        binding.btnWeek1.setOnClickListener { selectWeek(1) }
        binding.btnWeek2.setOnClickListener { selectWeek(2) }
        binding.btnWeek3.setOnClickListener { selectWeek(3) }
        binding.btnWeek4.setOnClickListener { selectWeek(4) }

        // Day navigation
        binding.btnDay1.setOnClickListener { selectDay(1) }
        binding.btnDay2.setOnClickListener { selectDay(2) }
        binding.btnDay3.setOnClickListener { selectDay(3) }
        binding.btnDay4.setOnClickListener { selectDay(4) }
        binding.btnDay5.setOnClickListener { selectDay(5) }
        binding.btnDay6.setOnClickListener { selectDay(6) }
        binding.btnDay7.setOnClickListener { selectDay(7) }
    }

    private fun selectWeek(week: Int) {
        // บันทึกข้อมูลก่อนเปลี่ยนสัปดาห์
        if (hasUnsavedChanges()) {
            showSaveConfirmDialog {
                saveDayData()
                changeWeek(week)
            }
        } else {
            changeWeek(week)
        }
    }

    private fun changeWeek(week: Int) {
        currentWeek = week
        binding.tvWeekTitle.text = "สัปดาห์ที่ $week"
        resetWeekButtons()
        highlightSelectedWeek(week)
        loadDayData(currentWeek, currentDay)
    }

    private fun selectDay(day: Int) {
        // บันทึกข้อมูลก่อนเปลี่ยนวัน
        if (hasUnsavedChanges()) {
            showSaveConfirmDialog {
                saveDayData()
                changeDay(day)
            }
        } else {
            changeDay(day)
        }
    }

    private fun changeDay(day: Int) {
        currentDay = day
        binding.tvDayTitle.text = "วันที่ $day - ${getDayName(day)}"
        resetDayButtons()
        highlightSelectedDay(day)
        loadDayData(currentWeek, currentDay)
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

    private fun resetDayButtons() {
        val buttons = listOf(
            binding.btnDay1, binding.btnDay2, binding.btnDay3,
            binding.btnDay4, binding.btnDay5, binding.btnDay6, binding.btnDay7
        )
        buttons.forEach { button ->
            button.setBackgroundColor(resources.getColor(android.R.color.transparent, null))
            button.setTextColor(resources.getColor(R.color.purple, null))
        }
    }

    private fun highlightSelectedDay(day: Int) {
        val selectedButton = when (day) {
            1 -> binding.btnDay1
            2 -> binding.btnDay2
            3 -> binding.btnDay3
            4 -> binding.btnDay4
            5 -> binding.btnDay5
            6 -> binding.btnDay6
            7 -> binding.btnDay7
            else -> binding.btnDay1
        }
        selectedButton.setBackgroundColor(resources.getColor(R.color.accent_green, null))
        selectedButton.setTextColor(resources.getColor(R.color.white, null))
    }

    /**
     * โหลดข้อมูลของวันที่เลือก
     */
    private fun loadDayData(week: Int, day: Int) {
        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("training_plans")
            .document(programId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val weekData = document.get("week_$week") as? HashMap<*, *>
                    val dayData = weekData?.get("day_$day") as? HashMap<*, *>

                    if (dayData != null) {
                        // แสดงข้อมูล
                        val type = dayData["type"] as? String ?: "Easy"
                        val description = dayData["description"] as? String ?: ""
                        val pace = dayData["pace"] as? String ?: ""

                        binding.spinnerType.setSelection(trainingTypes.indexOf(type))
                        binding.etDescription.setText(description)
                        binding.etPace.setText(pace)

                        Log.d(TAG, "Loaded day $day data: $dayData")
                    } else {
                        // ไม่มีข้อมูล - เคลียร์ form
                        clearForm()
                    }
                }
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load day data", e)
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "ไม่สามารถโหลดข้อมูลได้", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * บันทึกข้อมูล
     */
    private fun saveDayData() {
        val type = binding.spinnerType.selectedItem.toString()
        val description = binding.etDescription.text.toString().trim()
        val pace = binding.etPace.text.toString().trim()

        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "กรุณากรอกรายละเอียด", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        val dayData = hashMapOf(
            "day" to getDayName(currentDay),
            "type" to type,
            "description" to description,
            "pace" to pace
        )

        val updatePath = "week_$currentWeek.day_$currentDay"

        firestore.collection("training_plans")
            .document(programId)
            .update(updatePath, dayData)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                Toast.makeText(requireContext(), "✅ บันทึกสำเร็จ", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Saved: Week $currentWeek, Day $currentDay")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save", e)
                binding.progressBar.visibility = View.GONE
                binding.btnSave.isEnabled = true
                Toast.makeText(requireContext(), "ไม่สามารถบันทึกได้", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hasUnsavedChanges(): Boolean {
        // TODO: ตรวจสอบว่ามีการแก้ไขหรือไม่
        return false
    }

    private fun showSaveConfirmDialog(onConfirm: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("บันทึกการเปลี่ยนแปลง?")
            .setMessage("คุณต้องการบันทึกข้อมูลก่อนเปลี่ยนหรือไม่?")
            .setPositiveButton("บันทึก") { _, _ -> onConfirm() }
            .setNegativeButton("ไม่บันทึก") { _, _ -> }
            .show()
    }

    private fun clearForm() {
        binding.spinnerType.setSelection(0)
        binding.etDescription.setText("")
        binding.etPace.setText("")
    }

    private fun getDayName(day: Int): String {
        return when (day) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}