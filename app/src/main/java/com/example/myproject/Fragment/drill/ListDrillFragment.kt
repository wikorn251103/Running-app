package com.example.myproject.Fragment.drill

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myproject.Fragment.drill.adapter.DrillListener
import com.example.myproject.Fragment.drill.adapter.drillsAdapter
import com.example.myproject.Fragment.drill.detail.DrillDetailFragment
import com.example.myproject.Fragment.drill.list.drillsViewModel
import com.example.myproject.Fragment.drill.list.drillsViewModelFactory
import com.example.myproject.data.drill.drillModel
import com.example.myproject.data.drill.drillRepositoryImpl
import com.example.myproject.data.drill.drillServiceImpl
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
        _binding = FragmentListDrillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = drillsAdapter(this)
        binding.drillsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.drillsRecyclerView.adapter = adapter

        viewModel.getDrills()

        viewModel.drills.observe(viewLifecycleOwner) { drills ->
            binding.progressBar2.visibility = View.GONE
            adapter.submitList(drills)
        }

        binding.progressBar2.visibility = View.VISIBLE

        binding.backBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    // ตรงนี้แหละที่เพิ่ม — เวลา click จะไปหน้า DrillDetailFragment
    override fun onDrillClicked(drill: drillModel) {
        val detailFragment = DrillDetailFragment.newInstance(drill)

        parentFragmentManager.commit {
            replace(
                com.example.myproject.R.id.container_main, // container ที่ MainActivity ใช้
                detailFragment
            )
            addToBackStack(null) // กด back แล้วกลับมา List ได้
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ListDrillFragment()
    }
}
