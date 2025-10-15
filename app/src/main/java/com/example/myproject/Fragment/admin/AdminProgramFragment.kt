package com.example.myproject.Fragment.admin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.Fragment.loginandregister.SignInFragment
import com.example.myproject.R
import com.example.myproject.databinding.FragmentAdminProgramBinding
import kotlinx.coroutines.flow.collectLatest

class AdminProgramFragment : Fragment() {

    private var _binding: FragmentAdminProgramBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminProgramViewModel by viewModels()
    private lateinit var adapter: AdminProgramAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminProgramBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminProgramAdapter(
            onCancelClick = { athlete ->
                viewModel.cancelPlan(athlete.uid)
                Toast.makeText(context, "ยกเลิกตารางซ้อมของ ${athlete.name}", Toast.LENGTH_SHORT).show()
            },
            onDetailClick = { athlete ->
                Toast.makeText(context, "ดูรายละเอียดของ ${athlete.name}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerViewPrograms.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewPrograms.adapter = adapter

        // Observe data
        lifecycleScope.launchWhenStarted {
            viewModel.athletes.collectLatest {
                adapter.submitList(it)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.isLoading.collectLatest {
                binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
            }
        }

        viewModel.loadAthletes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = AdminProgramFragment()
    }
}

