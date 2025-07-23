package com.example.myproject.Fragment.newbiedt

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myproject.R
import com.example.myproject.databinding.FragmentNewbieDetailsBinding

class NewbieDetailsFragment : Fragment() {

    private var _binding: FragmentNewbieDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentNewbieDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            NewbieDetailsFragment()
    }
}