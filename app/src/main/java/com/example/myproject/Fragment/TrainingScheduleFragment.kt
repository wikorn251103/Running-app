package com.example.myproject.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myproject.Fragment.target.TargetDistanceFragment
import com.example.myproject.MainActivity
import com.example.myproject.R
import com.example.myproject.databinding.FragmentTrainingScheduleBinding


class TrainingScheduleFragment : Fragment() {

    private var _binding: FragmentTrainingScheduleBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentTrainingScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnStart.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(TargetDistanceFragment.newInstance())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = TrainingScheduleFragment()
    }


}