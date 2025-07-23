package com.example.myproject.Fragment.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myproject.R
import com.example.myproject.databinding.FragmentHomeBinding
import com.example.myproject.Fragment.target.TargetDistanceFragment
import com.example.myproject.MainActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater,container,false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //ตั้งค่า คลิกปุ่มไปหน้า TargetDistance
        binding.nextBtn.setOnClickListener {
            binding.nextBtn.setColorFilter(ContextCompat.getColor(requireContext(), R.color.yellow))
            (activity as? MainActivity)?.replaceFragment(TargetDistanceFragment.newInstance())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //ทำการล้างค่า _binding เมื่อ Fragment ถูกทำลาย เพื่อป้องกัน memory leak
        _binding = null
    }
    companion object {
        fun newInstance() = HomeFragment()
    }
}