package com.example.myproject.Fragment.detailprogram

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.myproject.R
import com.example.myproject.databinding.FragmentRunningGoal5kBinding


class RunningGoal5kFragment : Fragment() {

    private var _binding: FragmentRunningGoal5kBinding? = null
    private val binding get() = _binding!!

    private val timeOptions = listOf("20:00", "22:30", "25:00", "27:30", "30:00", "35:00")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRunningGoal5kBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Map ปุ่ม
        val timeButtons = listOf(
            binding.constraintLayout10.findViewById<View>(R.id.button1),
            binding.constraintLayout10.findViewById<View>(R.id.button2),
            binding.constraintLayout10.findViewById<View>(R.id.button3),
            binding.constraintLayout10.findViewById<View>(R.id.button4),
            binding.constraintLayout10.findViewById<View>(R.id.button5),
            binding.constraintLayout10.findViewById<View>(R.id.button6)
        )

        // ให้ทุกปุ่มเวลา เมื่อกดแล้วนำค่ามาใส่ใน EditText
        timeButtons.forEachIndexed { index, view ->
            view.setOnClickListener {
                binding.linearLayout5.findViewById<android.widget.EditText>(R.id.editTextTime)
                    .setText(timeOptions[index])

                timeButtons.forEachIndexed { i, btn ->
                    if (i == index) {
                        btn.setBackgroundResource(R.drawable.time_button_selected_bg)
                        (btn as? android.widget.Button)?.setTextColor(resources.getColor(R.color.white, null))
                    } else {
                        btn.setBackgroundResource(R.drawable.white_bg)
                        (btn as? android.widget.Button)?.setTextColor(resources.getColor(R.color.black, null))
                    }
                }
            }
        }

        //เริ่มโปรแกรม
        binding.startProgramBtn.setOnClickListener {
            val time = binding.linearLayout5.findViewById<android.widget.EditText>(R.id.editTextTime).text.toString()
            if (time.isBlank()) {
                Toast.makeText(requireContext(), "กรุณาเลือกหรือกรอกเวลา", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "เริ่มโปรแกรมด้วยเวลา $time", Toast.LENGTH_SHORT).show()
            }
        }

        //ย้อนกลับ
        binding.backBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        fun newInstance() = RunningGoal5kFragment()

    }
}