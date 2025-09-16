package com.example.Fragment.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myproject.MainActivity
import com.example.myproject.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.Fragment.loginandregister.SignInFragment
import com.example.myproject.R

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        loadUserData()

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            (activity as? MainActivity)?.replaceFragment(SignInFragment.newInstance())
        }

        return binding.root
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            binding.textEmail.text = currentUser.email ?: "ไม่มีอีเมล"

            val docRef = db.collection("users").document(currentUser.uid)
            docRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val name = document.getString("name") ?: "ไม่มีชื่อ"
                    val gender = document.getString("gender") ?: "-"
                    val age = document.getLong("age")?.toString() ?: "-"
                    val weight = document.getDouble("weight")?.toString() ?: "-"
                    val height = document.getLong("height")?.toString() ?: "-"

                    binding.textName.text = name
                    binding.textGender.text = gender
                    binding.textAge.text = age
                    binding.textWeight.text = weight
                    binding.textHeight.text = height

                    // ✅ แสดงรูปภาพตามเพศ
                    when (gender.lowercase()) {
                        "ชาย" -> binding.profileImageView.setImageResource(R.drawable.ic_man)
                        "หญิง" -> binding.profileImageView.setImageResource(R.drawable.ic_woman)
                        else -> binding.profileImageView.setImageResource(R.drawable.ic_default_user)
                    }

                } else {
                    binding.textName.text = "ไม่มีข้อมูลผู้ใช้"
                }
            }.addOnFailureListener {
                Toast.makeText(context, "ไม่สามารถโหลดข้อมูลได้", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.textName.text = "กรุณาล็อกอิน"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ProfileFragment()
    }
}
