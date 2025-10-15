package com.example.myproject.Fragment.admin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myproject.R
import com.example.myproject.databinding.FragmentEditProgramBinding


class EditProgramFragment : Fragment() {

    private var _binding : FragmentEditProgramBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       _binding = FragmentEditProgramBinding.inflate(inflater, container,false)
        return binding.root
    }

    companion object {
        fun newInstance() = EditProgramFragment()
    }
}