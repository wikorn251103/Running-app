package com.example.myproject.Fragment.target

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myproject.Fragment.detailprogram.NewbieDetailsFragment
import com.example.myproject.Fragment.detailprogram.RunningGoal10kFragment
import com.example.myproject.Fragment.detailprogram.RunningGoal5kFragment
import com.example.myproject.MainActivity
import com.example.myproject.ProgramSelectionActivity
import com.example.myproject.databinding.FragmentTargetDistanceBinding

class TargetDistanceFragment : Fragment() {

    private var _binding: FragmentTargetDistanceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTargetDistanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // คลิกปุ่มย้อนกลับ
        binding.provBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.getStartedBtn.setOnClickListener {
            navigateToFragment(NewbieDetailsFragment.newInstance())
        }

        binding.get5kmBtn.setOnClickListener {
            navigateToFragment(RunningGoal5kFragment.newInstance())
        }

        binding.get10kmBtn.setOnClickListener {
            navigateToFragment(RunningGoal10kFragment.newInstance())
        }
    }

    /**
     * ⭐ ฟังก์ชันช่วยในการ navigate ที่รองรับทั้ง MainActivity และ ProgramSelectionActivity
     */
    private fun navigateToFragment(fragment: Fragment) {
        when (activity) {
            is MainActivity -> {
                (activity as? MainActivity)?.replaceFragment(fragment)
            }
            is ProgramSelectionActivity -> {
                (activity as? ProgramSelectionActivity)?.addFragment(fragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = TargetDistanceFragment()
    }
}