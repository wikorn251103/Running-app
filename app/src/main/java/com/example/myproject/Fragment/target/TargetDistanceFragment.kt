package com.example.myproject.Fragment.target

import android.os.Bundle
import android.os.Handler

import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.postDelayed
import com.example.myproject.Fragment.newbiedt.NewbieDetailsFragment
import com.example.myproject.MainActivity
import com.example.myproject.R
import com.example.myproject.databinding.FragmentTargetDistanceBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class TargetDistanceFragment : Fragment() {

    private var _binding: FragmentTargetDistanceBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        //เชื่อมต่อ กับ Layout ของ Fragment
        _binding = FragmentTargetDistanceBinding.inflate(inflater, container, false)
        return binding.root //แสดงผล

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.provBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
        //คลิกปุ่มย้อนกลับ
        binding.provBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.getStartedBtn.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(NewbieDetailsFragment.newInstance())
        }
        binding.get5kmBtn.setOnClickListener { }
        binding.get10kmBtn.setOnClickListener { }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //ทำการล้างค่า _binding เมื่อ Fragment ถูกทำลาย เพื่อป้องกัน memory leak
        _binding = null
    }

    companion object {
        fun newInstance() =
            TargetDistanceFragment()
    }
}