package com.example.myproject.Fragment.drill

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.Fragment.drill.adapter.DrillListener
import com.example.myproject.Fragment.drill.adapter.drillsAdapter
import com.example.myproject.Fragment.drill.list.drillsViewModel
import com.example.myproject.Fragment.drill.list.drillsViewModelFactory
import com.example.myproject.data.article.drill.drillModel
import com.example.myproject.data.article.drill.drillRepositoryImpl
import com.example.myproject.data.article.drill.drillServiceImpl
import com.example.myproject.databinding.FragmentListDrillBinding


class ListDrillFragment : Fragment(), DrillListener {
    private var _binding: FragmentListDrillBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: drillsAdapter
    private val viewModel: drillsViewModel by viewModels {
        drillsViewModelFactory(drillRepositoryImpl(drillServiceImpl()))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentListDrillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //สร้าง Adapter โดยส่ง this ซึ่งตอนนี้เป็น DrillListener
        adapter = drillsAdapter(this)

        //กำหนด LayoutManager และ Adapter ให้กับ RecyclerView
        binding.drillsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.drillsRecyclerView.adapter = adapter

        //เรียกใช้ ViewModel เพื่อนำข้อมูลมาจาก Firebase หรือ Repository
        viewModel.getDrills()

        //อัพเดท UI เมื่อ drills เปลี่ยนแปลง
        viewModel.drills.observe(viewLifecycleOwner) { drills ->
            binding.progressBar2.visibility = View.GONE
            adapter.submitList(drills) // submitList ใช้กับ ListAdapter
        }

        //แสดง progressBar ขณะโหลดข้อมูล
        binding.progressBar2.visibility = View.VISIBLE

        binding.backBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    // Implement the DrillListener method
    override fun onDrillClicked(drill: drillModel) {
        // Handle drill click event here
        // คุณสามารถทำอะไรเมื่อคลิกที่รายการได้ที่นี่ เช่น แสดงรายละเอียดเพิ่มเติม
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ListDrillFragment()
    }
}
