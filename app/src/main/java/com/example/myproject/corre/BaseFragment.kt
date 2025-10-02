package com.example.myproject.corre

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<Binding : ViewBinding>(
    private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding
) : Fragment() {

    private var _binding: Binding? = null
    val binding get() = _binding!!

    // เพิ่ม flag ที่ override ได้ใน subclass
    open val hideBottomNav: Boolean = false

    abstract fun initViews()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = bindingInflater.invoke(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ซ่อน/แสดง BottomNav ตาม flag
        (parentFragment as? com.example.myproject.MainFragment)?.setBottomNavVisible(!hideBottomNav)

        initViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        // BottomNav กลับเมื่อ Fragment นี้โดน pop (ถ้าก่อนหน้านี้ซ่อน)
        if (hideBottomNav) {
            (parentFragment as? com.example.myproject.MainFragment)?.setBottomNavVisible(true)
        }
    }
}