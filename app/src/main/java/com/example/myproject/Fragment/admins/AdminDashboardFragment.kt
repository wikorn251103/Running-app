package com.example.myproject.Fragment.admins

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.Fragment.loginandregister.SignInFragment
import com.example.myproject.MainActivity

import com.example.myproject.databinding.FragmentAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    companion object {
        private const val TAG = "AdminDashboard"
        fun newInstance() = AdminDashboardFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkAdminRole()
        loadStatistics()
        setupClickListeners()
    }

    /**
     * ตรวจสอบว่าเป็น Admin หรือไม่
     */
    private fun checkAdminRole() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "กรุณา Login ก่อน", Toast.LENGTH_SHORT).show()
            activity?.onBackPressedDispatcher?.onBackPressed()
            return
        }

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")
                if (role != "admin") {
                    Toast.makeText(requireContext(), "ไม่มีสิทธิ์เข้าถึงหน้านี้", Toast.LENGTH_SHORT).show()
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to check admin role", e)
                Toast.makeText(requireContext(), "เกิดข้อผิดพลาด", Toast.LENGTH_SHORT).show()
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
    }

    /**
     * โหลดสถิติรวม
     */
    private fun loadStatistics() {
        binding.progressBar.visibility = View.VISIBLE

        // นับจำนวนผู้ใช้ทั้งหมด
        firestore.collection("users")
            .get()
            .addOnSuccessListener { users ->
                binding.tvTotalUsers.text = users.size().toString()
                Log.d(TAG, "Total users: ${users.size()}")
            }

        // นับจำนวนผู้ใช้ที่กำลัง Active (มีโปรแกรม)
        firestore.collection("Athletes")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { athletes ->
                binding.tvActiveUsers.text = athletes.size().toString()
                Log.d(TAG, "Active users: ${athletes.size()}")
            }

        // นับจำนวนโปรแกรมทั้งหมด
        firestore.collection("training_plans")
            .get()
            .addOnSuccessListener { plans ->
                binding.tvTotalPrograms.text = plans.size().toString()
                Log.d(TAG, "Total programs: ${plans.size()}")
            }

        // นับจำนวนบทความ
        firestore.collection("articles")
            .get()
            .addOnSuccessListener { articles ->
                binding.tvTotalArticles.text = articles.size().toString()
                Log.d(TAG, "Total articles: ${articles.size()}")
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load statistics", e)
                binding.progressBar.visibility = View.GONE
            }
    }

    private fun setupClickListeners() {
        // จัดการผู้ใช้
        binding.cardManageUsers.setOnClickListener {
            navigateToFragment(ManageUsersFragment.newInstance(), addToBackStack = true)
        }

        // จัดการโปรแกรมซ้อม
        binding.cardManagePrograms.setOnClickListener {
            navigateToFragment(ManageTrainingPlansFragment.newInstance(), addToBackStack = true)
        }

        // จัดการบทความ
        binding.cardManageArticles.setOnClickListener {
            Toast.makeText(requireContext(), "คลิกจัดการบทความ", Toast.LENGTH_SHORT).show()
            //navigateToFragment(ManageArticlesFragment.newInstance(), addToBackStack = true)
        }

        // จัดการดริล
        binding.cardManageDrills.setOnClickListener {
            Toast.makeText(requireContext(), "คลิกจัดการดริล", Toast.LENGTH_SHORT).show()
            //navigateToFragment(ManageDrillsFragment.newInstance(), addToBackStack = true)
        }

        // ดูรายงานและสถิติ
        binding.cardViewReports.setOnClickListener {
            Toast.makeText(requireContext(), "คลิกดูรรายงานและสถิติ", Toast.LENGTH_SHORT).show()
            //navigateToFragment(ReportsFragment.newInstance(), addToBackStack = true)
        }

        // ออกจากระบบ
        binding.cardManageLogout.setOnClickListener{
            auth.signOut()
            (activity as? MainActivity)?.replaceFragmentClearBackStack(
                SignInFragment.newInstance()
            )
        }

        // Refresh สถิติ
        binding.btnRefresh.setOnClickListener {
            loadStatistics()
        }
    }

    private fun navigateToFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        (activity as? MainActivity)?.replaceFragment(fragment, addToBackStack)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}